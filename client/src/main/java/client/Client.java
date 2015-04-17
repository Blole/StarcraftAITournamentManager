package client;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import common.Helper;
import common.RunnableUnicastRemoteObject;
import common.file.PackedFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.protocols.RemoteStarcraft;
import common.utils.WindowsCommandTools;

public class Client extends RunnableUnicastRemoteObject implements RemoteClient
{
	private static final long serialVersionUID = 3058199010003999345L;
	
	
	
	public final ClientEnvironment env;
	private RemoteServer server = null;
	private final Starcraft starcraft = new Starcraft(this);
	private Thread thread = null;

	public Client(ClientEnvironment env) throws RemoteException
	{
		this.env = env;
	}
	
	@Override
	public synchronized void onRun() throws InterruptedException, MalformedURLException
	{
		thread = Thread.currentThread();
		if (env.killOtherStarcraftProcessesOnStartup)
			WindowsCommandTools.killProcess("StarCraft.exe");
		if (env.addWindowsRegistryEntriesOnStartup)
			starcraft.addWindowsRegistryEntries();
		if (env.addStarcraftFirewallExceptionOnStartup)
			WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = " + new File(env.starcraftDir, "starcraft.exe").getAbsolutePath() + " name = Starcraft mode = ENABLE scope = ALL", true, false);
		
		while (true)
		{
			int attempts = 0;
			log("connecting to server '%s'", env.serverUrl);
			
			while (server == null)
			{
				try
				{
					server = (RemoteServer) Naming.lookup(env.serverUrl);
					server.connect(this);
				}
				catch (RemoteException | NotBoundException e)
				{
					if (attempts == 0)
						log("error connecting to server:\n%s", e.getMessage());
					log("retry %d failed...\r", attempts);
					attempts++;
	
					Thread.sleep(1000);
				}
			}
			
			if (attempts > 0)
				log("");
			
			log("connected");
			log("ready");
			
			try
			{
				while (server.isAlive())
					Thread.sleep((long) (env.serverAlivePollPeriod*1000));
			}
			catch (RemoteException e) {}
			
			log("server disconnected");
			server = null;
		}
	}
	
	@Override
	public void onExit() throws RemoteException
	{
		starcraft.kill();
		
		unexportObject(starcraft, true);
		
		if (server != null)
			server.disconnect(this);
	}
	
	@Override
	public RemoteStarcraft starcraft() throws RemoteException
	{
		return starcraft;
	}

	@Override
	public void delete(String path) throws RemoteException, IOException
	{
		File file = env.lookupFile(path);
		
		log("deleted %s", file);
		
		if (file.isDirectory())
			FileUtils.deleteDirectory(file);
		else
			file.delete();
	}
	
	@Override
	public PackedFile getFile(String path) throws IOException
	{
		PackedFile file = PackedFile.get(env.lookupFile(path));
		log("sent " + path);
		return file;
	}

	@Override
	public void extractFile(PackedFile file, String extractTo) throws IOException
	{
		log("unpacked %20s to %-30s", file.name, extractTo);
		file.writeTo(env.lookupFile(extractTo));
	}
	
	@Override
	public boolean isAlive()
	{
		return true;
	}
	
	@Override
	public void kill()
	{
		log("killed by server");
		if (thread != null)
			thread.interrupt();
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
	public void executeCommand(String command)
	{
		log("server told us to execute command: " + command);
		WindowsCommandTools.RunWindowsCommandAsync(command);
	}

	public static void log(String format, Object... args)
	{
		String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
		String line = timeStamp+" "+String.format(format, args);
		System.out.print(line + (line.endsWith("\r")?"":"\n"));
	}
	
	@Override
	public String toString()
	{
		return String.format("[Client %s]", Helper.getEndpointAddress(this));
	}

}
