package common.results;

import java.io.Serializable;

import common.Bot;

public class Timeout extends GameEndReason implements Serializable
{
	private static final long serialVersionUID = -1401865040389464261L;
	
	
	
	public final Bot bot;

	public Timeout(Bot bot)
	{
		this.bot = bot;
	}
}
