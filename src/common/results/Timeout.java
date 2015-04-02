package common.results;

import common.Bot;

public class Timeout extends GameEndReason
{
	public final Bot bot;

	public Timeout(Bot bot)
	{
		this.bot = bot;
	}
}
