package common.protocols;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import common.file.PackedFile;

public interface RemoteServer extends Remote
{
	void connect(RemoteClient client) throws RemoteException;
	void disconnect(RemoteClient client) throws RemoteException;
	void checkAlive() throws RemoteException;
	PackedFile getDataDir() throws RemoteException, IOException;
}
