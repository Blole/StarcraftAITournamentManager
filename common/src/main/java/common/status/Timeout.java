package common.status;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.Bot;

public class Timeout extends GameStatusFile implements Serializable
{
	private static final long serialVersionUID = -1401865040389464261L;
	public static final TypeDescription typeDescription = new TypeDescription(Timeout.class, "!timeout");
	
	
	
	public final Bot bot;
	
	public Timeout(Bot bot)
	{
		this.bot = bot;
	}
}
