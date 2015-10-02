package common.protocols;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import common.Game;
import common.exceptions.StarcraftException;
import common.file.PackedFile;

public interface RemoteClient extends Remote
{
	RemoteStarcraft startMatch(Game game, int index) throws RemoteException, StarcraftException;
	
	@Deprecated
	void delete(String path) throws RemoteException, IOException;
	@Deprecated
	void extractFile(PackedFile file, String extractTo) throws RemoteException, IOException;
	@Deprecated
	PackedFile getFile(String path) throws RemoteException, IOException;
	
	void checkAlive() throws RemoteException;
	void kill() throws RemoteException;
	void executeCommand(String command) throws RemoteException;
	byte[] screenshot() throws RemoteException;
}
