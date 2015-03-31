package client;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;
import common.protocols.RemoteStarcraft;
import common.utils.WindowsCommandTools;

public class Starcraft extends UnicastRemoteObject implements RemoteStarcraft
{
	private Client client;
	private Game game;
	private StarcraftMatch thread = null;

	public Starcraft(Client client) throws RemoteException
	{
		this.client = client;
	}

	@Override
	public void kill()
	{
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
	public boolean isDone() throws StarcraftException
	{
		if (thread == null)
			return true;
		else if (!thread.isAlive())
		{
			if (thread.exception != null)
				throw thread.exception;
			else
				return true;
		}
		else
			return false;
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
	}
}
