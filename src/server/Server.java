package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

import objects.BWAPISettings;
import objects.Bot;
import objects.ClientCommandMessage;
import objects.ClientStatus;
import objects.Environment;
import objects.FileMessage;
import objects.Game;
import objects.GameStatus;
import objects.GameStorage;
import objects.InstructionMessage;
import objects.Map;
import objects.RequestClientScreenshotMessage;
import objects.ServerShutdownMessage;
import objects.StartGameMessage;
import objects.TournamentModuleState;
import utility.FileUtils;
import utility.GameParser;
import utility.ResultsParser;
import utility.WindowsCommandTools;

public class Server  extends Thread
{
    private Vector<ServerClientThread> 		clients;
    private Vector<ServerClientThread> 		free;
	
    private ServerListenerThread			listener;
	private GameStorage						games;
	
	private int								gameRescheduleTimer = 2000;
	
	public ServerGUI 						gui;
	
	private static Server INSTANCE = null;
	public final Environment env;

    public Server(Environment env)
	{
    	if (INSTANCE != null)
    		throw new RuntimeException("can only create one server!");
    	else
    		INSTANCE = this;
    	
    	this.env = env;
    	gui = new ServerGUI(this);
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
		
        clients 	= new Vector<ServerClientThread>();
        free 		= new Vector<ServerClientThread>();

		setupServer();
		
		listener = new ServerListenerThread(this);
		listener.start();
    }
    
    public static Server Instance()
	{
        return INSTANCE;
    }

