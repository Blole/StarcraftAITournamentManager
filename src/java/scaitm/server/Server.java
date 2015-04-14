package server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Bot;
import common.BwapiSettings;
import common.Game;
import common.GameResult;
import common.Helper;
import common.RMIHelper;
import common.RunnableUnicastRemoteObject;
import common.exceptions.InvalidBwapiVersionString;
import common.exceptions.InvalidResultsException;
import common.exceptions.StarcraftException;
import common.file.PackedFile;
import common.file.TargetFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.status.GameStatus;
import common.yaml.GameConstructor;

public class Server extends RunnableUnicastRemoteObject implements RemoteServer
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	private ArrayList<RemoteClient> 		clients = new ArrayList<RemoteClient>();
	private ArrayList<RemoteClient> 		free = new ArrayList<RemoteClient>();
	private Lock							gameStarting = new ReentrantLock();
	private ArrayList<RunningMatch>			runningMatches = new ArrayList<>();
	private List<Game>						games;
	
	private final int						gameRescheduleTimer = 2000;
	
	public ServerGUI 						gui;
	ImageWindow								imageWindow;
	
	public final ServerEnvironment env;

	public Server(ServerEnvironment env) throws RemoteException
	{
		this.env = env;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onRun()
	{
		try
		{
			String gamesFileText = FileUtils.readFileToString(env.gameList);
			games = new Yaml(new GameConstructor(env.botDir)).loadAs(gamesFileText, List.class);
		}
		catch (IOException e)
		{
			throw new RuntimeException("error reading games file '"+env.gameList+"'", e);
		}
		
		gui = new ServerGUI(this);
		RMIHelper.rebindAndHookUnbind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		try
		{
			Game previousScheduledGame = null;
			Game nextGame = null;
			while ((nextGame = getNextUnstartedGame(games, runningMatches)) != null)
			{
				try
				{
					if (free.size() >= nextGame.bots.length && gameStarting.tryLock())
					{
						// if new round
						if (previousScheduledGame != null && nextGame.round > previousScheduledGame.round)
						{
							int round = previousScheduledGame.round;
							if (!runningMatches.isEmpty())
							{
								log("Waiting for ongoing games in round %d to finish", round);
								while (runningMatches.isEmpty())
									Thread.sleep(gameRescheduleTimer);
							}
							
							log("Round %d finished, moving write directory to read directory", round);
							for (Bot bot : getAllBots(games))
								FileUtils.copyDirectory(bot.getWriteDir(env.botDir), bot.getReadDir(env.botDir));
						}
						
						List<RemoteClient> players = new ArrayList<>(free.subList(0, nextGame.bots.length));
						free.removeAll(players);
						
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
		private final List<RemoteClient> players;
		private long startTime;
		
		RunningMatch(Game game, List<RemoteClient> players)
		{
			this.game = game;
			this.players = players;
		}
		
		@Override
		public void run()
		{
			boolean success = false;
			BwapiSettings defaultBwapiSettings = new BwapiSettings(env.defaultBwapiIni);
			try
			{
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					Bot bot = game.bots[i];
					
					player.delete("$starcraft/bwapi-data/");
					player.delete("$starcraft/characters/");
					player.delete("$starcraft/maps/");
					
					for (TargetFile file : env.filesToCopyToClientBeforeEachNewMatch)
						player.extractFile(PackedFile.get(file), file.extractTo);
					
					TargetFile bwapi = bot.bwapiVersion.getFile(env);
					player.extractFile(PackedFile.get(bwapi), bwapi.extractTo);
					
					if (bot.extraFiles != null)
					{
						for (TargetFile file : bot.extraFiles)
							player.extractFile(PackedFile.get(file), file.extractTo);
					}
					
					player.extractFile(PackedFile.get(bot.getDir(env.botDir)), "$starcraft/bwapi-data/");
					player.extractFile(PackedFile.get(game.map.getFile(env.mapDir)), "$starcraft/maps/"+game.map.path);
					player.extractFile(PackedFile.get(env.characterFileMultiplayer), "$starcraft/characters/"+bot.name+".mpc");
					player.extractFile(PackedFile.get(env.characterFileSingleplayer), "$starcraft/characters/"+bot.name+".spc");
					
					BwapiSettings bwapiSettings = defaultBwapiSettings.clone();
					bwapiSettings.setGame(game, i);
					player.extractFile(new PackedFile("bwapi.ini", bwapiSettings.getContentsString().getBytes()), "$starcraft/bwapi-data/");
				}
				
				startTime = System.currentTimeMillis();
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					gui.UpdateRunningStats(player, game, i, startTime);
					player.starcraft().start(game, i);
				}
				
				while (true)
				{
					boolean allStarted = true;
					
					for (RemoteClient player : players)
						allStarted &= player.starcraft().getStatus() != GameStatus.Starting;
					
					if (allStarted)
						break;
					
					Thread.sleep((long) (env.pollPeriod*1000));
				}
				
				//gameStarting.unlock();
				
				while (true)
				{
					boolean allDone = true;
					
					for (RemoteClient player : players)
						allDone &= player.starcraft().getStatus() == GameStatus.Done;
					
					if (allDone)
						break;
					
					Thread.sleep((long) (env.pollPeriod*1000));
				}
				
				log("%s finished, collecting replays", game);
				
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					Bot bot = game.bots[i];
					player.getFile("$starcraft/maps/replays/").writeTo(env.replaysDir);
					player.getFile("$starcraft/bwapi-data/write/").writeTo(bot.getWriteDir(env.botDir));
					if (game.results == null)
						game.results = new GameResult();
					game.results.add(bot, player.starcraft().getResult());
				}
				
				success = true;
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
			catch (InvalidResultsException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (!success)
				{
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
				}
				free.addAll(players);
				runningMatches.remove(this);
			}
		}
	}
	
	
	
	
	
	private static Game getNextUnstartedGame(Collection<Game> games, Collection<RunningMatch> runningMatches)
	{
		LinkedHashSet<Game> unstartedGames = new LinkedHashSet<>(games);
		unstartedGames.removeIf(g -> g.results != null);
		unstartedGames.removeAll(getRunningGames(runningMatches));
		
		if (unstartedGames.isEmpty())
			return null;
		else
			return unstartedGames.iterator().next();
	}
	
	private static List<Game> getRunningGames(Collection<RunningMatch> runningMatches)
	{
		return runningMatches.stream().map(g -> g.game).collect(Collectors.toList());
	}
	
	private static Set<Bot> getAllBots(List<Game> games)
	{
		return games.stream()
				.flatMap(g -> Arrays.asList(g.bots).stream())
				.collect(Collectors.toSet());
	}
}
