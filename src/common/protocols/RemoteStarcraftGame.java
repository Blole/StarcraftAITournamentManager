package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.exceptions.StarcraftException;

public interface RemoteStarcraftGame extends Remote
{
	void kill() throws RemoteException;
	boolean isDone() throws RemoteException, StarcraftException;
}
