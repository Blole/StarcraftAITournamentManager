package server;

import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.CompleteGameResults;
import common.Game;
import common.GameResults;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyBusyException;
import common.exceptions.DifferingCommonResultsException;
import common.exceptions.StarcraftException;
import common.file.MyFile;
import common.file.PackedFile;
import common.protocols.RemoteStarcraft;
import common.yaml.MyConstructor;
import common.yaml.MyRepresenter;
import server.exceptions.NotEnoughStarcraftInstancesCouldBeStartedException;

/**
 * This class "takes ownership" of the yamlFile, and will move it
 * out of the queued directory when the game is finished.
 */
public class ServerGame implements Runnable
{
	public static enum ServerGameState {QUEUED, RUNNING, DONE, ERROR};
	private final Server server;
	public final Game game;
	public final MyFile file;
	
	private ServerGameState state;
	private Thread thread = null;
	private String interruptReason = null;
	
	public ServerGame(Game game, MyFile file, Server server)
	{
		this.server = server;
		this.state = ServerGameState.QUEUED;
		this.file = file;
		this.game = game;
	}
	
	@Override
	public void run()
	{
		thread = Thread.currentThread();
		thread.setName(toString());
		String errorText = null;
		
		//start starcrafts
		ArrayList<RemoteStarcraft> starcrafts = new ArrayList<>();
		RemoteStarcraft host = null;
		for (ProxyClient client : server.clientManager.clients())
		{
			try
			{
				while (starcrafts.size() < game.bots.length)
				{
					if (host == null)
					{
						host = client.hostMatch(game, starcrafts.size());
						starcrafts.add(host);
					}
					else
						starcrafts.add(client.joinMatch(game, starcrafts.size(), host));
				}
			}
			catch (RemoteException e)
			{
			}
			catch (AllStarcraftInstanceSlotsAlreadyBusyException e)
			{
			}
		}
		
		try
		{
			if (starcrafts.size() < game.bots.length)
				throw new NotEnoughStarcraftInstancesCouldBeStartedException(this, starcrafts.size());
			
			state = ServerGameState.RUNNING;
			log("all starcraft instances started");
			//server.gui.UpdateRunningStats(player, game, i, startTime);
		
			CompleteGameResults[] completeResults = new CompleteGameResults[starcrafts.size()];
			while (true)
			{
				boolean allDone = true;
				for (int i=0; i<starcrafts.size(); i++)
				{
					if (completeResults[i] == null)
						completeResults[i] = starcrafts.get(i).getResults();
					if (completeResults[i] == null)
						allDone = false;
				}
				if (allDone)
					break;
				Thread.sleep((long) (server.env.matchFinishedPollPeriod*1000));
			}
			log("results collected");
			
			PackedFile[] writeDirs = new PackedFile[game.bots.length];
			GameResults results = new GameResults();
			PackedFile replay = completeResults[0].replay;
			for (int i=0; i<game.bots.length; i++)
			{
				writeDirs[i] = completeResults[i].writeDirectory;
				results.add(i, completeResults[i].result);
			}
			
			Yaml yaml = new Yaml(new MyRepresenter());
			String resultsText = yaml.dump(results);
			FileUtils.forceMkdir(resultsDir());
			FileUtils.writeStringToFile(resultsFile(), resultsText);
			
			replay.writeTo(resultsDir());
			for (int i=0; i<writeDirs.length; i++)
				writeDirs[i].writeTo(game.bots[i].getWriteDir(server.env));
			
			server.gameQueueManager.quietlyDequeue(file);
			FileUtils.moveFileToDirectory(file, resultsDir(), false);
			
			state = ServerGameState.DONE;
			server.gameQueueManager.checkAndNotify();
		}
		catch (NotEnoughStarcraftInstancesCouldBeStartedException e)
		{
			errorText = String.format("only %d/%d starcraft instances could be started", e.started, game.bots.length);
		}
		catch (InterruptedException e)
		{
			if (interruptReason==null)
				interruptReason = "reason==null";
			errorText = interruptReason.isEmpty() ? "" : "match interrupted: "+interruptReason;
		}
		catch (ConnectException e)
		{
			errorText = "remote starcraft died";
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			errorText = "remote starcraft died: " + e.getMessage();
		}
		catch (StarcraftException e)
		{
			errorText = "remote starcraft exception: " + e.getMessage();
		}
		catch (DifferingCommonResultsException e)
		{
			errorText = "collected common results differ";
		}
		catch (IOException e)
		{
			e.printStackTrace();
			FileUtils.deleteQuietly(resultsDir());
			errorText = "error writing results dir: " + e.getMessage();
		}
		finally
		{
			if (errorText != null)
			{
				state = ServerGameState.ERROR;
				//state = ServerGameState.QUEUED; //TODO: requeue?
				if (!errorText.isEmpty())
				{
					log(errorText);
					tryKillAllStarcrafts(starcrafts, "server exception: "+errorText);
				}
				else
					tryKillAllStarcrafts(starcrafts, "");
			}
			server.onMatchDone(ServerGame.this);
		}
	}
	
	/**
	 * @return true if the game was running and stopped
	 */
	public boolean stop(String reason)
	{
		if (thread != null && thread.isAlive())
		{
			interruptReason = reason;
			thread.interrupt();
			return true;
		}
		else
			return false;
	}
	
	
	public static MyFile resultsDir(Server server, MyFile file)
	{
		return new MyFile(server.env.gameResultsDir, file.getNameWithoutExtension());
	}
	private MyFile resultsDir()
	{
		return resultsDir(server, file);
	}
	private MyFile resultsFile()
	{
		return new MyFile(resultsDir(), "results.yaml");
	}
	public ServerGameState state()
	{
		return state;
	}
	@Override
	public String toString()
	{
		return game.toString().replaceFirst("Game", "ServerGame");
	}
	
	
	

	private void log(String format, Object... args)
	{
		server.log(this+": "+String.format(format, args));
	}
	
	private static void tryKillAllStarcrafts(Iterable<RemoteStarcraft> starcrafts, String reason)
	{
		for (RemoteStarcraft starcraft : starcrafts)
		{
			try
			{
				starcraft.kill(reason);
			}
			catch (RemoteException e)
			{
			}
		}
	}
	
	public static ServerGame load(Server server, File file_) throws IOException
	{
		MyFile file = new MyFile(file_);
		String gameText = FileUtils.readFileToString(file);
		Yaml yaml = new Yaml(new MyConstructor(server.env));
		Game game = yaml.loadAs(gameText, Game.class);
		game.id = file.getNameWithoutExtension();
		return new ServerGame(game, file, server);
	}
}
