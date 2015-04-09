package common.status;

import java.io.Serializable;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.TypeDescription;

import common.Bot;
import common.exceptions.InvalidResultsException;

public class GameResult implements Serializable
{
	private static final long serialVersionUID = -5345577906937910410L;
	public static final TypeDescription typeDescription = new TypeDescription(GameResult.class, "!result");
	
	
	
	public Object common = null;
	public LinkedHashMap<Bot, Object> individual = new LinkedHashMap<>();
	
	public void add(Bot bot, Done result) throws InvalidResultsException
	{
		if (common == null)
			common = result.common;
		else if (result.common != null && !common.equals(result.common))
			throw new InvalidResultsException(String.format("non-equal common fields in results: '%s' and '%s'", common, result.common));
		
		if (result.individual != null)
		{
			if (individual.put(bot, result.individual) != null)
				throw new InvalidResultsException("tried to add individual result for bot "+bot+" twice");
		}
	}
}
