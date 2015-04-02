package client;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import common.BotExecutableType;
import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;
import common.exceptions.StarcraftNotRunningException;
import common.protocols.RemoteStarcraft;
import common.results.BasicGameResult;
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
	public BasicGameResult getResult() throws StarcraftException, StarcraftNotRunningException
	{
		if (thread == null)
			throw new StarcraftNotRunningException();
		else if (!thread.isAlive())
		{
			if (thread.exception != null)
				throw thread.exception;
			else
				return new BasicGameResult(null, null);
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
			Vector<Integer> startingproc = WindowsCommandTools.GetRunningProcesses();
			
			try
			{
				killStarcraftAndChaoslauncher(startingproc);
				
				File replayFile = client.env.lookupFile("$starcraft/maps/replays/"+game.getReplayString());
				File gameStateFile = client.env.lookupFile("$starcraft/gameState.txt");
				replayFile.delete();
				gameStateFile.delete();
				
				// If this is a proxy bot, start the proxy bot script before StarCraft starts
				if (game.bots[index].type == BotExecutableType.proxy)
					WindowsCommandTools.RunWindowsCommand(client.env.lookupFile("$starcraft/bwapi-data/AI/run_proxy.bat").getAbsolutePath(), false, false);
				
				// Start chaoslauncher and thereby starcraft
				Client.log("      Client_StartChaoslauncher()");
				
				// Launch Chaoslauncher, do not wait for this to finish, exit if it fails (false, true)
				WindowsCommandTools.RunWindowsExeLocal(client.env.lookupFile("$chaoslauncher").getAbsolutePath(), "Chaoslauncher.exe", false, true);
		
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
				return;
			}
			catch (StarcraftException e)
			{
				exception = e;
				Client.log(e.getMessage());
			}
			finally
			{
				killStarcraftAndChaoslauncher(startingproc);
				Client.log("ready");
			}
		}
	}
	
	private void killStarcraftAndChaoslauncher(Vector<Integer> startingproc)
	{
		Client.log("killing StarCraft and Chaoslauncher");
		
		while (WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
		{
			System.out.println("Killing Starcraft...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM StarCraft.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		while (WindowsCommandTools.IsWindowsProcessRunning("Chaoslauncher.exe"))
		{
			System.out.println("Killing Chaoslauncher...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM Chaoslauncher.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		while (WindowsCommandTools.IsWindowsProcessRunning("\"Chaoslauncher - MultiInstance.exe\""))
		{
			System.out.println("Killing Chaoslauncher...  ");
			WindowsCommandTools.RunWindowsCommand("taskkill /F /IM \"Chaoslauncher - MultiInstance.exe\"", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		// Kill any processes that weren't running before startcraft started
		// This is helpful to kill any proxy bots or java threads that may still be going
		WindowsCommandTools.KillExcessWindowsProccess(startingproc);
		Client.log("ready");
	}
	
	public void addWindowsRegistryEntries()
	{
		Client.log("registering StarCraft");
		
		// 32-bit machine StarCraft settings
		String sc32KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		String sc32UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc32KeyName,     "InstallPath", "REG_SZ",    client.env.lookup("$starcraft") + "\\");
		WindowsCommandTools.RegEdit(sc32KeyName,     "Program",     "REG_SZ",    client.env.lookup("$starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32KeyName,     "GamePath",    "REG_SZ",    client.env.lookup("$starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// 64-bit machine StarCraft settings
		String sc64KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		String sc64UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc64KeyName, "InstallPath", "REG_SZ", client.env.lookup("$starcraft") + "\\");
		WindowsCommandTools.RegEdit(sc64KeyName, "Program",     "REG_SZ", client.env.lookup("$starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64KeyName, "GamePath",    "REG_SZ", client.env.lookup("$starcraft") + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// Chaoslauncher Settings
		String clKeyName = "HKEY_CURRENT_USER\\Software\\Chaoslauncher\\Launcher";
		WindowsCommandTools.RegEdit(clKeyName,   "GameVersion",     "REG_SZ",    "Starcraft 1.16.1");
		WindowsCommandTools.RegEdit(clKeyName,   "Width",           "REG_DWORD", "00000640");
		WindowsCommandTools.RegEdit(clKeyName,   "Height",          "REG_DWORD", "00000480");
		WindowsCommandTools.RegEdit(clKeyName,   "StartMinimized",  "REG_SZ",    "0");
		WindowsCommandTools.RegEdit(clKeyName,   "MinimizeOnRun",   "REG_SZ",    "1");
		WindowsCommandTools.RegEdit(clKeyName,   "RunScOnStartup",  "REG_SZ",    "1");
		WindowsCommandTools.RegEdit(clKeyName,   "AutoUpdate",      "REG_SZ",    "0");
		WindowsCommandTools.RegEdit(clKeyName,   "WarnNoAdmin",     "REG_SZ",    "0");
		
		// Chaoslauncher plugin settings
		String clpKeyName = "HKEY_CURRENT_USER\\Software\\Chaoslauncher\\PluginsEnabled";
		WindowsCommandTools.RegEdit(clpKeyName,  "BWAPI Injector (1.16.1) RELEASE", "REG_SZ", "1");
		WindowsCommandTools.RegEdit(clpKeyName,  "W-MODE 1.02",                     "REG_SZ", "1");
		WindowsCommandTools.RegEdit(clpKeyName,  "Chaosplugin for 1.16.1",          "REG_SZ", "0");
	}
}
