package common.exceptions;

import common.Bot;

public class BotTimeoutException extends BotException
{
	private static final long serialVersionUID = 8207621355838155638L;
	
	
	
	public final Bot bot;

	public BotTimeoutException(Bot bot)
	{
		super(bot.name+" timed out");
		this.bot = bot;
	}
}
