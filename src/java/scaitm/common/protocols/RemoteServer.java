package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServer extends Remote
{
	void connect(RemoteClient client) throws RemoteException;
	void disconnect(RemoteClient client) throws RemoteException;
	boolean isAlive() throws RemoteException;
}
