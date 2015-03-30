package common.protocols;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import common.Game;
import common.PackedFile;

public interface RemoteClient extends Remote
{
	byte[] screenshot() throws RemoteException;
	void kill() throws RemoteException;
	RemoteStarcraftGame startStarcraftGame(Game game, int i) throws RemoteException;
	
	void delete(String path) throws RemoteException, IOException;
	void extractFile(PackedFile file, String extractTo) throws RemoteException, IOException;
	PackedFile getFile(String path) throws RemoteException, IOException;
	
	void executeCommand(String command) throws RemoteException;
}
