package common.results;

import java.io.Serializable;
import java.util.List;

import common.Bot;

public class Win extends GameEndReason implements Serializable
{
	private static final long serialVersionUID = 4778242080272839959L;
	
	
	
	public final List<Bot> bots;

	public Win(List<Bot> bots)
	{
		this.bots = bots;
	}
}
