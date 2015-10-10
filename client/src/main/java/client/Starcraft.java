package client;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Bot;
import common.BotExecutableType;
import common.Game;
import common.RunnableUnicastRemoteObject;
import common.exceptions.StarcraftException;
import common.exceptions.StarcraftMatchNotFinishedException;
import common.file.CopyFile;
import common.file.MyFile;
import common.file.PackedFile;
import common.file.RequiredFile;
import common.protocols.RemoteStarcraft;
import common.status.Done;
import common.status.GameStatusFile;
import common.utils.Helper;
import common.utils.WindowsCommandTools;
import common.yaml.MyConstructor;

public class Starcraft extends RunnableUnicastRemoteObject implements RemoteStarcraft
{
	private static final long serialVersionUID = 5069349040098758632L;
	
	final private Client client;
	final private ClientEnvironment env;
	final private Game game;
	final private int index;
	final private Bot bot;
	
	private StarcraftException exception = null;
	private long startTime;
	
	private Done result = null;
	private PackedFile packedReplay = null;
	private PackedFile packedWriteDirectory = null;
	
	private MyFile instanceDir;
	private Process starcraftProcess = null;

	public Starcraft(Client client, Game game, int index) throws RemoteException
	{
		this.client = client;
		this.env = client.env;
		this.game = game;
		this.index = index;
		this.bot = game.bots[index];
		instanceDir = new MyFile("game"+game.id+"_player"+index);
	}
	
	@Override
	protected void onRun() throws IOException
	{
		startTime = System.currentTimeMillis();
		try
		{
			File starcraftDir = env.starcraftDir;
			File configDir = new MyFile(starcraftDir, "bwapi-data");
			
			FileUtils.forceMkdir(instanceDir);
			FileUtils.cleanDirectory(instanceDir);
			FileUtils.forceDeleteOnExit(instanceDir);
			
			MyFile replayFile = new MyFile(instanceDir, "replay.rep");
			MyFile statusFile = new MyFile(instanceDir, env.gamestatusFileName);
			MyFile tournamentYaml = env.tournamentModuleYaml();
			
			// If this is a proxy bot, start the proxy bot script before StarCraft starts
			if (bot.type == BotExecutableType.proxy)
				WindowsCommandTools.RunWindowsCommand(new RequiredFile(env.dataDir, "run_proxy.bat").getAbsolutePath(), false, false);
			
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("data/injectory.exe", "--launch", "starcraft_multiinstance.exe", "--inject", bot.bwapiVersion.getDll(env).getAbsolutePath(), "--kill-on-exit", "--wait-for-exit");
			pb.directory(starcraftDir);
			pb.environment().put("BWAPI_CONFIG_INI", 	    				env.iniFile().require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__AI",     				bot.getDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__AI_DBG", 				bot.getDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AI__TOURNAMENT",				bot.bwapiVersion.getTournamentDll(env).require().getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__CHARACTER_NAME",	bot.displayName());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__AUTO_RESTART",	"EXIT");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__MAP",				index==0?game.map.getFile(env).require().getAbsolutePath():"");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__GAME",			game.id+"");
			//TODO: pb.environment().put("BWAPI_CONFIG_AUTO_MENU__GAME_TYPE", game.type);
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__RACE",			bot.race.toString());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__SAVE_REPLAY",		replayFile.getAbsolutePath());
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__WAIT_FOR_MIN_PLAYERS", game.bots.length+"");
			pb.environment().put("BWAPI_CONFIG_AUTO_MENU__WAIT_FOR_MAX_PLAYERS", game.bots.length+"");
			//TODO tournament environment config
			pb.environment().put("SCAITM_TOURNAMENT_CONFIG",				tournamentYaml.require().getAbsolutePath());
			pb.environment().put("SCAITM_TOURNAMENT_STATUS_FILE",			statusFile.getAbsolutePath());
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			//log(String.join(" ", pb.command()));
			
