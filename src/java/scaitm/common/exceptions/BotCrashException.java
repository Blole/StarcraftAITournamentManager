package common.exceptions;

import common.Bot;

public class BotCrashException extends Exception
{
	private static final long serialVersionUID = -2843983072590416696L;
	
	public final Bot bot;

	public BotCrashException(Bot bot)
	{
		super(bot.name+" crashed");
		this.bot = bot;
	}
}
