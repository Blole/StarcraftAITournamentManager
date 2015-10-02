package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.exceptions.StarcraftException;
import common.file.PackedFile;
import common.status.Done;

public interface RemoteStarcraft extends Remote
{
	/**
	 * For polling until the match is finished and there is a result.
	 * Throws a StarcraftException if an error occurred while starting or during the game.
	 * 
	 * @return a result if the match has finished, otherwise null
	 */
	boolean isFinished() throws RemoteException, StarcraftException;
	Done getResult() throws RemoteException, StarcraftException;
	PackedFile getReplay() throws RemoteException, StarcraftException;
	PackedFile getWriteDirectory() throws RemoteException, StarcraftException;
	void kill() throws RemoteException;
}
