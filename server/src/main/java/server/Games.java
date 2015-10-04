package server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.yaml.snakeyaml.Yaml;

import common.Bot;
import common.Game;
import server.Server.RunningMatch;
import server.ServerGame.ServerGameState;

public class Games extends FileAlterationListenerAdaptor
{
	private Collection<ServerGame> games = null;
	public final ArrayList<RunningMatch> runningMatches = new ArrayList<>();
	private FileAlterationObserver obs;
	private ServerEnvironment env;
	private Yaml yaml;

	public Games(ServerEnvironment env)
	{
		this.env = env;
		this.obs = new FileAlterationObserver(env.gameQueueDir);
		this.yaml = new Yaml(new ServerConstructor(env));
		obs.addListener(this);
	}
	
	
	public ServerGame getNextUnstartedGame()
	{
		return queued().findFirst().orElse(null);
	}
	
	public List<Game> getRunningGames()
	{
		obs.checkAndNotify();
		return runningMatches.stream().map(g -> g.game).collect(Collectors.toList());
	}
	
	public Set<Bot> getAllBots()
	{
		obs.checkAndNotify();
		return games.stream()
				.flatMap(g -> Stream.of(g.bots))
				.collect(Collectors.toSet());
	}


	public boolean allDone()
	{
		return queued().count() == 0;
	}
	
	public Stream<ServerGame> queued()
	{
		obs.checkAndNotify();
		return games.stream().filter(g-> g.state() == ServerGameState.DONE);
	}


	@Override
	public void onStart(FileAlterationObserver observer)
	{
		System.out.println("obs start");
	}


	@Override
	public void onFileCreate(File file)
	{
		System.out.println(file+" created");
	}


	@Override
	public void onFileChange(File file)
	{
		System.out.println(file+" changed");
	}


	@Override
	public void onFileDelete(File file)
	{
		System.out.println(file+" deleted");
	}


	@Override
	public void onStop(FileAlterationObserver observer)
	{
		System.out.println("obs stop");
	}


	public void checkAndNotify()
	{
		obs.checkAndNotify();
	}
}
