package common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

import server.ServerEnvironment;

import common.yaml.GameConstructor;

public class GameStorage
{
	public List<Game> games = null;
	
	@SuppressWarnings("unchecked")
	public GameStorage(ServerEnvironment env, String yamlData)
	{
		this(new Yaml(new GameConstructor(env.lookupFile("$botDir"))).loadAs(yamlData, List.class));
	}
	
	public GameStorage(List<Game> games)
	{
		this.games = games;
	}

	public Set<Bot> getAllBots()
	{
		return games.stream()
				.flatMap(g -> Arrays.asList(g.bots).stream())
				.collect(Collectors.toSet());
	}
}
