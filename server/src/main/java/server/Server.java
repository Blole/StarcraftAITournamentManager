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

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.Bot;
import common.RunnableUnicastRemoteObject;
import common.file.PackedFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.utils.Helper;

public class Server extends RunnableUnicastRemoteObject implements RemoteServer
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	
	
	private ArrayList<RemoteClient> 		clients = new ArrayList<RemoteClient>();
	ArrayList<RemoteClient> 				free = new ArrayList<RemoteClient>();
	private GameQueueManager				games;
	public ServerGUI 						gui;
	ImageWindow								imageWindow;
	
	public final ServerEnvironment env;
	private Registry registry = null;
	
	public Server(ServerEnvironment env) throws RemoteException
	{
		this.env = env;
	}
	
	@Override
	public void onRun() throws RemoteException, MalformedURLException, InterruptedException, IOException
	{
		games = new GameQueueManager(this);
		gui = new ServerGUI(this);
		
		registry = LocateRegistry.createRegistry(env.port);
		registry.rebind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		int finishedRound = -1;
		int printedWaitingForRoundToFinish = -1;
		ServerGame prevGame = null;
		while (!games.allDone())
		{
			ServerGame nextGame = games.getNextUnstartedGame();
			
			// if new round
			if (prevGame != null && (nextGame == null || nextGame.game.round > prevGame.game.round) && prevGame.game.round != finishedRound)
			{
				
				if (games.running().count() == 0 && finishedRound < prevGame.game.round)
				{
					log("Round %d finished, moving write directory to read directory", prevGame.game.round);
					for (Bot bot : games.getAllBots())
						FileUtils.copyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
					
					finishedRound = prevGame.game.round;
					continue;
				}
				else if (printedWaitingForRoundToFinish < prevGame.game.round)
				{
					log("Waiting for ongoing games in round %d to finish", prevGame.game.round);
					printedWaitingForRoundToFinish = prevGame.game.round;
				}
			}
			else if (nextGame != null)
			{
				List<RemoteClient> players = getFree(nextGame.game.bots.length);
				if (players != null)
				{
					nextGame.start(players);
					prevGame = nextGame;
				}
			}
			
			Thread.sleep((long)(env.gameReschedulePeriod*1000));
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
	
	@Override
	public PackedFile getDataDir() throws IOException
	{
		return PackedFile.get(env.dataDir);
	}
	
	@Override
	public String toString()
	{
		return String.format("{Server %s}", Helper.getEndpointAddress(this));
	}
}
