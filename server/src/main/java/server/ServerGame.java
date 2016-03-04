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
import server.exceptions.ServerGameResultsDirAlreadyExistsException;

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
	private Exception exception = null;
	
	public ServerGame(Game game, MyFile file, Server server) throws ServerGameResultsDirAlreadyExistsException
	{
		this.server = server;
		this.state = ServerGameState.QUEUED;
		this.file = file;
		this.game = game;
		
		if (resultsDir().exists())
			throw new ServerGameResultsDirAlreadyExistsException(file, resultsDir());
	}
	
	@Override
	public void run()
	{
		thread = Thread.currentThread();
		
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
			server.log("%s all starcraft instances started", this);
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
			server.log(game+" results collected");
			
			PackedFile[] writeDirs = new PackedFile[game.bots.length];
			GameResults results = new GameResults();
			PackedFile replay = completeResults[0].replay;
			for (int i=0; i<game.bots.length; i++)
			{
				writeDirs[i] = completeResults[i].writeDirectory;
				results.add(i,  completeResults[i].result);
			}
			
			Yaml yaml = new Yaml(new MyRepresenter());
			String resultsText = yaml.dump(results);
			FileUtils.forceMkdir(resultsDir());
			FileUtils.writeStringToFile(resultsFile(), resultsText);
			
			replay.writeTo(resultsDir());
			for (int i=0; i<writeDirs.length; i++)
				writeDirs[i].writeTo(game.bots[i].getWriteDir(server.env));
			
			FileUtils.moveFileToDirectory(file, resultsDir(), false);
			
			state = ServerGameState.DONE;
			server.gameQueueManager.checkAndNotify();
		}
		catch (NotEnoughStarcraftInstancesCouldBeStartedException e)
		{
			server.log("%s: error starting, only %d/%d starcraft instances could be started", this, e.started, game.bots.length);
			exception = e;
		}
		catch (InterruptedException e)
		{
			server.log("%s: interrupted", this);
			exception = e;
		}
		catch (ConnectException e)
		{
			server.log("%s: remote starcraft died", this);
			exception = e;
		}
		catch (RemoteException e)
		{
			server.log("%s: remote starcraft died: %s", this, e.getMessage());
			e.printStackTrace();
			exception = e;
		}
		catch (StarcraftException e)
		{
			server.log("%s: remote starcraft exception: %s", this, e.getMessage());
			e.printStackTrace();
			exception = e;
		}
		catch (DifferingCommonResultsException e)
		{
			server.log("%s: collected common results differ", this);
			exception = e;
		}
		catch (IOException e)
		{
			FileUtils.deleteQuietly(resultsDir());
			server.log("%s: error writing results dir: %s", this, e.getMessage());
			e.printStackTrace();
			exception = e;
		}
		finally
		{
			if (exception != null)
			{
				state = ServerGameState.ERROR;
				//state = ServerGameState.QUEUED; //TODO: requeue?
				tryKillAllStarcrafts(starcrafts, "killed by server");
			}
			server.onMatchDone(ServerGame.this);
		}
	}
	
	/**
	 * @return true if the game was running and stopped
	 */
	public boolean stop()
	{
		if (thread == null)
			return false;
		else
		{
			thread.interrupt();
			thread = null;
			return true;
		}
	}
	
	
	private MyFile resultsDir()
	{
		return new MyFile(server.env.gameResultsDir, file.getNameWithoutExtension());
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
	
	
	

	private static void tryKillAllStarcrafts(Iterable<RemoteStarcraft> starcrafts, String reason)
	{
		for (RemoteStarcraft starcraft : starcrafts)
		{
			try
			{
				starcraft.kill(reason);
			}
			catch (RemoteException e2)
			{
			}
		}
	}
	
	public static ServerGame load(Server server, File file_) throws ServerGameResultsDirAlreadyExistsException, IOException
	{
		MyFile file = new MyFile(file_);
		String gameText = FileUtils.readFileToString(file);
		Yaml yaml = new Yaml(new MyConstructor(server.env));
		Game game = yaml.loadAs(gameText, Game.class);
		game.id = file.getNameWithoutExtension();
		return new ServerGame(game, file, server);
	}
}
