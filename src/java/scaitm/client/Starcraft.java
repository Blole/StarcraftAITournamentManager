package client;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.BotExecutableType;
import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;
import common.exceptions.StarcraftMatchNotFinishedException;
import common.exceptions.StarcraftMatchNotStartedException;
import common.file.MyFile;
import common.protocols.RemoteStarcraft;
import common.status.Done;
import common.status.GameStatus;
import common.status.GameStatusFile;
import common.utils.WindowsCommandTools;
import common.yaml.MyConstructor;

public class Starcraft extends UnicastRemoteObject implements RemoteStarcraft
{
	private static final long serialVersionUID = 5069349040098758632L;
	
	private Client client;
	private StarcraftMatch thread = null;

	public Starcraft(Client client) throws RemoteException
	{
		this.client = client;
	}

	@Override
	public void kill()
	{
		if (thread == null)
			return;
		
		try
		{
			thread.interrupt();
			thread.join();
			Client.log("killed (by remote?)");
		}
		catch (InterruptedException e)
		{
			Client.log("interrupted while stopping starcraft game thread");
			e.printStackTrace();
		}
	}

	@Override
	public GameStatus getStatus() throws StarcraftException
	{
		if (thread == null)
			throw new StarcraftMatchNotStartedException();
		else if (thread.exception != null)
			throw thread.exception;
		else
			return thread.status;
	}
	
	@Override
	public Done getResult() throws StarcraftException
	{
		if (getStatus() == GameStatus.Done)
			return thread.result;
		else
			throw new StarcraftMatchNotFinishedException();
	}

	@Override
	public void start(Game game, int i) throws RemoteException, StarcraftAlreadyRunningException
	{
		if (thread != null && thread.isAlive())
			throw new StarcraftAlreadyRunningException();
		else
		{
			thread = new StarcraftMatch(game, i);
			thread.start();
		}
	}
	
	class StarcraftMatch extends Thread
	{
		Game game;
		int index;
		StarcraftException exception = null;
		Done result = null;
		private GameStatus status;
		
		public StarcraftMatch(Game game, int index)
		{
			this.game = game;
			this.index = index;
		}
		
		@Override
		public void run()
		{
			status = GameStatus.Starting;
			File starcraftDir = client.env.starcraftDir;
			File starcraftExe = null;
			File replayFile = new MyFile(client.env.starcraftDir, "maps/replays/"+game.getReplayString());
			File statusFile = client.env.gamestatusFile;
			
			try
			{
				replayFile.delete();
				statusFile.delete();
				
				if (client.env.multiInstance)
				{
					starcraftExe = new File(starcraftDir, String.format("StarCraft_%08x.exe", new Random().nextInt(0x7fffffff)));
					FileUtils.copyFile(new File(starcraftDir, "StarCraft_MultiInstance.exe"), starcraftExe);
					Client.log("exe is: "+starcraftExe.getName());
				}
				else
				{
					starcraftExe = new File(starcraftDir, "StarCraft.exe");
					WindowsCommandTools.killProcess(starcraftExe.getName());
				}
				
				// If this is a proxy bot, start the proxy bot script before StarCraft starts
				if (game.bots[index].type == BotExecutableType.proxy)
					WindowsCommandTools.RunWindowsCommand(new MyFile(client.env.starcraftDir, "bwapi-data/AI/run_proxy.bat").getAbsolutePath(), false, false);
				
				Runtime.getRuntime().exec(starcraftDir+"/loader.exe --launch "+starcraftExe.getName()+" --lib bwapi-data/BWAPI.dll", null, starcraftDir);
				
				
				long time = System.currentTimeMillis();
				while (!WindowsCommandTools.IsWindowsProcessRunning(starcraftExe.getName()))
				{
					if (System.currentTimeMillis()-time > client.env.starcraftStartingTimeout*1000)
						throw new StarcraftException("timeout starting StarCraft");
					
					sleep(100);
				}
				Client.log("starcraft started in %.1f s", (System.currentTimeMillis() - time)/1000.0);
				
				
				
				time = System.currentTimeMillis();
				while (!statusFile.exists())
				{
					if (!WindowsCommandTools.IsWindowsProcessRunning(starcraftExe.getName()))
						throw new StarcraftException("starcraft died while starting match");
					if (System.currentTimeMillis()-time > client.env.matchStartingTimeout*1000)
						throw new StarcraftException("timeout starting match");
					
					sleep(200);
				}
				Client.log("match started in %.1f s", (System.currentTimeMillis() - time)/1000.0);
				status = GameStatus.Running;
				
				
				
				time = System.currentTimeMillis();
				while (true)
				{
					if (!WindowsCommandTools.IsWindowsProcessRunning(starcraftExe.getName()))
						throw new StarcraftException("starcraft died during the match");
				
					long timeSinceModification = System.currentTimeMillis() - statusFile.lastModified();
					
					GameStatusFile gamestatus = readStatus(statusFile);
					if (gamestatus != null && gamestatus.getStatus() == GameStatus.Done && replayFile.exists())
					{
						result = (Done) gamestatus;
						break;
					}
					else if (timeSinceModification > client.env.matchRunningTimeout*1000)
						throw new StarcraftException(statusFile+" was not updated in time");
					
					sleep((long)(client.env.matchRunningTimeout*1000 - timeSinceModification));
				}
				Client.log("match ended in %.1f s", (System.currentTimeMillis() - time)/1000.0);
			}
			catch (InterruptedException e)
			{
				exception = new StarcraftException("match interrupted");
				Client.log("match interrupted");
			}
			catch (StarcraftException e)
			{
				exception = e;
				Client.log(e.getMessage());
			}
			catch (IOException e)
			{
				exception = new StarcraftException(e);
				e.printStackTrace();
			}
			finally
			{
				WindowsCommandTools.killProcess(starcraftExe.getName());
				
				if (client.env.multiInstance && starcraftExe.exists())
					starcraftExe.delete();
				
				Client.log("ready");
				status = GameStatus.Done;
			}
		}

		private GameStatusFile readStatus(File statusFile) throws IOException
		{
			String status = FileUtils.readFileToString(statusFile);
			return (GameStatusFile) new Yaml(new MyConstructor()).load(status);
		}
	}
	
	public void addWindowsRegistryEntries()
	{
		Client.log("registering StarCraft");
		
		// 32-bit machine StarCraft settings
		String sc32KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		String sc32UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		String starcraftDir = client.env.starcraftDir.toString();
		WindowsCommandTools.RegEdit(sc32KeyName,     "InstallPath", "REG_SZ",    starcraftDir + "\\");
		WindowsCommandTools.RegEdit(sc32KeyName,     "Program",     "REG_SZ",    starcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32KeyName,     "GamePath",    "REG_SZ",    starcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// 64-bit machine StarCraft settings
		String sc64KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		String sc64UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc64KeyName, "InstallPath", "REG_SZ", starcraftDir + "\\");
		WindowsCommandTools.RegEdit(sc64KeyName, "Program",     "REG_SZ", starcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64KeyName, "GamePath",    "REG_SZ", starcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64UserKeyName, "introX",      "REG_DWORD", "00000000");
	}
}
