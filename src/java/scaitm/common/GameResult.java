package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.yaml.snakeyaml.TypeDescription;

import common.exceptions.InvalidResultsException;
import common.status.Done;

public class GameResult implements Serializable
{
	private static final long serialVersionUID = -5345577906937910410L;
	public static final TypeDescription typeDescription = new TypeDescription(GameResult.class, "!result");
	
	
	
	protected Game game;
	public Object common = null;
	public ArrayList<BotResult> individual = new ArrayList<>();
	
	public GameResult(Game game)
	{
		this.game = game;
	}
	
	public void add(Bot bot, Done result) throws InvalidResultsException
	{
		if (common == null)
			common = result.common;
		
		if (result.individual != null)
		{
			long gameBotCount = Arrays.stream(game.bots).filter(b->b.equals(bot)).count();
			long alreadyCollectedBotCount = individual.stream().map(r->r.bot).filter(b->b.equals(bot)).count();
			
			if (alreadyCollectedBotCount > gameBotCount)
				throw new InvalidResultsException("tried to record too many individual results for "+bot+" ("+(alreadyCollectedBotCount+1)+" times)");
			else
				individual.add(new BotResult(bot, result.individual));
		}
	}
	
	
	
	public static class BotResult
	{
		public final Bot bot;
		public final Object result;

		BotResult(Bot bot, Object result)
		{
			this.bot = bot;
			this.result = result;
		}
	}
}
