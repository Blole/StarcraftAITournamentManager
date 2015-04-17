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
import org.yaml.snakeyaml.error.YAMLException;

import common.Bot;
import common.BwapiSettings;
import common.Game;
import common.GameResult;
import common.Helper;
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
import common.yaml.GameRepresenter;

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
	private Registry registry = null;
	Thread thread = null;
	
	public Server(ServerEnvironment env) throws RemoteException
	{
		this.env = env;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onRun() throws RemoteException, MalformedURLException, InterruptedException, IOException
	{
		thread = Thread.currentThread();
		String gamesFileText = FileUtils.readFileToString(env.gameList);
		games = new Yaml(new GameConstructor(env.botDir)).loadAs(gamesFileText, List.class);
		
		tryWriteResults();
		
		gui = new ServerGUI(this);
		
		registry = LocateRegistry.createRegistry(env.port);
		registry.rebind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		int finishedRound = -1;
		int printedWaitingForRoundToFinish = -1;
		Game prevGame = null;
		Game nextGame = null;
		while (!games.stream().allMatch(g-> g.results != null))
		{
			nextGame = getNextUnstartedGame(games, runningMatches);
			
			// if new round
			if (prevGame != null && (nextGame == null || nextGame.round > prevGame.round))
			{
				
				if (runningMatches.isEmpty() && finishedRound < prevGame.round)
				{
					log("Round %d finished, moving write directory to read directory", prevGame.round);
					for (Bot bot : getAllBots(games))
						FileUtils.copyDirectory(bot.getWriteDir(env.botDir), bot.getReadDir(env.botDir));
					
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
					runningMatches.add(runningGame);
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
				if (player.isAlive())
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

	public void tryWriteResults()
	{
		try
		{
			Yaml yaml = new Yaml(new GameRepresenter());
			String yamlText = yaml.dump(games);
			FileUtils.writeStringToFile(env.results, yamlText);
		}
		catch (YAMLException | IOException e)
		{
			System.err.println("error writing to results file");
			e.printStackTrace();
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
	public boolean isAlive()
	{
		return true;
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
			try
			{
				BwapiSettings defaultBwapiSettings = new BwapiSettings(env.defaultBwapiIni);
				
				for (int i=0; i<players.size(); i++)
				{
					RemoteClient player = players.get(i);
					Bot bot = game.bots[i];
					
					player.delete("$starcraft/bwapi-data/");
					player.delete("$starcraft/characters/");
					player.delete("$starcraft/maps/");
					
					for (TargetFile file : env.filesToCopyToClientBeforeEachNewMatch)
						player.extractFile(PackedFile.get(file), file.extractTo);
					
					TargetFile bwapi = bot.bwapiVersion.getFile(env.bwapiVersions);
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
					
					Thread.sleep((long) (env.matchFinishedPollPeriod*1000));
				}
				
				//gameStarting.unlock();
				
				while (true)
				{
					boolean allDone = true;
					
					for (RemoteClient player : players)
						allDone &= player.starcraft().getStatus() == GameStatus.Done;
					
					if (allDone)
						break;
					
					Thread.sleep((long) (env.matchFinishedPollPeriod*1000));
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
				log("match interrupted");
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
				tryWriteResults();
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
