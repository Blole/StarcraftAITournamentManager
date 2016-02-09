package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.exceptions.StarcraftException;
import common.file.PackedFile;
import common.status.Done;

public interface RemoteStarcraft extends Remote
{
	/**
	 * For polling whether the match is finished.
	 * Throws a StarcraftException if an error occurred while starting or during the game.
	 */
	boolean isFinished() throws RemoteException, StarcraftException;
	Done getResult() throws RemoteException, StarcraftException;
	PackedFile getReplay() throws RemoteException, StarcraftException;
	PackedFile getWriteDirectory() throws RemoteException, StarcraftException;
	void kill(String reason) throws RemoteException;
	int getLocalPort() throws RemoteException, StarcraftException;
	String getIP() throws RemoteException, StarcraftException;
}