			if (bot.extraFiles != null)
			{
				for (CopyFile file : bot.extraFiles)
					file.copyDiffering(env.lookupFile(file.extractTo));
			}
			
			
			log("starting starcraft "+game);
			starcraftProcess = pb.start();
			while (!statusFile.exists())
			{
				if (!starcraftProcess.isAlive())
					throw new StarcraftException("starcraft died while starting match");
				if (System.currentTimeMillis()-startTime > env.matchStartingTimeout*1000)
					throw new StarcraftException("timeout starting match");
				
				Thread.sleep(200);
			}
			
			log("match started");
			while (starcraftProcess.isAlive())
			{
				GameStatusFile status = getStatus(statusFile);
				if (status != null && status instanceof Done)
				{
					log("waiting for starcraft to close");
					if (!starcraftProcess.waitFor((long)(env.starcraftClosingTimeout*1000), TimeUnit.MILLISECONDS))
						throw new StarcraftException("timeout waiting for starcraft to close");
					break;
				}
				
				long timeSinceModification = System.currentTimeMillis() - statusFile.lastModified();
				if (timeSinceModification > env.matchAlivePollPeriod*1000)
					throw new StarcraftException("status file '"+statusFile+"' not updated in time");
				starcraftProcess.waitFor((long)(env.matchAlivePollPeriod*1000)-timeSinceModification, TimeUnit.MILLISECONDS);
				
				
				Thread.sleep((long)(env.matchAlivePollPeriod*1000 - timeSinceModification));
			}
			log("starcraft closed");
			
			GameStatusFile gameStatusFile = getStatus(statusFile);
			
			if (!(gameStatusFile instanceof Done))
				throw new StarcraftException(statusFile+" does not contain a !done-object");
			if (!replayFile.exists())
				throw new StarcraftException("starcraft died without creating the replay file '"+replayFile+"'");
			
			packedWriteDirectory = new PackedFile(new MyFile(instanceDir, "write/"));
			packedReplay = new PackedFile(replayFile);
			result = (Done) gameStatusFile;
			
			log("match ended correctly");
		}
		catch (InterruptedException e)
		{
			exception = new StarcraftException("match interrupted");
			log("match interrupted");
		}
		catch (StarcraftException e)
		{
			exception = e;
			log(e.getMessage());
		}
		catch (IOException e)
		{
			exception = new StarcraftException(e);
			e.printStackTrace();
		}
		finally
		{
			starcraftProcess.destroyForcibly();
		}
	}
	
	@Override
	protected void onExit()
	{
		FileUtils.deleteQuietly(instanceDir);
		client.onMatchDone(this);
	}
	
	@Override
	public void kill()
	{
		if (thread == null || !thread.isAlive())
			return;
		
		try
		{
			thread.interrupt();
			thread.join();
			Client.log("killed (by remote?)");
		}
		catch (InterruptedException e)
		{
			Client.log("interrupted while stopping starcraft game thread");
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFinished() throws StarcraftException
	{
		if (exception != null)
			throw exception;
		else if (thread == null)
			return false;
		else
			return !thread.isAlive();
	}
	
	private void checkFinished() throws StarcraftException
	{
		if (!isFinished())
			throw new StarcraftMatchNotFinishedException();
	}
	
	@Override
	public Done getResult() throws StarcraftException
	{
		checkFinished();
		return result;
	}
	
	@Override
	public PackedFile getReplay() throws StarcraftException
	{
		checkFinished();
		return packedReplay;
	}

	@Override
	public PackedFile getWriteDirectory() throws StarcraftException
	{
		checkFinished();
		return packedWriteDirectory;
	}
	
	
	
	private GameStatusFile getStatus(File statusFile) throws IOException, StarcraftException
	{
		String statusString = FileUtils.readFileToString(statusFile);
		return new Yaml(new MyConstructor(env)).loadAs(statusString, GameStatusFile.class);
	}
	
	private void log(String format, Object... args)
	{
		double sinceMatchStart = (System.currentTimeMillis() - startTime)/1000.0;
		String matchTimeStamp = String.format("[%02d:%02d] ", (int)(sinceMatchStart/60), (int)(sinceMatchStart%60));
		Client.log(matchTimeStamp+format, args);
	}
	
	@Override
	public String toString()
	{
		return String.format("{Starcraft %s}", Helper.getEndpointAddress(this));
	}
}
