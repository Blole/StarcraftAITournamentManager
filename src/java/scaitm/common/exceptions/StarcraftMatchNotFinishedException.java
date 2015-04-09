package common.exceptions;

public class StarcraftMatchNotFinishedException extends StarcraftException
{
	private static final long serialVersionUID = -1230464412920783715L;

	
	
	public StarcraftMatchNotFinishedException()
	{
		super("StarCraft match not finished yet");
	}
}
