package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.Game;
import common.exceptions.StarcraftAlreadyRunningException;
import common.exceptions.StarcraftException;

public interface RemoteStarcraft extends Remote
{
	void start(Game game, int i) throws RemoteException, StarcraftAlreadyRunningException;
	boolean isDone() throws RemoteException, StarcraftException;
	void kill() throws RemoteException;
}
