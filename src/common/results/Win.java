package common.results;

import java.util.List;

import common.Bot;

public class Win extends GameEndReason
{
	public final List<Bot> bots;

	public Win(List<Bot> bots)
	{
		this.bots = bots;
	}
}
