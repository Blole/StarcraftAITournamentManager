package client;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Bot;
import common.BotExecutableType;
import common.CompleteGameResults;
import common.Game;
import common.RunnableUnicastRemoteObject;
import common.exceptions.BotCrashException;
import common.exceptions.BotTimeoutException;
import common.exceptions.StarcraftException;
import common.exceptions.StarcraftNotHostException;
import common.file.CopyFile;
import common.file.MyFile;
import common.file.PackedFile;
import common.file.RequiredFile;
import common.protocols.RemoteStarcraft;
import common.status.Crash;
import common.status.Done;
import common.status.GameStatusFile;
import common.status.Timeout;
import common.utils.Helper;
import common.utils.WindowsCommandTools;
import common.yaml.MyConstructor;

public class Starcraft extends RunnableUnicastRemoteObject implements RemoteStarcraft
{
	private static final long serialVersionUID = 5069349040098758632L;
	
	final private Client client;
	final private ClientEnvironment env;
	final private Game game;
	final private Bot bot;
	final private int playerIndex;
	
	final private RemoteStarcraft host;
	private int localPort = -1;
	
	private StarcraftException exception = null;
	private long startTime;
	
	private Done result = null;
	private PackedFile packedReplay = null;
	private PackedFile packedWriteDirectory = null;
	
	final private MyFile instanceDir;
	final private MyFile localPortFile;
	private Process starcraftProcess = null;

	private String interruptReason = null;

	public Starcraft(Client client, Game game, int playerIndex, RemoteStarcraft host) throws RemoteException
	{
		super(false); //don't unexport automatically
		this.client = client;
		this.env = client.env;
		this.game = game;
		this.playerIndex = playerIndex;
		this.bot = game.bots[playerIndex];
		this.host = host;
		instanceDir = new MyFile("game"+game.id+"_player"+playerIndex);
		localPortFile = new MyFile(instanceDir, "localport.txt");
	}
	
