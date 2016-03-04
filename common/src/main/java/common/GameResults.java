package common;

import java.io.Serializable;
import java.util.ArrayList;

import org.yaml.snakeyaml.TypeDescription;

import common.exceptions.DifferingCommonResultsException;
import common.status.Done;

public class GameResults implements Serializable
{
	private static final long serialVersionUID = -5345577906937910410L;
	public static final TypeDescription typeDescription = new TypeDescription(GameResults.class, "!result");
	
	
	
	public Object common = null;
	public ArrayList<Object> individual = new ArrayList<>();
	
	public void add(int index, Done result) throws DifferingCommonResultsException
	{
		if (common == null)
			common = result.common;
		else if (!common.equals(result.common))
			throw new DifferingCommonResultsException();
		
		if (result.individual != null)
		{
			while (individual.size() <= index)
				individual.add(null);
			individual.set(index, result.individual);
		}
	}
}