	@Override
	public void run()
	{
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
				
				// we can't start a game if we don't have enough clients
				if (free.size() < neededClients)
					continue;
				// also don't start a game if a game is currently in the lobby
				else if (isAnyGameStarting())
					continue;
				
				// if new round
				if (previousScheduledGame != null && game.getRound() > previousScheduledGame.getRound())
				{
					int round = previousScheduledGame.getRound();
					// put some polling code here to wait until all games from this round are free
					if (free.size() < clients.size())
					{
						log("Waiting for ongoing games in round "+round+" to finish\n");
						while (free.size() < clients.size())
							sleep(gameRescheduleTimer);
					}
					
					log("Round "+round+" finished, moving write directory to read directory\n");
					Iterable<Bot> bots = env.get("bots");
			    	for (Bot bot : bots)
					{
						FileUtils.CopyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
						//String copy = "xcopy " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/*.* " + ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/ /E /V /Y";
						//WindowsCommandTools.RunWindowsCommand(copy, true, false);
						//WindowsCommandTools.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot.getName() + "/write/", ServerSettings.Instance().ServerBotDir + bot.getName() + "/read/");
			    	}
				}
						
		    	previousScheduledGame = game;
				log(game + " starting\n");
				start1v1Game(game);
				games.advanceToNextGame();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				log(e.toString() + "\n");
			}
		}
		
		if (previousScheduledGame == null)
			log("No more games in games list\n");
		else
		{
			log("No more games in games list, waiting for all ongoing games to finish\n");
			while (free.size() < clients.size())
				sleep(gameRescheduleTimer);
		}
		log("Done\n");
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
				log("Generating All Results File...\n");
				writeHTMLFile(rp.getAllResultsHTML(), "html/results.html");
				log("Generating All Results File Complete!\n");
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
	
	public synchronized void updateRunningStats(String client, TournamentModuleState state, boolean isHost)
	{
		int fpm = 24 * 60;
		int fps = 24;
		int minutes = state.frameCount / fpm;
		int seconds = (state.frameCount / fps) % 60;
		gui.UpdateRunningStats(	client,
								state.selfName,
								state.enemyName,
								state.mapName,
								"" + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds),
								state.selfWin == 1 ? "Victory" : "");
	}
	
		public synchronized void updateStartingStats(String client, int startingTime)
	{
		gui.UpdateRunningStats(	client,
								"",
								"",
								"",
								"" + startingTime + "s",
								"");
	}

	public synchronized void sendScreenshotRequestToClient(String client)
	{
		boolean found = false;
		
		try
		{
			for (int i = 0; i < clients.size(); i++)
			{
				if (clients.get(i).getAddress().toString().contains(client))
				{
					clients.get(i).sendMessage(new RequestClientScreenshotMessage());
					found = true;
				}
	        }
		}
		catch (Exception ex)
		{
			log("Screenshot Request: Error in searching for client\n");
		}
		
		if (!found)
		{
			log("Screenshot Request: Client not found\n");
		}
	}
		
	public synchronized void sendCommandToAllClients(String command)
	{
		ClientCommandMessage message = new ClientCommandMessage(command);
		
		log("Sending command to all clients: " + message.getCommand() + "\n");
		
		try
    	{
	        for (int i = 0; i < clients.size(); i++)
			{
	            clients.get(i).sendMessage(message);
	        }
    	}
    	catch (Exception e)
    	{
    		log("There was an error sending the client command message");
    	}
	}
		
	public synchronized void updateStatusTable()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			ServerClientThread c = clients.get(i);
			
			updateClientStatus(c);
        }
	}
	
	public synchronized void updateClientGUI(ServerClientThread c)
	{
		if (c != null)
		{
		
			String client = c.toString();
			String status = "" + c.getStatus();
			String gameNumber = "";
			String hostBotName = "";
			String awayBotName = "";
			
			InstructionMessage ins = c.lastInstructionSent;
			
			if (ins != null)
			{
				gameNumber = "" + ins.game_id + " / " + ins.round_id;
				hostBotName = ins.hostBot.name;
				awayBotName = ins.awayBot.name;
			}
			
			if (status.equals("READY"))
			{
				gameNumber = "";
				hostBotName = "";
				awayBotName = "";
			}
		
			gui.UpdateClient(client, status, gameNumber, hostBotName, awayBotName);
		}
	}
	
	public static String getTimeStamp()
	{
		return new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
	}
	
	public synchronized void log(String s)
	{
		gui.logText(getTimeStamp() + " " + s);
	}
	
	private synchronized void removeNonFreeClientsFromFreeList()
	{
		for (int i = 0; i < free.size(); i++)
		{
            if (free.get(i).getStatus() != ClientStatus.READY)
			{
                free.remove(i);
                log("AddClient(): Non-Free Client in Free List\n");
            }
        }
	}
	
	public synchronized void updateClientStatus(ServerClientThread c)
	{
		if (c != null)
		{
            if (!clients.contains(c))
			{
                clients.add(c);
                log("New Client Added: " + c.toString() + "\n");
            }
            if (c.getStatus() == ClientStatus.READY && !free.contains(c))
			{
                free.add(c);
                log("Client Ready: " + c.toString() + "\n");
            }
        }
	}
	 
    public synchronized boolean updateClient(ServerClientThread c)
	{
		// double check to make sure the free list is correct
        removeNonFreeClientsFromFreeList();
        
		// update this client's status in the list
		updateClientStatus(c);
		updateClientGUI(c);
		//updateStatusTable();
		
        return true;
    }
    
	private synchronized boolean isAnyGameStarting()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			ServerClientThread c = clients.get(i);
			
			if (c.getStatus() == ClientStatus.STARTING)
			{
				return true;
			}
		}
		
		return false;
	}
	
    /**
     * Handles all of the code needed to start a 1v1 game
     */
    private synchronized void start1v1Game(Game game) throws IOException
	{
		// remove the clients from the free list
		ServerClientThread hostClient = free.remove(0);
		ServerClientThread awayClient = free.remove(0);
		
		// set the clients to starting
        hostClient.setStatus(ClientStatus.STARTING);
		awayClient.setStatus(ClientStatus.STARTING);
		
		// send instructions and files to the host machine
        sendFiles(hostClient, game, true);
        sendFiles(awayClient, game, false);
        
		// start games on those machines
		hostClient.sendMessage(new StartGameMessage());
		awayClient.sendMessage(new StartGameMessage());
		
		// set the game to running
        game.setStatus(GameStatus.RUNNING);
        game.startTime();
		
		updateClientGUI(hostClient);
		updateClientGUI(awayClient);
    }
    
    private void sendFiles(ServerClientThread client, Game game, boolean isHost) throws IOException
    {
    	Bot bot = isHost ? game.getHomebot() : game.getAwaybot();
		
        client.sendMessage(new InstructionMessage(new BWAPISettings(), isHost, game));
        client.sendMessage(new FileMessage(env.lookupFile("$"+bot.bwapiVersion), "$starcraft/"));
        client.sendMessage(new FileMessage(env.lookupFile("$chaoslauncher"), "$chaoslauncher"));
        client.sendMessage(new FileMessage(env.lookupFile("$bot_dir/"+bot.name), "$starcraft/bwapi-data/"));
        client.sendMessage(new FileMessage(env.lookupFile("$map_dir/"+game.getMap().path), "$starcraft/maps/"));
        client.sendMessage(new FileMessage(env.lookupFile("$tm_settings"), "$starcraft/bwapi-data/"));
        //client.sendMessage(new FileMessage(env.lookupFile("$TournamentModule"), "$starcraft/bwapi-data/"));
    }
    
    void shutDown()
	{
    	try
    	{
	        for (int i = 0; i < clients.size(); i++)
			{
	            clients.get(i).sendMessage(new ServerShutdownMessage());
	        }
    	}
    	catch (Exception e)
    	{
    		
    	}
    	finally
    	{
    		System.exit(0);
    	}
    }
	
    public synchronized void receiveGameResults(Game game)
	{
		try
		{
			log("Recieving Replay: (" + game.getGameID() + " / " + game.getRound() + ")\n");				// EXCEPTION HERE
			System.out.println("Recieving Replay: (" + game.getGameID() + " / " + game.getRound() + ")\n");
			Game g = games.lookupGame(game.getGameID(), game.getRound());
			g.updateWithGame(game);
			appendGameData(g);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log("Error Receiving Game Results\n");
		}
    }

    public int getClientIndex(ServerClientThread c)
    {
    	return clients.indexOf(c);
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
    
    
    
    public void setupServer()
	{
		log("Server: Created, Running Setup...\n");
     	//runCommand("ServerSetup.bat");
		if (System.getProperty("os.name").contains("Windows"))
		{
			WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = server.jar name = AIIDEServer mode = ENABLE scope = ALL", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 12345 \"Open Port 12345TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 12345 \"Open Port 12345UDP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 1337 \"Open Port 1337TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 1337 \"Open Port 1337UDP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 11337 \"Open Port 11337TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 11337 \"Open Port 11337UDP\"", true, false);
		}
		log("Server: Setup Successful. Ready!\n");
    }

    synchronized public void removeClient(ServerClientThread c)
	{
        this.clients.remove(c);
        this.free.remove(c);
		
		gui.RemoveClient(c.toString());
		updateStatusTable();
    }

    synchronized public void killClient(String ip)
	{
        System.out.println("Attempting to kill client: " + ip);
        for (int i = 0; i < clients.size(); i++)
		{
            if (clients.get(i).getAddress().toString().contentEquals(ip))
			{
                System.out.println("Client Found and Stopped\n");
                free.remove(clients.get(i));
                clients.get(i).stopThread();
                clients.remove(i);
                return;
            }
        }
    }
	
	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
		}
	}
}
class FileCopyThread extends Thread
{
	String source;
	String dest;
	Server server;

