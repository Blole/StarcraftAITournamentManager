package common.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.FileMessage;
import common.InstructionMessage;

public interface RemoteClient extends Remote
{
	byte[] screenshot() throws RemoteException;
	void kill() throws RemoteException;
	void startStarCraft(InstructionMessage instructions) throws RemoteException;
	void extractFile(FileMessage msg) throws RemoteException;
	void executeCommand(String command) throws RemoteException;
	
	FileMessage getFile(String path, String extractTo) throws RemoteException;
}
