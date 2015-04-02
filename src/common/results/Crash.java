package common.results;

import common.Bot;

public class Crash extends GameEndReason
{
	public final Bot bot;

	public Crash(Bot bot)
	{
		this.bot = bot;
	}
}
