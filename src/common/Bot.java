package common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import server.ServerEnvironment;

public class Bot implements Serializable
{
	private static final long serialVersionUID = 2690734629985126222L;
	
	public final String name;
	public final Race race;
	public final BotExecutableType type;
	public final ArrayList<TargetFile> requiredFiles = new ArrayList<>();

	public Bot(String name, Race race, BotExecutableType type, TargetFile... requiredFiles)
	{
		this.name = name;
		this.race = race;
		this.type = type;
		
		this.requiredFiles.addAll(Arrays.asList(requiredFiles));
	}
	
	public boolean isProxyBot()
	{
		return type == BotExecutableType.proxy;
	}
	
	@Override
	public String toString()
	{
		return String.format("{Bot:%s}", name);
	}
	
	public File getDir(ServerEnvironment env)
	{
		return env.lookupFile("$bot_dir/"+name);
	}

	public File getReadDir(ServerEnvironment env)
	{
		return new File(getDir(env), "read");
	}

	public File getWriteDir(ServerEnvironment env)
	{
		return new File(getDir(env), "write");
	}
}
