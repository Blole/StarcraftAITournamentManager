package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;
import common.status.Done;
import common.status.GameStatus;

public interface RemoteStarcraft extends Remote
{
	void start(Game game, int i) throws RemoteException, StarcraftAlreadyRunningException;
	
	/**
	 * For polling until the match is finished and there is a result.
	 * Throws a StarcraftException if an error occurred while starting or during the game.
	 * 
	 * @return a result if the match has finished, otherwise null
	 */
	GameStatus getStatus() throws RemoteException, StarcraftException;
	Done getResult() throws RemoteException, StarcraftException;
	void kill() throws RemoteException;
}
