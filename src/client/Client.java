package client;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;

import javax.imageio.ImageIO;

import common.Environment;
import common.FileMessage;
import common.Helper;
import common.InstructionMessage;
import common.RMIHelper;
import common.RunnableWithShutdownHook;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.utils.WindowsCommandTools;

@SuppressWarnings("serial")
public class Client extends UnicastRemoteObject implements RemoteClient, RunnableWithShutdownHook
{
	private Vector<Integer> startingproc;
	
	public boolean 		shutDown = false;

	private RemoteServer server;
	public final Environment env;

	private boolean running = true;

	public Client(Environment env) throws RemoteException
	{
		this.env = env;
		startingproc = WindowsCommandTools.GetRunningProcesses();
	}

	
	@Override
	public synchronized void run()
	{
		String serverURL = "//"+env.get("ServerAddress")+"/scaitm_server";
		log("connecting to server '%s'", serverURL);
		server = (RemoteServer) RMIHelper.lookupAndWaitForRemoteToStartIfNecessary(serverURL, 1000);
		
		ClientCommands.Client_CleanStarcraftDirectory(env);
		
		try
		{
			server.connect(this);
		}
		catch (RemoteException e)
		{
			log("error connecting to server");
			e.printStackTrace();
			System.exit(0);
		}
		
		log("connected");
		ClientCommands.Client_InitialSetup(env);
		
		while (running)
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{}
		}
	}
	
	@Override
	public void onExit()
	{
		try
		{
			server.disconnect(this);
		}
		catch (RemoteException e)
		{
			log("error disconnecting from server");
		}
	}
	
	private boolean isProxyBot(InstructionMessage instructions)
	{
		if (instructions == null)
		{
			return false;
		}
		
		if (instructions.isHost)
		{
			return instructions.hostBot.isProxyBot();
		}
		else
		{
			return instructions.awayBot.isProxyBot();
		}
	}
	
	@Override
	public void startStarCraft(InstructionMessage instructions) throws RemoteException
	{
		try
		{
			startingproc = WindowsCommandTools.GetRunningProcesses();
			
			// Prepare the machine for Starcraft launching
			ClientCommands.Client_KillStarcraftAndChaoslauncher();
			
			// Rename the character files to match the bot names
			ClientCommands.Client_RenameCharacterFile(env, instructions);
			ClientCommands.Client_RegisterStarCraft(env);
			
			// Write out the BWAPI and TournamentModule settings files
			ClientCommands.Client_WriteBWAPISettings(env, instructions);
			
			File replayFile = env.lookupFile("$starcraft/maps/replays/LastReplay.rep");
			replayFile.delete();
			File gameStateFile = env.lookupFile("$starcraft/gameState.txt");
			gameStateFile.delete();
			
			// If this is a proxy bot, start the proxy bot script before StarCraft starts
			if (isProxyBot(instructions))
				ClientCommands.Client_RunProxyScript(env);
			
			// Start chaoslauncher and starcraft
			ClientCommands.Client_StartChaoslauncher(env);
	
			// Record the time that we tried to start the game
			long time = System.currentTimeMillis();
			
			while (!WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
			{
				if (System.currentTimeMillis()-time > env.get("starcraftStartingTimeout", Integer.class)*1000)
				{
					log("timeout starting StarCraft");
					throw new RemoteException("timeout starting StarCraft");
				}
				sleep(500);
			}
			log("starcraft started");
			
			time = System.currentTimeMillis();
			while (!gameStateFile.exists())
			{
				if (System.currentTimeMillis()-time > env.get("matchStartingTimeout", Integer.class)*1000)
				{
					log("timeout starting match");
					throw new RemoteException("timeout starting match");
				}
				sleep(1000);
			}
			log("match started");
			
			time = System.currentTimeMillis();
			while (!replayFile.exists())
			{
				if (System.currentTimeMillis()-time > env.get("matchEndingTimeout", Integer.class)*1000)
				{
					log("timeout ending match");
					throw new RemoteException("timeout ending match");
				}
				sleep(1000);
			}
			log("match ended");
			
			//sleep to make sure StarCraft wrote the replay file correctly
			sleep(5000);
			
			//String timeStamp = new SimpleDateFormat("[mm:ss]").format(new Date(gameState.frameCount*42));
		}
		finally
		{
			killProcesses();
		}
	}
	
	private void killProcesses()
	{
		ClientCommands.Client_KillStarcraftAndChaoslauncher();
		ClientCommands.Client_KillExcessWindowsProccess(startingproc);
	}
	
	@Override
	public void kill()
	{
		log("killed by server");
		killProcesses();
		running = false;
		notifyAll();
	}

	public static void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp+" "+String.format(format, args));
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
	public byte[] screenshot() throws RemoteException
	{
		try
		{
			Robot robot = new Robot();
		    Rectangle captureSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		    BufferedImage bufferedImage = robot.createScreenCapture(captureSize);
		    
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ImageIO.write(bufferedImage, "png", baos);
		    return baos.toByteArray();
		}
		catch (IOException | AWTException e)
		{
			log("error taking screenshot");
			throw new RemoteException("error taking screenshot", e);
		}
	}

	@Override
	public FileMessage getFile(String path, String extractTo) throws RemoteException
	{
		return new FileMessage(env.lookupFile(path), extractTo);
	}

	@Override
	public void extractFile(FileMessage msg)
	{
		msg.write(env);
	}
	
	@Override
	public void executeCommand(String command)
	{
		log("server told us to execute command: " + command);
		WindowsCommandTools.RunWindowsCommandAsync(command);
	}

	@Override
	public String toString()
	{
		return String.format("[Client %s]", Helper.getEndpointAddress(this));
	}
}
