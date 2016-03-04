package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.CompleteGameResults;
import common.exceptions.StarcraftException;

public interface RemoteStarcraft extends Remote
{
	/**
	 * For polling whether the match is finished.
	 * Will only return the result once, afterwards it unexports itself.
	 * @return null if not done yet
	 * @throws StarcraftException if an error occurred while starting or during the game
	 */
	CompleteGameResults getResults() throws RemoteException, StarcraftException;
	void kill(String reason) throws RemoteException;
	int getLocalPort() throws RemoteException, StarcraftException;
	String getIP() throws RemoteException, StarcraftException;
}
