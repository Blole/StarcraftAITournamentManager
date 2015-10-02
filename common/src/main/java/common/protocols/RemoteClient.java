package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.Game;
import common.exceptions.StarcraftException;

public interface RemoteClient extends Remote
{
	RemoteStarcraft startMatch(Game game, int index) throws RemoteException, StarcraftException;
	
	void checkAlive() throws RemoteException;
	void kill() throws RemoteException;
	void executeCommand(String command) throws RemoteException;
	byte[] screenshot() throws RemoteException;
}
