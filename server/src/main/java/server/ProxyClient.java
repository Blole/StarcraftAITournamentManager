package server;

import java.rmi.RemoteException;

import common.Game;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyUsedException;
import common.protocols.RemoteClient;
import common.protocols.RemoteStarcraft;

public class ProxyClient implements RemoteClient
{
	public final RemoteClient remote;
	private final String cachedEndpointAddress;
	
	ProxyClient(RemoteClient remote) throws RemoteException
	{
		this.remote = remote;
		this.cachedEndpointAddress = remote.endpointAddress();
	}
	
	@Override
	public String toString()
	{
		String addr;
		try
		{
			addr = remote.endpointAddress();
		}
		catch (RemoteException e)
		{
			addr = cachedEndpointAddress+"(disconnected)";
		}
		return String.format("{ProxyClient %s}", addr);
	}
	
	
	
	
	
	@Override
	public RemoteStarcraft startMatch(Game game, int index) throws RemoteException, AllStarcraftInstanceSlotsAlreadyUsedException
	{
		return remote.startMatch(game, index);
	}
	@Override
	public void checkAlive() throws RemoteException
	{
		remote.checkAlive();
	}
	@Override
	public void kill() throws RemoteException
	{
		remote.kill();
	}
	@Override
	public void executeCommand(String command) throws RemoteException
	{
		remote.executeCommand(command);
	}
	@Override
	public byte[] screenshot() throws RemoteException
	{
		return remote.screenshot();
	}
	@Override
	public String endpointAddress() throws RemoteException
	{
		return remote.endpointAddress();
	}

	@Override
	public int getNumberOfUnusedStarcraftInstanceSlots() throws RemoteException
	{
		return remote.getNumberOfUnusedStarcraftInstanceSlots();
	}
}
