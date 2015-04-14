package common;

import java.io.Serializable;
import java.util.ArrayList;

import org.yaml.snakeyaml.TypeDescription;

import common.exceptions.InvalidResultsException;
import common.status.Done;

public class GameResult implements Serializable
{
	private static final long serialVersionUID = -5345577906937910410L;
	public static final TypeDescription typeDescription = new TypeDescription(GameResult.class, "!result");
	
	
	
	public Object common = null;
	public ArrayList<BotResult> individual = new ArrayList<>();
	
	public void add(Bot bot, Done result) throws InvalidResultsException
	{
		if (common == null)
			common = result.common;
		
		if (result.individual != null)
			individual.add(new BotResult(bot, result.individual));
	}
	
	
	
	public static class BotResult
	{
		public Bot bot;
		public Object result;
		
		@SuppressWarnings("unused")
		private BotResult() //dummy for yaml
		{
		}
		
		public BotResult(Bot bot, Object result)
		{
			this.bot = bot;
			this.result = result;
		}
	}
}
