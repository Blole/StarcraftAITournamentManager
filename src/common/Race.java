package common;

public enum Race
{
	Terran,
	Protoss,
	Zerg;
	
	public String toStringShort()
	{
		return toString().substring(0, 1);
	}
}
