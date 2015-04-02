package server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.BWAPISettings;
import common.Bot;
import common.Game;
import common.GameStorage;
import common.Helper;
import common.PackedFile;
import common.RMIHelper;
import common.RunnableWithShutdownHook;
import common.TargetFile;
import common.exceptions.InvalidBwapiVersionString;
import common.exceptions.StarcraftException;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;

public class Server extends UnicastRemoteObject implements RemoteServer, RunnableWithShutdownHook
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	private ArrayList<RemoteClient> 		clients = new ArrayList<RemoteClient>();
    private ArrayList<RemoteClient> 		free = new ArrayList<RemoteClient>();
	private Lock							gameStarting = new ReentrantLock();
	private ArrayList<RunningMatch>			runningMatches = new ArrayList<>();
	private GameStorage						games;
	
	private final int						gameRescheduleTimer = 2000;
	
	public final ServerGUI 					gui = new ServerGUI(this);
	ImageWindow								imageWindow;
	
	public final ServerEnvironment env;

    public Server(ServerEnvironment env) throws RemoteException
	{
    	this.env = env;
    }

	@Override
	public void run()
	{
		RMIHelper.rebindAndHookUnbind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		try
		{
			String gamesFileText = FileUtils.readFileToString(env.lookupFile("$games"));
			games = new GameStorage(env, gamesFileText);
		}
		catch (IOException e)
		{
			throw new RuntimeException("error reading games file '"+env.lookupFile("$games")+"'", e);
		}
		
		try
		{
			Game previousScheduledGame = null;
			Game nextGame = null;
			while ((nextGame = getNextUnstartedGame()) != null)
			{
				try
				{
					if (free.size() >= nextGame.bots.length && gameStarting.tryLock())
					{
						// if new round
						if (previousScheduledGame != null && nextGame.round > previousScheduledGame.round)
						{
							int round = previousScheduledGame.round;
							// put some polling code here to wait until all games from this round are free
							if (!runningMatches.isEmpty())
							{
								log("Waiting for ongoing games in round %d to finish", round);
								while (runningMatches.isEmpty())
									Thread.sleep(gameRescheduleTimer);
							}
							
							log("Round %d finished, moving write directory to read directory", round);
					    	for (Bot bot : games.getAllBots())
							{
								FileUtils.copyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
								//String copy = "xcopy " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/*.* " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/ /E /V /Y";
								//WindowsCommandTools.RunWindowsCommand(copy, true, false);
								//WindowsCommandTools.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/", ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/");
					    	}
						}
						
						List<RemoteClient> players = new ArrayList<>();
						while (players.size() < nextGame.bots.length)
							players.add(free.remove(0));
						
				    	RunningMatch runningGame = new RunningMatch(nextGame, players);
				    	runningMatches.add(runningGame);
				    	new Thread(runningGame).start();
						
						log(nextGame + " starting");
				    	previousScheduledGame = nextGame;
					}
				}
				catch (Exception e)
				{
					log(e.toString());
					e.printStackTrace();
				}
				
				Thread.sleep(gameRescheduleTimer);
			}
			
			
			if (previousScheduledGame == null)
				log("No more games in games list");
			else
			{
				log("No more games in games list, waiting for all ongoing games to finish");
				while (!runningMatches.isEmpty())
					Thread.sleep(gameRescheduleTimer);
			}
		}
		catch (InterruptedException e)
		{
			log("interrupted");
		}
		log("Done");
	}
	
	private Game getNextUnstartedGame()
	{
		LinkedHashSet<Game> unstartedGames = new LinkedHashSet<>(games.games);
		unstartedGames.removeIf(g -> !g.hasResults());
		unstartedGames.removeAll(getRunningGames());
		
		if (unstartedGames.isEmpty())
			return null;
		else
			return unstartedGames.iterator().next();
	}
	
	private List<Game> getRunningGames()
	{
		return runningMatches.stream().map(g -> g.game).collect(Collectors.toList());
	}

	@Override
	public void onExit()
	{
		if (env.killClientsOnExit)
			killClients();
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
	    		log(client + "error sending command");
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
				log("%s error killing", client);
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
	
	
	
	
	
	class RunningMatch implements Runnable
	{
		private final Game game;
		private final RemoteClient[] players;
		private long startTime;
		
		RunningMatch(Game game, List<RemoteClient> players)
		{
			this.game = game;
			this.players = new RemoteClient[players.size()];
			players.toArray(this.players);
		}
		
		@Override
		public void run()
		{
			BWAPISettings defaultBwapiSettings = new BWAPISettings(new File("data/bwapi.ini"));
			try
			{
				for (int i=0; i<players.length; i++)
				{
					RemoteClient player = players[i];
					Bot bot = game.bots[i];
					
					player.delete("$chaoslauncher");
					//player.delete("$starcraft/SentReplays/");
					player.delete("$starcraft/bwapi-data/");
					player.delete("$starcraft/characters/");
					player.delete("$starcraft/maps/");
					player.delete("$starcraft/gameState.txt");
					
					for (TargetFile file : env.filesToCopyToClientBeforeEachNewMatch)
						player.extractFile(PackedFile.get(file), file.extractTo);
			        
					TargetFile bwapi = bot.bwapiVersion.getFile(env);
					player.extractFile(PackedFile.get(bwapi), bwapi.extractTo);
					
					if (bot.extraFiles != null)
					{
						for (TargetFile file : bot.extraFiles)
				        	player.extractFile(PackedFile.get(file), file.extractTo);
					}
			        
			        player.extractFile(PackedFile.get(bot.getDir(env)), "$starcraft/bwapi-data/");
			        player.extractFile(PackedFile.get(game.map.getFile(env)), "$starcraft/maps/"+game.map.path);
			        player.extractFile(PackedFile.get(new File("data/characters/default.mpc")), "$starcraft/characters/"+bot.name+".mpc");
			        player.extractFile(PackedFile.get(new File("data/characters/default.spc")), "$starcraft/characters/"+bot.name+".spc");
			        
			        BWAPISettings bwapiSettings = defaultBwapiSettings.clone();
			        bwapiSettings.setGame(game, i);
			        player.extractFile(new PackedFile("bwapi.ini", bwapiSettings.getContentsString().getBytes()), "$starcraft/bwapi-data/");
				}
				
				startTime = System.currentTimeMillis();
				for (int i=0; i<players.length; i++)
				{
					gui.UpdateRunningStats(players[i], game, i, startTime);
					players[i].starcraft().start(game, i);
				}
				
		        while (true)
		        {
		        	boolean allDone = true;
		        	
		        	for (RemoteClient player : players)
		        		allDone &= player.starcraft().getResult() != null;
		        	
		        	if (allDone)
		        		break;
		        	
		        	Thread.sleep((long) (env.pollPeriod*1000));
		        }
		        
		        log("%s finished, collecting replays", game);
		        
				for (int i=0; i<players.length; i++)
				{
					RemoteClient player = players[i];
					Bot bot = game.bots[i];
					player.getFile("$starcraft/maps/replays/").writeTo(env.lookupFile("$replays/"));
					player.getFile("$starcraft/bwapi-data/write/").writeTo(bot.getWriteDir(env));
					game.results.add(player.starcraft().getResult());
				}
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
			catch (InterruptedException e)
			{
				log("interrupted");
			}
			catch (InvalidBwapiVersionString e)
			{
				e.printStackTrace();
			}
			finally
			{
				free.addAll(Arrays.asList(players));
				
	        	for (RemoteClient player : players)
	        	{
					try
					{
						player.starcraft().kill();
					}
					catch (RemoteException e)
					{
						log("error killing remote StarCraft");
						e.printStackTrace();
					}
				}
	        	
				runningMatches.remove(this);
			}
	    }
	}
}
