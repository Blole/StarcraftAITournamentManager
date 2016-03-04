package server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.yaml.snakeyaml.error.YAMLException;

import common.Bot;
import common.file.MyFile;
import server.ServerGame.ServerGameState;

public class GameQueueManager extends FileAlterationListenerAdaptor
{
	private final HashMap<File, ServerGame> games = new HashMap<>();
	private final Server server;
	private final FileAlterationObserver obs;

	public GameQueueManager(Server server)
	{
		this.server = server;
		this.obs = new FileAlterationObserver(server.env.gameQueueDir, FileFilterUtils.suffixFileFilter(".yaml",IOCase.INSENSITIVE));
		this.obs.addListener(this);
	}
	
	
	public Set<Bot> getAllBots()
	{
		obs.checkAndNotify();
		return games.values().stream()
				.flatMap(g -> Stream.of(g.game.bots))
				.collect(Collectors.toSet());
	}
	
	
	
	private void read(File file, String action)
	{
		File resultsDir = ServerGame.resultsDir(server, new MyFile(file));
		if (resultsDir.exists())
		{
			server.log("'%s' not %sd, results dir '%s' already exists", file, action, resultsDir);
			return;
		}
		
		try
		{
			ServerGame game = ServerGame.load(server, file);
			games.put(file, game);
			server.log("%s %sd", game, action);
		}
		catch (IOException|YAMLException e)
		{
			server.log("'%s' not %sd, error parsing file", file, action);
			e.printStackTrace();
		}
	}
	
	@Override
	public void onFileCreate(File file)
	{
		read(file, "queue");
	}
	
	@Override
	public void onFileChange(File file)
	{
		ServerGame game = games.get(file);
		if (game != null && game.stop())
			server.log(game+" stopped");
		
		read(file, "update");
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
			if (game.stop())
				server.log(game+" stopped");
			else
				server.log(game+" unqueued");
		}
	}
	
	
	
	private Stream<ServerGame> gamesInState(ServerGameState state)
	{
		obs.checkAndNotify();
		return games.values().stream().filter(g-> g.state() == state).sorted(
				(ServerGame a, ServerGame b)->
				{
					if (a.game.round != b.game.round)
						return a.game.round-b.game.round;
					else
						return a.game.id.compareTo(b.game.id);
				});
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
		return queued().count() == 0 && running().count() == 0;
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
