package common;

import java.io.File;
import java.io.Serializable;

public class Bot implements Serializable
{
	private static final long serialVersionUID = 2690734629985126222L;
	public final String name;
	public final Race race;
	public final BotExecutableType type;
	public final BWAPIVersion bwapiVersion;

	public Bot(String name, Race race, BotExecutableType type, BWAPIVersion bwapiVersion)
	{
		this.name = name;
		this.race = race;
		this.type = type;
		this.bwapiVersion = bwapiVersion;
		
	}
	
	public boolean isProxyBot()
	{
		return type == BotExecutableType.proxy;
	}
	
	@Override
	public String toString()
	{
		return String.format("[Bot: %s, %s, %s, %s]", name, race, type, bwapiVersion);
	}
	
	public File getDir(Environment env)
	{
		return env.lookupFile("$botdir/"+name);
	}

	public File getReadDir(Environment env)
	{
		return new File(getDir(env), "read");
	}

	public File getWriteDir(Environment env)
	{
		return new File(getDir(env), "write");
	}
}
