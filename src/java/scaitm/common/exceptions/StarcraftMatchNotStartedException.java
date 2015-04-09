package common.exceptions;

public class StarcraftMatchNotStartedException extends StarcraftException
{
	private static final long serialVersionUID = -2604255713614994101L;
	
	
	
	public StarcraftMatchNotStartedException()
	{
		super("StarCraft match not started yet");
	}
}