	@Override
	protected void onRun()
	{
		startTime = System.currentTimeMillis();
		try
		{
			FileUtils.forceMkdir(instanceDir);
			FileUtils.cleanDirectory(instanceDir);
			if (env.deleteInstanceDirs)
				FileUtils.forceDeleteOnExit(instanceDir);
			
			MyFile replayFile = new MyFile(instanceDir, "replay.rep");
			MyFile statusFile = new MyFile(instanceDir, env.gamestatusFileName);
			MyFile tournamentYaml = env.tournamentModuleYaml();
			RequiredFile injectory = new RequiredFile(env.dataDir, "injectory.x86.exe");
			
			// If this is a proxy bot, start the proxy bot script before StarCraft starts
			if (bot.type == BotExecutableType.proxy)
				WindowsCommandTools.RunWindowsCommand(new RequiredFile(env.dataDir, "run_proxy.bat").getAbsolutePath(), false, false);
			
			for (CopyFile file : bot.copyFiles)
				file.copyDiffering(env.lookupFile(file.extractTo));
			
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(injectory.toString(), "--launch", "starcraft_multiinstance.exe", "--inject", bot.bwapiVersion.getDll(env).getAbsolutePath(), "--kill-on-exit", "--wait-for-exit");
			pb.directory(env.starcraftDir);
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			
			// environment
			pb.environment().put("BWAPI_CONFIG_INI", 	    				env.iniFile().require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__AI",     				bot.getDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__AI_DBG", 				bot.getDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__TOURNAMENT",				bot.bwapiVersion.getTournamentDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__CHARACTER_NAME",	bot.nick);
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__AUTO_RESTART",	"EXIT");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__GAME",			game.id+"");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__GAME_TYPE",		game.type.toString());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__RACE",			bot.race.toString());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__SAVE_REPLAY",		replayFile.getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__WAIT_FOR_MIN_PLAYERS", game.bots.length+"");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__WAIT_FOR_MAX_PLAYERS", game.bots.length+"");
			// tournament environment config
			pb.environment().put("SCAITM_TOURNAMENT_CONFIG_FILE",			tournamentYaml.require().getAbsolutePath());
			pb.environment().put("SCAITM_TOURNAMENT_STATUS_FILE",			statusFile.getAbsolutePath());
			// direct IP
			pb.environment().put("DIRECT_IP_LOCAL_PORT_OUTPUT_FILE",		localPortFile.getAbsolutePath());
			
			// host/join specifics
			if (isHost())
			{
				pb.environment().put("BWAPI_CONFIG_AUTO_MENU__MAP",				env.starcraftDir.getRelativePath(game.map.getFile(env).require()).getPath());
				pb.environment().put("DIRECT_IP_HOST_IP",						"127.0.0.1");
				pb.environment().put("DIRECT_IP_HOST_PORT",						"0");
				pb.environment().put("DIRECT_IP_LOCAL_PORT",					"6112");

				log("starting starcraft, hosting %s", game);
			}
			else
			{
				int hostPort;
				while ((hostPort = host.getLocalPort()) == -1)
				{
					if (System.currentTimeMillis()-startTime > env.hostingTimeout*1000)
						throw new StarcraftException("timeout getting host port");
					Thread.sleep(200);
				}
				
				String hostIP = host.getIP();
				pb.environment().put("BWAPI_CONFIG_AUTO_MENU__MAP",				"");
				pb.environment().put("DIRECT_IP_HOST_IP",						hostIP);
				pb.environment().put("DIRECT_IP_HOST_PORT",						""+hostPort);
				pb.environment().put("DIRECT_IP_LOCAL_PORT",					"30000");
				
				log("starting starcraft, joining %s %s:%d", game, hostIP, hostPort);
			}
			
			// extensions
			pb.command().addAll(client.commonEnv.injectoryArguments);
			pb.command().addAll(bot.injectoryArguments);
			pb.environment().putAll(client.commonEnv.environmentVariables);
			pb.environment().putAll(bot.environmentVariables);
			
			
			
			
			
			// start
			starcraftProcess = pb.start();
			
			while (!localPortFile.exists())
			{
				if (System.currentTimeMillis()-startTime > env.hostingTimeout*1000)
					throw new StarcraftException("timeout getting local port");
				if (starcraftProcess.waitFor(200, TimeUnit.MILLISECONDS))
					throw new StarcraftException("starcraft died before getting local port");
			}
			localPort = readLocalPortFile();

			while (!statusFile.exists())
			{
				if (System.currentTimeMillis()-startTime > env.matchStartingTimeout*1000)
					throw new StarcraftException("timeout starting match");
				if (starcraftProcess.waitFor(200, TimeUnit.MILLISECONDS))
					throw new StarcraftException("starcraft died before match start");
			}
			
			log("match started");
			GameStatusFile status = null;
			while (starcraftProcess.isAlive())
			{
				status = getStatus(statusFile);
				if (status != null)
				{
					if (status instanceof Crash)
						throw new BotCrashException(((Crash)status).bot);
					else if (status instanceof Timeout)
						throw new BotTimeoutException(((Timeout)status).bot);
					else if (status instanceof Done)
						break;
				}
				
				long timeSinceModification = System.currentTimeMillis() - statusFile.lastModified();
				if (timeSinceModification > env.matchAlivePollPeriod*1000)
					throw new StarcraftException("status file '"+statusFile+"' not updated in time");
				starcraftProcess.waitFor((long)(env.matchAlivePollPeriod*1000)-timeSinceModification, TimeUnit.MILLISECONDS);
			}
			// got done status or starcraft died
			
			if (!(status instanceof Done))
				throw new StarcraftException("starcraft died");
			
			// wait for starcraft to close
			if (!starcraftProcess.waitFor((long)(env.starcraftClosingTimeout*1000), TimeUnit.MILLISECONDS))
				throw new StarcraftException("timeout waiting for starcraft to close");
			
			if (!replayFile.exists())
				throw new StarcraftException("got done status, but starcraft died without creating the replay file '"+replayFile+"'");
			
			packedWriteDirectory = new PackedFile(new MyFile(instanceDir, "write/"));
			packedReplay = new PackedFile(replayFile);
			result = (Done) status;
			
			log("match ended correctly");
		}
		catch (java.rmi.ConnectException e)
		{
			exception = new StarcraftException("host died.");
			log("host died.");
		}
		catch (java.rmi.NoSuchObjectException e)
		{
			exception = new StarcraftException("host died");
			log("host died");
		}
		catch (InterruptedException e)
		{
			if (interruptReason==null)
				interruptReason = "reason==null";
			
			if (!interruptReason.isEmpty())
				log("interrupted: "+interruptReason);
			
			exception = new StarcraftException("interrupted: "+interruptReason);
		}
		catch (StarcraftException e)
		{
			exception = e;
			log(e.getMessage());
		}
		catch (IOException e)
		{
			exception = new StarcraftException(e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onExit()
	{
		if (starcraftProcess != null)
			starcraftProcess.destroyForcibly();
		
		if (env.deleteInstanceDirs)
			FileUtils.deleteQuietly(instanceDir);
		client.onStarcraftExit(this);
	}
	
	@Override
	public void kill(String reason)
	{
		if (thread() != null && thread().isAlive())
		{
			tryUnexport(true);
			interruptReason = reason;
			thread().interrupt();
		}
	}

	@Override
	public CompleteGameResults getResults() throws StarcraftException
	{
		if (exception != null)
		{
			tryUnexport(true); //unexport now that we've thrown the exception
			throw exception;
		}
		
		if (thread() == null || thread().isAlive())
			return null; //not done yet
		else
		{
			tryUnexport(true);
			return new CompleteGameResults(result, packedReplay, packedWriteDirectory);
		}
	}
	
	@Override
	public String getIP() throws StarcraftException
	{
		try
		{
			return getClientHost();
		}
		catch (ServerNotActiveException e)
		{
			throw new StarcraftException(e);
		}
	}
	
	@Override
	public int getLocalPort() throws StarcraftException
	{
		if (isHost())
			return localPort;
		else
			throw new StarcraftNotHostException();
	}
	
	private boolean isHost()
	{
		return host == null;
	}
	
	private int readLocalPortFile() throws StarcraftException, IOException
	{
		if (!localPortFile.exists())
			throw new StarcraftException("'"+localPortFile.getAbsolutePath()+"' doesn't exist (yet)");
		
		try
		{
			String portString = FileUtils.readFileToString(localPortFile);
			
			int localPort = Integer.parseInt(portString);
			if (localPort == -1)
				throw new StarcraftException("error opening a local port");
			else
				return localPort;
		}
		catch (NumberFormatException e)
		{
			throw new StarcraftException("unexpected text in '"+localPortFile.getAbsolutePath()+"': "+e.getMessage(), e);
		}
	}
	
	private GameStatusFile getStatus(File statusFile) throws IOException, StarcraftException
	{
		String statusString = FileUtils.readFileToString(statusFile);
		return (GameStatusFile) new Yaml(new MyConstructor(env)).load(statusString);
	}
	
	private void log(String format, Object... args)
	{
		double sinceMatchStart = (System.currentTimeMillis() - startTime)/1000.0;
		String prefix = String.format("g%sp%d [%02d:%02d] ", game.id, playerIndex, (int)(sinceMatchStart/60), (int)(sinceMatchStart%60));
		Client.log(prefix+String.format(format, args));
	}
	
	@Override
	public String toString()
	{
		return String.format("{Starcraft %s}", Helper.getEndpointAddress(this));
	}
}