	public FileCopyThread(Server m, String source, String dest)
	{
		this.source = source;
		this.dest = dest;
		server = m;
		
		server.log("File Copy Thread Initialized\n");
	}

	@Override
	public void run()
	{
		server.log("File Copy Thread Started()\n");
	
		while(true)
		{
			try
			{
				Thread.sleep(5000);
				server.log("Trying to copy file to web_docs\n");
				copyFileWindows(source, dest);
				server.log("SUCCESS   : " + source + " copied to " + dest + "\n");
				copyFileWindows(dest, "y:\\web_docs\\index.html");
				server.log("SUCCESS   : Final Copy\n");
			}
			catch (Exception e)
			{
				server.log("FAIL   : " + source + " not copied to " + dest + "\n");
			}
		}
	}

	public void copyFileWindows(String s, String d) throws Exception
	{
		String[] args = { "CMD", "/C", "COPY", "/Y", s, d };
		Process p = Runtime.getRuntime().exec(args);
		p.waitFor();
	}
	
	public void copyFile() throws IOException
	{
		File sourceFile = new File(source);
		File destFile = new File(dest);
	
		if(!destFile.exists())
		{
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try
		{
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally
		{
			if(source != null)
			{
				source.close();
			}
			if(destination != null)
			{
				destination.close();
			}
		}
	}
}
