package server;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.BWAPISettings;
import common.Bot;
import common.Environment;
import common.Game;
import common.GameStatus;
import common.GameStorage;
import common.Helper;
import common.Map;
import common.PackedFile;
import common.RMIHelper;
import common.RunnableWithShutdownHook;
import common.TargetFile;
import common.exceptions.StarcraftException;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.utils.GameParser;
import common.utils.ResultsParser;

public class Server extends UnicastRemoteObject implements RemoteServer, RunnableWithShutdownHook
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	private ArrayList<RemoteClient> 		clients = new ArrayList<RemoteClient>();
    private ArrayList<RemoteClient> 		free = new ArrayList<RemoteClient>();
	private Lock							gameStarting = new ReentrantLock();
	private ArrayList<RunningMatch>			runningGames = new ArrayList<>();
	private GameStorage						games;
	
	private final int						gameRescheduleTimer = 2000;
	
	public ServerGUI 						gui;
	ImageWindow								imageWindow;
	
	private static Server INSTANCE = null;
	public final Environment env;

    public Server(Environment env) throws RemoteException
	{
    	if (INSTANCE != null)
    		throw new RuntimeException("can only create one server!");
    	else
    		INSTANCE = this;
    	
    	this.env = env;
    	gui = new ServerGUI(this);
    }

    public static Server Instance()
	{
        return INSTANCE;
    }

	@Override
	public void run()
	{
		RMIHelper.rebindAndHookUnbind(env.get("serverUrlPath"), this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		boolean resumed = gui.handleTournamentResume();
		gui.handleFileDialogues();
    	
		ArrayList<Bot> bots = env.get("bots");
		ArrayList<Map> maps = env.get("maps");
		
		games = GameParser.getGames(bots, maps);
		if (resumed)
		{
			ResultsParser rp = new ResultsParser(env.get("ResultsFile"));
			games.removePlayedGames(rp);
		}
		
		int neededClients = 2;
		int iterations = 0;
		Game previousScheduledGame = null;
		
		while (games.hasMoreGames())
		{
			// schedule a game once every few seconds
			sleep(gameRescheduleTimer);
			Game game = games.peekNextGame();
			
			try
			{
				writeHTMLFiles("index.html", iterations++);
				
				if (free.size() >= neededClients && gameStarting.tryLock())
				{
					// if new round
					if (previousScheduledGame != null && game.getRound() > previousScheduledGame.getRound())
					{
						int round = previousScheduledGame.getRound();
						// put some polling code here to wait until all games from this round are free
						if (!runningGames.isEmpty())
						{
							log("Waiting for ongoing games in round %d to finish", round);
							while (runningGames.isEmpty())
								sleep(gameRescheduleTimer);
						}
						
						log("Round %d finished, moving write directory to read directory", round);
				    	for (Bot bot : bots)
						{
							FileUtils.copyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
							//String copy = "xcopy " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/*.* " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/ /E /V /Y";
							//WindowsCommandTools.RunWindowsCommand(copy, true, false);
							//WindowsCommandTools.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/", ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/");
				    	}
					}
					
			    	RemoteClient host = free.remove(0);
			    	RemoteClient away = free.remove(0);
			    	RunningMatch runningGame = new RunningMatch(game, host, away);
			    	runningGames.add(runningGame);
			    	new Thread(runningGame).start();
					
					log(game + " starting");
			    	previousScheduledGame = game;
					games.advanceToNextGame();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				log(e.toString());
			}
		}
		
		if (previousScheduledGame == null)
			log("No more games in games list");
		else
		{
			log("No more games in games list, waiting for all ongoing games to finish");
			while (!runningGames.isEmpty())
				sleep(gameRescheduleTimer);
		}
		log("Done");
	}
	
	@Override
	public void onExit()
	{
		killClients();
	}
	
	public synchronized void writeHTMLFiles(String filename, int iter) throws Exception
	{
		try
		{
			ResultsParser rp = new ResultsParser(env.get("ResultsFile"));
			
			String schedulerHTML = gui.getHTML();
			String entrantsHTML = rp.getEntrantsHTML();
			String headerHTML = rp.getHeaderHTML();
			String footerHTML = rp.getFooterHTML();
			String resultsHTML = rp.getResultsHTML();
			
			// if there are no clients, don't both writing the current scheduler info
			if (clients.size() == 0)
			{
				schedulerHTML = "";
			}
			
			// only write the all results file every 30 reschedules, saves time
			if (env.get("DetailedResults").toString().equalsIgnoreCase("yes") && iter % 30 == 0)
			{
				log("Generating All Results File...");
				writeHTMLFile(rp.getAllResultsHTML(), "html/results.html");
				log("Generating All Results File Complete!");
			}
			
			writeHTMLFile(headerHTML + entrantsHTML + schedulerHTML + resultsHTML + footerHTML, "html/index.html");
		}
		catch (Exception e)
		{
			
		}
	}
	
	public void writeHTMLFile(String html, String filename) throws Exception
	{
		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(html);
		bw.close();
		fw.close();
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
		for (RemoteClient client : clients)
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
    }
    
    private synchronized void appendGameData(Game game)
	{
    	System.out.println("Writing out replay data for gameID " + game.getGameID());
        try
		{
            FileWriter fstream = new FileWriter(env.lookupFile("$ResultsFile"), true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(game.getResultString());
            out.close();
        }
		catch (Exception e)
		{
			e.printStackTrace();
        }
    }
    
	public synchronized void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		gui.logText(timeStamp+" "+String.format(format, args)+"\n");
	}
    
	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {}
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
		} catch (ServerNotActiveException e) {}
        clients.remove(client);
        free.remove(client);
		gui.RemoveClient(client.toString());
	}
	
	
	
	
	
	class RunningMatch implements Runnable
	{
		private final Game game;
		private final RemoteClient[] players;
		private long startTime;
		
		RunningMatch(Game game, RemoteClient... players)
		{
			this.game = game;
			this.players = players;
		}
		
		@SuppressWarnings("unchecked")
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
					
					for (TargetFile file : (Iterable<TargetFile>) env.get("common_files"))
						player.extractFile(PackedFile.get(file), file.extractTo);
			        
					for (TargetFile file : bot.requiredFiles)
			        	player.extractFile(PackedFile.get(file), file.extractTo);
			        
			        player.extractFile(PackedFile.get(bot.getDir(env)), "$starcraft/bwapi-data/");
			        player.extractFile(PackedFile.get(game.map.getFile(env)), "$starcraft/maps/"+game.map.path);
			        player.extractFile(PackedFile.get(new File("data/characters/default.mpc")), "$starcraft/characters/"+bot.name+".mpc");
			        player.extractFile(PackedFile.get(new File("data/characters/default.mpc")), "$starcraft/characters/"+bot.name+".mpc");
			        
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
				
		        game.setStatus(GameStatus.RUNNING);
		        game.startTime();
		        
		        while (true)
		        {
		        	boolean allDone = true;
		        	
		        	for (RemoteClient player : players)
		        		allDone &= player.starcraft().isDone();
		        	
		        	if (allDone)
		        		break;
		        }
		        
		        log("%s finished, collecting replays", game);
		        
				for (int i=0; i<players.length; i++)
				{
					RemoteClient player = players[i];
					Bot bot = game.bots[i];
					player.getFile("$starcraft/maps/replays/").writeTo(env.lookupFile("$replays/"));
					player.getFile("$starcraft/bwapi-data/write/").writeTo(env.lookupFile("$bot_dir/"+bot.name+"/write/"));
				}
				
				Game g = games.lookupGame(game.getGameID(), game.getRound());
				g.updateWithGame(game);
				appendGameData(g);
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
			finally
			{
	        	for (RemoteClient player : players)
	        	{
					free.add(player);
					
					try
					{
						player.starcraft().kill();
					}
					catch (RemoteException e)
					{
						System.err.println("error killing remote starcraft");
						e.printStackTrace();
					}
				}
				runningGames.remove(this);
			}
	    }
	}
}
