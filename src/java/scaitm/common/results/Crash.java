package common.results;

import java.io.Serializable;

import common.Bot;

public class Crash extends GameEndReason implements Serializable
{
	private static final long serialVersionUID = -3491080203970435547L;
	
	
	
	public final Bot bot;

	public Crash(Bot bot)
	{
		this.bot = bot;
	}
}
