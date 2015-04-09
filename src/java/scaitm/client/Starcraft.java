package client;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import common.BotExecutableType;
import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;
import common.exceptions.StarcraftNotRunningException;
import common.file.MyFile;
import common.protocols.RemoteStarcraft;
import common.results.GameResult;
import common.utils.WindowsCommandTools;

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
		}
		catch (InterruptedException e)
		{
			System.err.println("interrupted while stopping starcraft game thread");
			e.printStackTrace();
		}
	}
	
	@Override
	public GameResult getResult() throws StarcraftException, StarcraftNotRunningException
	{
		if (thread == null)
			throw new StarcraftNotRunningException();
		else if (!thread.isAlive())
		{
			if (thread.exception != null)
				throw thread.exception;
			else
				return new GameResult(null, null);
		}
		else //matched not yet finished
			return null;
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
		
		public StarcraftMatch(Game game, int index)
		{
			this.game = game;
			this.index = index;
		}
		
		@Override
		public void run()
		{
			File starcraftDir = client.env.starcraftDir;
			Vector<Integer> startingproc = WindowsCommandTools.GetRunningProcesses();
			ProcessBuilder starcraftProcessBuilder = new ProcessBuilder(new File(starcraftDir, "loader.exe").getAbsolutePath(),
					"--launch", "StarCraft.exe",
					"--lib", "bwapi-data/BWAPI.dll");
			starcraftProcessBuilder.directory(starcraftDir);
			
			try
			{
				killStarcraft(null);
				
				File replayFile = new MyFile(client.env.starcraftDir, "maps/replays/"+game.getReplayString());
				File gameStateFile = new MyFile(client.env.starcraftDir, "gameState.txt");
				replayFile.delete();
				gameStateFile.delete();
				
				// If this is a proxy bot, start the proxy bot script before StarCraft starts
				if (game.bots[index].type == BotExecutableType.proxy)
					WindowsCommandTools.RunWindowsCommand(new MyFile(client.env.starcraftDir, "bwapi-data/AI/run_proxy.bat").getAbsolutePath(), false, false);
				
				Process starcraft = starcraftProcessBuilder.start();
				
				// Record the time that we tried to start the game
				long time = System.currentTimeMillis();
				
				while (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
				{
					if (System.currentTimeMillis()-time > client.env.starcraftStartingTimeout*1000)
						throw new StarcraftException("timeout starting StarCraft");
					
					sleep(500);
				}
				Client.log("starcraft started");
				
				time = System.currentTimeMillis();
				while (!gameStateFile.exists())
				{
					if (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
						throw new StarcraftException("starcraft died while starting match");
					if (System.currentTimeMillis()-time > client.env.matchStartingTimeout*1000)
						throw new StarcraftException("timeout starting match");
					
					sleep(1000);
				}
				Client.log("match started");
				
				time = System.currentTimeMillis();
				while (!replayFile.exists())
				{
					if (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
						throw new StarcraftException("starcraft died during the match");
					if (System.currentTimeMillis()-time > client.env.matchEndingTimeout*1000)
						throw new StarcraftException("timeout ending match");
					
					sleep(1000);
				}
				Client.log("match ended");
				
				//sleep to make sure StarCraft wrote the replay file correctly
				sleep(5000);
				Client.log("match done");
				
				//String timeStamp = new SimpleDateFormat("[mm:ss]").format(new Date(gameState.frameCount*42));
			}
			catch (InterruptedException e)
			{
				Client.log("match interrupted");
			}
			catch (StarcraftException e)
			{
				exception = e;
				Client.log(e.getMessage());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				killStarcraft(startingproc);
				Client.log("ready");
			}
		}
	}
	
	public void killStarcraft(Vector<Integer> startingproc)
	{
		while (WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
		{
			Client.log("killing Starcraft");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM StarCraft.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		// Kill any processes that weren't running before startcraft started
		// This is helpful to kill any proxy bots or java threads that may still be going
		if (startingproc != null)
			WindowsCommandTools.KillExcessWindowsProccess(startingproc);
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
