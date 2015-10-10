package server;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.Game;
import common.GameResults;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyUsedException;
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
		for (ProxyClient client : server.clientManager.clients())
		{
			try
			{
				while (starcrafts.size() < game.bots.length)
					starcrafts.add(client.startMatch(game, starcrafts.size()));
			}
			catch (RemoteException e)
			{
				server.clientManager.disconnected(client);
			}
			catch (AllStarcraftInstanceSlotsAlreadyUsedException e)
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
	public boolean stop() throws RemoteException
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
		server.gameManager.checkAndNotify();
	}
	
	public void onException(Exception e)
	{
		if (!(e instanceof InterruptedException))
			state = ServerGameState.ERROR;
		//state = ServerGameState.QUEUED; //TODO: requeue?
		e.printStackTrace();
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
			server.log(game + " starting");
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
			catch (InterruptedException|StarcraftException|IOException|InvalidResultsException e)
			{
				onException(e);
				for (RemoteStarcraft starcraft : starcrafts)
				{
					try
					{
						starcraft.kill();
					}
					catch (RemoteException e2)
					{
						server.log("%s: error killing remote StarCraft", ServerGame.this);
						e2.printStackTrace();
					}
				}
			}
			finally
			{
				server.notifyAll();
			}
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
		return game.toString().replace("Game", "ServerGame").replace("}", " "+state().name()+"}");
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
