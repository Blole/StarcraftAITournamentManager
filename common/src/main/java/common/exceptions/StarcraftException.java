package common.exceptions;


public class StarcraftException extends Exception
{
	private static final long serialVersionUID = -8291297177363234151L;

	public StarcraftException()
	{
	}
	
	public StarcraftException(String msg)
	{
		super(msg);
	}

	public StarcraftException(Exception e)
	{
		super(e);
	}

	public StarcraftException(String msg, Exception e)
	{
		super(msg, e);
	}
}
