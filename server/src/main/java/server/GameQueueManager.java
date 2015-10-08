package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import common.Bot;
import common.Game;
import common.file.MyFile;
import common.yaml.MyConstructor;
import server.ServerGame.ServerGameState;

public class GameQueueManager extends FileAlterationListenerAdaptor
{
	private HashMap<File, ServerGame> games = new HashMap<>();
	public final Server server;
	private final ServerEnvironment env;
	private final FileAlterationObserver obs;
	private Yaml yaml;

	public GameQueueManager(Server server)
	{
		this.server = server;
		this.env = server.env;
		this.obs = new FileAlterationObserver(env.gameQueueDir, new SuffixFileFilter(".yaml",IOCase.INSENSITIVE));
		this.yaml = new Yaml(new MyConstructor(server.env));
		obs.addListener(this);
	}
	
	
	public Set<Bot> getAllBots()
	{
		obs.checkAndNotify();
		return games.values().stream()
				.flatMap(g -> Stream.of(g.game.bots))
				.collect(Collectors.toSet());
	}
	
	
	
	/**
	 * @return the read game or null if error
	 */
	private ServerGame read(File file)
	{
		try
		{
			String gameText = FileUtils.readFileToString(file);
			Game game = yaml.loadAs(gameText, Game.class);
			return new ServerGame(game, new MyFile(file), this);
		}
		catch (FileAlreadyExistsException e)
		{
			server.log("error parsing queued game file '%s', the corresponding results dir already exists '%s'", file, e.getFile());
		}
		catch (IOException|YAMLException e)
		{
			server.log("error parsing queued game file '%s'", file);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return true if the game was running and stopped
	 */
	private boolean tryStop(ServerGame game)
	{
		try
		{
			return game.stop();
		}
		catch (RemoteException e)
		{
			server.log(game+" error stopping");
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public void onFileCreate(File file)
	{
		ServerGame game = read(file);
		if (game != null)
		{
			games.put(file, game);
			server.log(game+" queued");
		}
	}
	
	@Override
	public void onFileChange(File file)
	{
		ServerGame game = games.get(file);
		if (game != null && tryStop(game))
			server.log(game+" stopped");
		
		game = read(file);
		if (game != null)
		{
			games.put(file, game);
			server.log(game+" updated");
		}
	}
	
	/**
	 * When a queued game file is removed from the directory,
	 * it is no longer in the queue, and therefore not managed by this class.
	 */
	@Override
	public void onFileDelete(File file)
	{
		ServerGame game = games.remove(file);
		if (game != null)
		{
			if (tryStop(game))
				server.log(game+" stopped");
			else
				server.log(game+" unqueued");
		}
	}
	
	
	
	private Stream<ServerGame> gamesInState(ServerGameState state)
	{
		obs.checkAndNotify();
		return games.values().stream().filter(g-> g.state() == state);
	}
	public Stream<ServerGame> queued()
	{
		return gamesInState(ServerGameState.QUEUED);
	}
	public Stream<ServerGame> running()
	{
		return gamesInState(ServerGameState.RUNNING);
	}
	public ServerGame getNextUnstartedGame()
	{
		return queued().findFirst().orElse(null);
	}
	public boolean allDone()
	{
		return queued().count() == 0;
	}
	
	
	public void checkAndNotify()
	{
		obs.checkAndNotify();
	}
	
	@Override
	public String toString()
	{
		return String.format("[GameQueueManager %d queued, %d running]",
				queued().count(), running().count());
	}
}
