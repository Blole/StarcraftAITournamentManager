package common.results;

import java.io.Serializable;



public class BasicGameResult implements Serializable
{
	private static final long serialVersionUID = 6341856802689068846L;
	
	
	
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
