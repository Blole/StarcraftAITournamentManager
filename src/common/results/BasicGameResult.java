package common.results;



public class BasicGameResult
{
	private final int[] scores;
	private GameEndReason reason;
	private final int frames = 0;
	private final int ms = 0;

	public BasicGameResult(int[] scores, GameEndReason reason)
	{
		this.scores = scores;
		this.reason = reason;
	}
}
