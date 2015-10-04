package server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.Bot;
import common.Game;
import common.GameResults;
import common.Helper;
import common.RunnableUnicastRemoteObject;
import common.exceptions.InvalidResultsException;
import common.exceptions.StarcraftException;
import common.file.PackedFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.protocols.RemoteStarcraft;
import common.status.Done;

public class Server extends RunnableUnicastRemoteObject implements RemoteServer
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	
	
	private ArrayList<RemoteClient> 		clients = new ArrayList<RemoteClient>();
	private ArrayList<RemoteClient> 		free = new ArrayList<RemoteClient>();
	private Lock							gameStarting = new ReentrantLock();
	private Games							games;
	
	private final int						gameRescheduleTimer = 2000;
	
	public ServerGUI 						gui;
	ImageWindow								imageWindow;
	
	public final ServerEnvironment env;
	private Registry registry = null;
	
	public Server(ServerEnvironment env) throws RemoteException
	{
		this.env = env;
		this.games = new Games(env);
	}
	
	@Override
	public void onRun() throws RemoteException, MalformedURLException, InterruptedException, IOException
	{
		gui = new ServerGUI(this);
		registry = LocateRegistry.createRegistry(env.port);
		registry.rebind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		int finishedRound = -1;
		int printedWaitingForRoundToFinish = -1;
		Game prevGame = null;
		while (!games.allDone())
		{
			ServerGame nextGame = games.getNextUnstartedGame();
			
			// if new round
			if (prevGame != null && (nextGame == null || nextGame.round > prevGame.round) && prevGame.round != finishedRound)
			{
				
				if (games.runningMatches.isEmpty() && finishedRound < prevGame.round)
				{
					log("Round %d finished, moving write directory to read directory", prevGame.round);
					for (Bot bot : games.getAllBots())
						FileUtils.copyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
					
					finishedRound = prevGame.round;
					continue;
				}
				else if (printedWaitingForRoundToFinish < prevGame.round)
				{
					log("Waiting for ongoing games in round %d to finish", prevGame.round);
					printedWaitingForRoundToFinish = prevGame.round;
				}
			}
			else if (nextGame != null)
			{
				List<RemoteClient> players = getFree(nextGame.bots.length);
				if (players != null && gameStarting.tryLock())
				{
					RunningMatch runningGame = new RunningMatch(nextGame, players);
					games.runningMatches.add(runningGame);
					new Thread(runningGame).start();
					
					log(nextGame + " starting");
					prevGame = nextGame;
				}
			}
			
			Thread.sleep(gameRescheduleTimer);
		}
		
		log("Done");
		if (!env.exitWhenDone)
			Thread.sleep(Long.MAX_VALUE);
	}

	@Override
	public void onExit() throws RemoteException, AccessException, NotBoundException
	{
		if (env.killClientsOnExit)
			killClients();
		
		if (registry != null)
			registry.unbind(env.serverUrlPath);
		
		if (gui != null)
			gui.mainFrame.dispose();
	}
	
	private List<RemoteClient> getFree(int playerCount)
	{
		List<RemoteClient> players = new ArrayList<>();
		
		while (players.size() < playerCount && free.size()+players.size() >= playerCount)
		{
			RemoteClient player = free.remove(0);
			try
			{
				player.checkAlive();
				players.add(player);
			}
			catch (RemoteException e)
			{
				log("unable to contact "+player);
				clients.remove(player);
			}
		}
		
		if (players.size() == playerCount)
			return players;
		else
		{
			free.addAll(players);
			return null;
		}
	}

	public void getAndDisplayScreenshotFromClient(RemoteClient client)
	{
		try
		{
			InputStream in = new ByteArrayInputStream(client.screenshot());
			BufferedImage screenshot = ImageIO.read(in);
			
			if (imageWindow == null)
				imageWindow = new ImageWindow(client.toString(), screenshot);
			else
				imageWindow.refresh(screenshot);
		}
		catch (IOException e)
		{
			log("%s error getting screenshot", client);
			e.printStackTrace();
		}
	}
	
	public synchronized void sendCommandToAllClients(String command)
	{
		log("sending command '%s' to all clients", command);
		
		for (RemoteClient client : clients)
		{
			try
			{
				client.executeCommand(command);
			}
			catch (Exception e)
			{
				log("%s error sending command", client);
				e.printStackTrace();
			}
		}
	}
	
	void killClients()
	{
		for (RemoteClient client : new ArrayList<RemoteClient>(clients))
		{
			try
			{
				client.kill();
			}
			catch (RemoteException e)
			{
				log("error killing "+client);
			}
		}
		clients.clear();
		free.clear();
	}
	
	public synchronized void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		gui.logText(timeStamp+" "+String.format(format, args)+"\n");
	}
	
	@Override
	public void connect(RemoteClient client)
	{
		try
		{
			log("%s connected", getClientHost());
		} catch (ServerNotActiveException e) {}
		clients.add(client);
		free.add(client);
	}
	
	@Override
	public void disconnect(RemoteClient client)
	{
		try
		{
			log("%s disconnected", getClientHost());
		}
		catch (ServerNotActiveException e) {}
		clients.remove(client);
		free.remove(client);
		gui.RemoveClient(client.toString());
	}

	@Override
	public void checkAlive()
	{
	}
	
	
	
	
	
	class RunningMatch implements Runnable
	{
		public final ServerGame game;
		private final List<RemoteClient> players;
		private final List<RemoteStarcraft> starcrafts = new ArrayList<>();
		private long startTime;
		
		RunningMatch(ServerGame game, List<RemoteClient> players)
		{
			this.game = game;
			this.players = players;
		}
		
		@Override
		public void run()
		{
			boolean success = false;
			try
			{
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					Bot bot = game.bots[i];
					
//					player.delete("$starcraft/bwapi-data/");
//					player.delete("$starcraft/maps/");
					
				}
				
				startTime = System.currentTimeMillis();
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					gui.UpdateRunningStats(player, game, i, startTime);
					starcrafts.add(player.startMatch(game, i));
				}
				
//				while (true)
//				{
//					boolean allStarted = true;
//
//					for (RemoteClient player : players)
//						allStarted &= player.starcraft().getStatus() != GameStatus.Starting;
//
//					if (allStarted)
//						break;
//
//					Thread.sleep((long) (env.matchFinishedPollPeriod*1000));
//				}
				
				//gameStarting.unlock();
				
				
				while (true)
				{
					boolean allDone = true;
					for (RemoteStarcraft starcraft : starcrafts)
						allDone &= starcraft.isFinished();
					if (allDone)
						break;
					Thread.sleep((long) (env.matchFinishedPollPeriod*1000));
				}
				
				log("%s finished, collecting replays", game);
				
				PackedFile[] writeDirs = new PackedFile[game.bots.length];
				GameResults results = new GameResults();
				PackedFile replay = null;
				for (int i=0; i<players.size(); i++)
				{
					RemoteStarcraft starcraft = starcrafts.get(i);
					writeDirs[i] = starcraft.getWriteDirectory();
					
					replay = starcraft.getReplay();
					Done done = starcraft.getResult();
					results.add(i, done);
				}
				game.reportResults(results, replay);
				
				//do this only after the results have been collected and recorded from everybody,
				//to minimize the risk that one bot learns more due to disconnecting clients
				for (int i=0; i<writeDirs.length; i++)
					writeDirs[i].writeTo(game.bots[i].getWriteDir(env));
				
				success = true;
			}
			catch (InterruptedException e)
			{
				log("match interrupted");
			}
			catch (StarcraftException e)
			{
				e.printStackTrace();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InvalidResultsException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (!success)
				{
					for (RemoteStarcraft starcraft : starcrafts)
					{
						try
						{
							starcraft.kill();
						}
						catch (RemoteException e)
						{
							log("error killing remote StarCraft");
							e.printStackTrace();
						}
					}
				}
				free.addAll(players);
				games.runningMatches.remove(this);
			}
		}
	}
	
	
	
	
	
	@Override
	public PackedFile getDataDir() throws IOException
	{
		return PackedFile.get(env.dataDir);
	}
}
