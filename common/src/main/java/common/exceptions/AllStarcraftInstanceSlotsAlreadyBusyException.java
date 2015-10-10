package common.exceptions;

public class AllStarcraftInstanceSlotsAlreadyBusyException extends StarcraftException
{
	private static final long serialVersionUID = 886907877566172110L;
	
	public AllStarcraftInstanceSlotsAlreadyBusyException(int maxStarcraftInstances)
	{
		super(String.format("all %s slots used", maxStarcraftInstances));
	}
}
