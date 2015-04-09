package common.protocols;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import common.PackedFile;

public interface RemoteClient extends Remote
{
	RemoteStarcraft starcraft() throws RemoteException;
	
	void delete(String path) throws RemoteException, IOException;
	void extractFile(PackedFile file, String extractTo) throws RemoteException, IOException;
	PackedFile getFile(String path) throws RemoteException, IOException;
	
	void kill() throws RemoteException;
	void executeCommand(String command) throws RemoteException;
	byte[] screenshot() throws RemoteException;
}
