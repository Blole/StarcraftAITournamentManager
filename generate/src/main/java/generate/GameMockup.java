package generate;

public class GameMockup
{
	public final int round;
	public final String map;
	public final String[] bots;
	
	GameMockup(int round, String map, String... bots)
	{
		this.round = round;
		this.map = map;
		this.bots = bots;
	}
}
