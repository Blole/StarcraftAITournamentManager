package common.results;

import java.io.Serializable;



public class GameResult implements Serializable
{
	private static final long serialVersionUID = 6341856802689068846L;
	
	
	
	protected final int[] scores;
	protected GameEndReason reason;
	protected final int frames;
	protected final int ms;

	public GameResult(int[] scores, GameEndReason reason)
	{
		this.scores = scores;
		this.reason = reason;
		frames = 0;
		ms = 0;
	}
}
