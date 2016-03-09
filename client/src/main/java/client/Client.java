package client;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Game;
import common.RunnableUnicastRemoteObject;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyBusyException;
import common.file.CopyFile;
import common.file.MyFile;
import common.file.PackedFile;
import common.protocols.RemoteClient;
import common.protocols.RemoteServer;
import common.protocols.RemoteStarcraft;
import common.utils.Helper;
import common.utils.WindowsCommandTools;
import common.yaml.MyConstructor;

public class Client extends RunnableUnicastRemoteObject implements RemoteClient
{
	private static final long serialVersionUID = 3058199010003999345L;
	
	
	
	public final ClientEnvironment env;
	private RemoteServer server = null;
	private ArrayList<Starcraft> runningStarcrafts = new ArrayList<>();
	public CommonClientEnvironment commonEnv = new CommonClientEnvironment();

	public Client(ClientEnvironment env) throws RemoteException
	{
		this.env = env;
	}
	
	@Override
	public synchronized void onRun() throws InterruptedException, IOException, AllStarcraftInstanceSlotsAlreadyBusyException
	{
		if (env.killOtherStarcraftProcessesOnStartup)
			WindowsCommandTools.killProcess("StarCraft.exe");
		if (env.addStarcraftFirewallExceptionOnStartup)
			WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = " + new File(env.starcraftDir, "starcraft.exe").getAbsolutePath() + " name = Starcraft mode = ENABLE scope = ALL", true, false);
		
		log("connecting to server '%s'", env.serverUrl);
		while (true)
		{
			server = null;
			
			for (int attempts=0; server==null; attempts++)
			{
				try
				{
					server = (RemoteServer) Naming.lookup(env.serverUrl);
					if (attempts > 0)
						log("");
				}
				catch (RemoteException | NotBoundException e)
				{
					if (attempts == 0)
					{
						if (e.getMessage().matches("Connection refused to host: .*?; nested exception is: \n\tjava.net.ConnectException: Connection refused: connect"))
							log("error: connection refused");
						else if (e instanceof NotBoundException)
							log("error: not bound %s", e.getMessage());
						else
							log("error: %s", e.getMessage());
					}
					log("retry %d failed...\r", attempts);
	
					Thread.sleep(1000);
				}
			}
			log("found server, getting files");
			
			PackedFile serverDataDir = null;
			try
			{
				serverDataDir = server.getDataDir();
			}
			catch (IOException e)
			{
				log("error getting server's data dir, reconnecting");
				e.printStackTrace();
				continue;
			}
			
			serverDataDir.syncTo(env.dataDir); //don't recover if this throws
			log("got files");
			
			loadCommonEnv();

			for (CopyFile file : commonEnv.copyFiles)
				file.copyDiffering(env.lookupFile(file.extractTo));
			
			try
			{
				server.connect(this);
				log("connected");
				
				while (true)
				{
					Thread.sleep((long) (env.serverAlivePollPeriod*1000));
					server.checkAlive();
				}
			}
			catch (RemoteException e)
			{
				log("server disconnected, reconnecting");
			}
			
			killAllStarcrafts("server disconnected");
		}
	}
	
	@Override
	public void onExit()
	{
		if (server != null)
		{
			try
			{
				server.disconnect(this); //courtesy
			}
			catch (RemoteException e)
			{
			}
		}
		
		killAllStarcrafts("client exit");
	}
	
	private void loadCommonEnv() throws IOException
	{
		MyFile commonEnvFile = new MyFile(env.dataDir, "clientcommon.yaml");
		if (!commonEnvFile.exists())
			log("'%s' not found, ignoring", commonEnvFile);
		else
		{
			String commonEnvString = FileUtils.readFileToString(commonEnvFile);
			Yaml yaml = new Yaml(new MyConstructor(env));
			commonEnv = yaml.loadAs(commonEnvString, CommonClientEnvironment.class);
		}
	}

	@Override
	public RemoteStarcraft hostMatch(Game game, int index) throws RemoteException, AllStarcraftInstanceSlotsAlreadyBusyException
	{
		return joinMatch(game, index, null);
	}
	
	@Override
	public RemoteStarcraft joinMatch(Game game, int index, RemoteStarcraft host) throws RemoteException, AllStarcraftInstanceSlotsAlreadyBusyException
	{
		if (getOpenStarcraftInstanceSlotCount() <= 0)
			throw new AllStarcraftInstanceSlotsAlreadyBusyException(env.maxStarcraftInstances);
		Starcraft starcraft = new Starcraft(this, game, index, host);
		runningStarcrafts.add(starcraft);
		new Thread(starcraft).start();
		return starcraft;
	}

	public void onStarcraftExit(Starcraft starcraft)
	{
		runningStarcrafts.remove(starcraft);
	}
	
	private void killAllStarcrafts(String reason)
	{
		if (!runningStarcrafts.isEmpty())
		{
			log("killing all starcraft instances (%d): %s", runningStarcrafts.size(), reason);
			for (Starcraft starcraft : new ArrayList<Starcraft>(runningStarcrafts))
				starcraft.kill("");
		}
	}
	
	@Override
	public int getOpenStarcraftInstanceSlotCount()
	{
		return env.maxStarcraftInstances - runningStarcrafts.size();
	}
	
	@Override
	public void checkAlive()
	{
	}
	
	@Override
	public void kill(String reason)
	{
		if (thread() != null && thread().isAlive())
		{
			if (reason==null)
				reason = "reason=null";
			
			if (!reason.isEmpty())
				log("killed: "+reason);
			
			thread().interrupt();
		}
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
	public String endpointAddress()
	{
		return Helper.getEndpointAddress(this);
	}
	
	@Override
	public String toString()
	{
		return String.format("{Client %s}", Helper.getEndpointAddress(this));
	}
}
