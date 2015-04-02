package common.exceptions;

public class StarcraftAlreadyRunningException extends StarcraftException
{
	private static final long serialVersionUID = -6110017545332219129L;

	public StarcraftAlreadyRunningException()
	{
		super("StarCraft already running");
	}
}
