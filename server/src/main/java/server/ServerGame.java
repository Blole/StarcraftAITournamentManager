package server;

import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Game;
import common.GameResults;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyBusyException;
import common.exceptions.InvalidResultsException;
import common.exceptions.StarcraftException;
import common.file.MyFile;
import common.file.PackedFile;
import common.protocols.RemoteStarcraft;
import common.status.Done;
import common.yaml.MyConstructor;
import common.yaml.MyRepresenter;
import server.exceptions.NotEnoughStarcraftInstancesCouldBeStartedException;
import server.exceptions.ServerGameResultsDirAlreadyExistsException;

/**
 * This class "takes ownership" of the yamlFile, and will move it
 * out of the queued directory when the game is finished.
 */
public class ServerGame
{
	public static enum ServerGameState {QUEUED, RUNNING, DONE, ERROR};
	private final Server server;
	public final Game game;
	public final MyFile file;
	
	private ServerGameState state;
	private RunningGame runningGame = null;
	
	public ServerGame(Game game, MyFile file, Server server) throws ServerGameResultsDirAlreadyExistsException
	{
		this.server = server;
		this.state = ServerGameState.QUEUED;
		this.file = file;
		this.game = game;
		
		if (resultsDir().exists())
			throw new ServerGameResultsDirAlreadyExistsException(file, resultsDir());
	}
	
	public void start() throws NotEnoughStarcraftInstancesCouldBeStartedException
	{
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
		
		if (starcrafts.size() < game.bots.length)
			throw new NotEnoughStarcraftInstancesCouldBeStartedException(this, starcrafts.size());
		
		runningGame = new RunningGame(starcrafts);
		runningGame.start();
		state = ServerGameState.RUNNING;
		
		//server.gui.UpdateRunningStats(player, game, i, startTime);
	}
	
	/**
	 * @return true if the game was running and stopped
	 */
	public boolean stop()
	{
		if (runningGame == null)
			return false;
		else
		{
			runningGame.interrupt();
			runningGame = null;
			return true;
		}
	}
	
	private void onDone(GameResults results, PackedFile replay, PackedFile[] writeDirs) throws IOException
	{
		try
		{
			Yaml yaml = new Yaml(new MyRepresenter());
			String resultsText = yaml.dump(results);
			FileUtils.forceMkdir(resultsDir());
			FileUtils.writeStringToFile(resultsFile(), resultsText);
			
			replay.writeTo(resultsDir());
			for (int i=0; i<writeDirs.length; i++)
				writeDirs[i].writeTo(game.bots[i].getWriteDir(server.env));
			
			FileUtils.moveFileToDirectory(file, resultsDir(), false);
		}
		catch (IOException e)
		{
			FileUtils.deleteQuietly(resultsDir());
			throw e;
		}
		state = ServerGameState.DONE;
		server.gameQueueManager.checkAndNotify();
	}
	
	public void onException(Exception e)
	{
		if (!(
				e instanceof InterruptedException ||
				e instanceof ConnectException))
			e.printStackTrace();
		state = ServerGameState.ERROR;
		//state = ServerGameState.QUEUED; //TODO: requeue?
	}
	
	
	
	private class RunningGame extends Thread
	{
		private final ArrayList<RemoteStarcraft> starcrafts;
		
		public RunningGame(ArrayList<RemoteStarcraft> starcrafts)
		{
			this.starcrafts = starcrafts;
		}

		@Override
		public void run()
		{
			Exception exception = null;
			server.log("%s starting", this);
			try
			{
				while (true)
				{
					boolean allDone = true;
					for (RemoteStarcraft starcraft : starcrafts)
						allDone &= starcraft.isFinished();
					if (allDone)
						break;
					Thread.sleep((long) (server.env.matchFinishedPollPeriod*1000));
				}
				
				server.log("%s finished, collecting replays", game);
				
				PackedFile[] writeDirs = new PackedFile[game.bots.length];
				GameResults results = new GameResults();
				PackedFile replay = null;
				for (int i=0; i<game.bots.length; i++)
				{
					RemoteStarcraft starcraft = starcrafts.get(i);
					writeDirs[i] = starcraft.getWriteDirectory();
					
					replay = starcraft.getReplay();
					Done done = starcraft.getResult();
					results.add(i, done);
				}
				onDone(results, replay, writeDirs);
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
				exception = e;
			}
			catch (StarcraftException e)
			{
				server.log("%s: remote starcraft exception: %s", this, e.getMessage());
				exception = e;
			}
			catch (InvalidResultsException e)
			{
				server.log("%s: invalid results: %s", this, e.getMessage());
				exception = e;
			}
			catch (IOException e)
			{
				server.log("%s: error writing results dir: %s", this, e.getMessage());
				exception = e;
			}
			finally
			{
				if (exception != null)
				{
					onException(exception);
					tryKillAllStarcrafts("killed by server");
				}
				server.onMatchDone(ServerGame.this);
			}
		}
		
		private void tryKillAllStarcrafts(String reason)
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
		
		@Override
		public String toString()
		{
			return ServerGame.this.toString();
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
		return game.toString().replace("Game", "ServerGame");
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
