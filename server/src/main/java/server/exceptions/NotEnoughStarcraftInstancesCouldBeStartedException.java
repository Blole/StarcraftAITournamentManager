package server.exceptions;

import common.exceptions.StarcraftException;
import server.ServerGame;

public class NotEnoughStarcraftInstancesCouldBeStartedException extends StarcraftException
{
	private static final long serialVersionUID = 8446330315608306596L;
	public final ServerGame game;
	public final int started;

	
	public NotEnoughStarcraftInstancesCouldBeStartedException(ServerGame game, int started)
	{
		super(String.format("only %d/%d started", started, game.game.bots.length));
		this.game = game;
		this.started = started;
	}
}
