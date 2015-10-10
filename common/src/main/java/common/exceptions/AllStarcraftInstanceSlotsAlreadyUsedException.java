package common.exceptions;

public class AllStarcraftInstanceSlotsAlreadyUsedException extends StarcraftException
{
	private static final long serialVersionUID = 886907877566172110L;
	
	public AllStarcraftInstanceSlotsAlreadyUsedException(int maxStarcraftInstances)
	{
		super(String.format("all %s slots used", maxStarcraftInstances));
	}
}
