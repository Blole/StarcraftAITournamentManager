package common.status;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.Bot;
import common.exceptions.BotCrashException;

public class Crash extends GameStatusFile implements Serializable
{
	private static final long serialVersionUID = -3491080203970435547L;
	public static final TypeDescription typeDescription = new TypeDescription(Crash.class, "!crash");
	
	
	
	public Crash(Bot bot) throws BotCrashException
	{
		throw new BotCrashException(bot);
	}
}
