package common.exceptions;

public class StarcraftNotRunningException extends StarcraftException
{
	private static final long serialVersionUID = -8072783355450000464L;

	public StarcraftNotRunningException()
	{
		super("StarCraft is not running");
	}
}
