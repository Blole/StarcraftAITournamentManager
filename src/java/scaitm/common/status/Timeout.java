package common.status;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.Bot;
import common.exceptions.BotTimeoutException;

public class Timeout extends GameStatusFile implements Serializable
{
	private static final long serialVersionUID = -1401865040389464261L;
	public static final TypeDescription typeDescription = new TypeDescription(Timeout.class, "!timeout");
	
	
	
	public Timeout(Bot bot) throws BotTimeoutException
	{
		throw new BotTimeoutException(bot);
	}

	@Override
	public GameStatus getStatus()
	{
		return null;
	}
}
