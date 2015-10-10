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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.Bot;
import common.RunnableUnicastRemoteObject;
import common.file.PackedFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.utils.Helper;
import server.ServerGame.ServerGameState;
import server.exceptions.NotEnoughStarcraftInstancesCouldBeStartedException;

public class Server extends RunnableUnicastRemoteObject implements RemoteServer
{
	private static final long serialVersionUID = -6886770266188997347L;
	
	
	public final ServerEnvironment	env;
	public final GameQueueManager	gameQueueManager;
	public final ClientManager		clientManager;
	public final ServerGUI			gui;
	
	private ImageWindow imageWindow = null;
	private Registry registry = null;
	
	public Server(ServerEnvironment env) throws RemoteException
	{
		this.env = env;
		this.clientManager = new ClientManager(this);
		this.gameQueueManager = new GameQueueManager(this);
		this.gui = new ServerGUI(this);
	}
	
	@Override
	public synchronized void onRun() throws RemoteException, MalformedURLException, InterruptedException, IOException
	{
		registry = LocateRegistry.createRegistry(env.port);
		registry.rebind(env.serverUrlPath, this);
		log("server listening on '%s'", Helper.getEndpointAddress(this));
		
		int finishedRound = -1;
		int printedWaitingForRoundToFinish = -1;
		ServerGame prevGame = null;
		while (!gameQueueManager.allDone())
		{
			ServerGame nextGame = gameQueueManager.getNextUnstartedGame();
			
			// if new round
			if (prevGame != null && (nextGame == null || nextGame.game.round > prevGame.game.round) && prevGame.game.round != finishedRound)
			{
				
				if (gameQueueManager.running().count() == 0 && finishedRound < prevGame.game.round)
				{
					log("Round %d finished, moving write directory to read directory", prevGame.game.round);
					for (Bot bot : gameQueueManager.getAllBots())
					{
						if (bot.getWriteDir(env).exists())
							FileUtils.copyDirectory(bot.getWriteDir(env), bot.getReadDir(env));
					}
					
					finishedRound = prevGame.game.round;
					continue;
				}
				else if (printedWaitingForRoundToFinish < prevGame.game.round)
				{
					log("Waiting for ongoing games in round %d to finish", prevGame.game.round);
					printedWaitingForRoundToFinish = prevGame.game.round;
				}
			}
			else if (nextGame != null && clientManager.getOpenSlotCount() >= nextGame.game.bots.length)
			{
				try
				{
					nextGame.start();
					prevGame = nextGame;
				}
				catch (NotEnoughStarcraftInstancesCouldBeStartedException e)
				{
					log("error starting %s, not enough starcraft instances could be started", nextGame);
				}
			}
			
			wait((long)(env.gameReschedulePeriod*1000));
		}
		
		log("Done");
		if (!env.exitWhenDone)
			Thread.sleep(Long.MAX_VALUE);
	}

	@Override
	public void onExit() throws RemoteException, AccessException, NotBoundException
	{
		if (env.killClientsOnExit)
			clientManager.killAll();
		
		if (registry != null)
			registry.unbind(env.serverUrlPath);
		
		if (gui != null)
			gui.mainFrame.dispose();
	}
	
	public synchronized void onMatchDone(ServerGame serverGame)
	{
		if (serverGame.state() == ServerGameState.DONE)
			log("%s: done", serverGame);
		else
			log("%s: error", serverGame);
		notifyAll();
	}
	
	public void getAndDisplayScreenshotFromClient(ProxyClient client)
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
	
	public synchronized void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		gui.logText(timeStamp+" "+String.format(format, args)+"\n");
	}
	
	@Override
	public void connect(RemoteClient client) throws RemoteException
	{
		clientManager.connected(client);
	}
	
	@Override
	public void disconnect(RemoteClient client)
	{
		clientManager.clientCalledDisconnect(client);
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
