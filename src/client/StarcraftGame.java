package client;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Vector;

import common.Game;
import common.exceptions.StarcraftException;
import common.protocols.RemoteStarcraftGame;
import common.utils.WindowsCommandTools;

public class StarcraftGame extends Thread implements RemoteStarcraftGame
{
	private Client client;
	private Game game;
	private StarcraftException exception = null;
	private int index;

	public StarcraftGame(Client client, Game game, int i) throws RemoteException
	{
		this.client = client;
		this.game = game;
		this.index = i;
	}

	@Override
	public void run()
	{
		Vector<Integer> startingproc = WindowsCommandTools.GetRunningProcesses();
		
		try
		{
			ClientCommands.Client_KillStarcraftAndChaoslauncher();
			
			File replayFile = client.env.lookupFile("$starcraft/"+game.getReplayString());
			File gameStateFile = client.env.lookupFile("$starcraft/gameState.txt");
			replayFile.delete();
			gameStateFile.delete();
			
			// If this is a proxy bot, start the proxy bot script before StarCraft starts
			if (game.bots[index].isProxyBot())
				ClientCommands.Client_RunProxyScript(client.env);
			
			// Start chaoslauncher and thereby starcraft
			ClientCommands.Client_StartChaoslauncher(client.env);
	
			// Record the time that we tried to start the game
			long time = System.currentTimeMillis();
			
			while (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
			{
				if (System.currentTimeMillis()-time > client.env.get("starcraftStartingTimeout", Integer.class)*1000)
					throw new StarcraftException("timeout starting StarCraft");
				
				sleep(500);
			}
			Client.log("starcraft started");
			
			time = System.currentTimeMillis();
			while (!gameStateFile.exists())
			{
				if (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
					throw new StarcraftException("starcraft died while starting match");
				if (System.currentTimeMillis()-time > client.env.get("matchStartingTimeout", Integer.class)*1000)
					throw new StarcraftException("timeout starting match");
				
				sleep(1000);
			}
			Client.log("match started");
			
			time = System.currentTimeMillis();
			while (!replayFile.exists())
			{
				if (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
					throw new StarcraftException("starcraft died during the match");
				if (System.currentTimeMillis()-time > client.env.get("matchEndingTimeout", Integer.class)*1000)
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
			ClientCommands.Client_KillStarcraftAndChaoslauncher();
			// Kill any processes that weren't running before startcraft started
			// This is helpful to kill any proxy bots or java threads that may still be going
			WindowsCommandTools.KillExcessWindowsProccess(startingproc);
			Client.log("ready");
		}
	}

	@Override
	public void kill()
	{
		try
		{
			this.interrupt();
			this.join();
		}
		catch (InterruptedException e)
		{
			System.err.println("interrupted while stopping starcraft game thread");
			e.printStackTrace();
		}
	}

	@Override
	public boolean isDone() throws StarcraftException
	{
		if (exception != null)
			throw exception;
		else
			return !isAlive();
	}
}
