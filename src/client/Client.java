package client;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import common.Environment;
import common.Helper;
import common.InstructionMessage;
import common.PackedFile;
import common.RMIHelper;
import common.RunnableWithShutdownHook;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.protocols.RemoteStarcraftGame;
import common.utils.FileUtils;
import common.utils.WindowsCommandTools;

@SuppressWarnings("serial")
public class Client extends UnicastRemoteObject implements RemoteClient, RunnableWithShutdownHook
{
	public final Environment env;
	private RemoteServer server = null;
	private StarcraftGame starcraftGame = null;

	public Client(Environment env) throws RemoteException
	{
		this.env = env;
	}

	
	@Override
	public synchronized void run()
	{
		String serverURL = "//"+env.get("ServerAddress")+"/scaitm_server";
		log("connecting to server '%s'", serverURL);
		server = (RemoteServer) RMIHelper.lookupAndWaitForRemoteToStartIfNecessary(serverURL, 1000);
		
		try
		{
			server.connect(this);
			log("connected");
		}
		catch (RemoteException e)
		{
			log("error connecting to server");
			e.printStackTrace();
			System.exit(0);
		}
		
		ClientCommands.Client_KillStarcraftAndChaoslauncher();
		ClientCommands.Client_DeleteChaoslauncherDirectory(env);
		ClientCommands.Client_CleanStarcraftDirectory(env);
		FileUtils.CleanDirectory(env.lookupFile("$starcraft/SentReplays/"));
		// Set up local firewall access
		//WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = " + env.get("starcraft") + "starcraft.exe name = Starcraft mode = ENABLE scope = ALL", true, false);
		
		log("ready");
		
		try
		{
			while (true)
				Thread.sleep(10000);
		}
		catch (InterruptedException e) {}
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
		if (starcraftGame != null)
			starcraftGame.kill();
	}
	
	@Override
	public RemoteStarcraftGame startStarcraftGame(InstructionMessage instructions) throws RemoteException
	{
		starcraftGame = new StarcraftGame(this, instructions);
		starcraftGame.start();
		return (RemoteStarcraftGame) exportObject(starcraftGame, 0);
	}
	
	@Override
	public void kill()
	{
		log("killed by server");
		System.exit(0);
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
	public PackedFile getFile(String path) throws IOException
	{
		PackedFile file = new PackedFile(env.lookupFile(path));
		log("sent " + file);
		return file;
	}

	@Override
	public void extractFile(PackedFile file, String extractTo) throws IOException
	{
		log("received " + file);
		file.writeTo(env.lookupFile(extractTo));
	}
	
	@Override
	public void executeCommand(String command)
	{
		log("server told us to execute command: " + command);
		WindowsCommandTools.RunWindowsCommandAsync(command);
	}

	public static void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp+" "+String.format(format, args));
	}
	
	@Override
	public String toString()
	{
		return String.format("[Client %s]", Helper.getEndpointAddress(this));
	}
}
