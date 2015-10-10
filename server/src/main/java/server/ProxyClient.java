package server;

import java.rmi.RemoteException;

import common.Game;
import common.exceptions.AllStarcraftInstanceSlotsAlreadyBusyException;
import common.protocols.RemoteClient;
import common.protocols.RemoteStarcraft;

public class ProxyClient implements RemoteClient
{
	private final Server server;
	public final RemoteClient remote;
	private String cachedEndpointAddress = null;
	
	ProxyClient(Server server, RemoteClient remote) throws RemoteException
	{
		this.server = server;
		this.remote = remote;
		this.cachedEndpointAddress = remote.endpointAddress();
	}
	
	@Override
	public String toString()
	{
		return String.format("{ProxyClient %s%s}", endpointAddress(), isAlive()?"":"(dead)");
	}
	
	private void onRemoteException(RemoteException e)
	{
		server.clientManager.onRemoteException(this, e);
	}
	
	
	
	@Override
	public RemoteStarcraft startMatch(Game game, int index) throws RemoteException, AllStarcraftInstanceSlotsAlreadyBusyException
	{
		try
		{
			return remote.startMatch(game, index);
		}
		catch (RemoteException e)
		{
			onRemoteException(e);
			throw e;
		}
	}
	
	public boolean isAlive()
	{
		try
		{
			checkAlive();
			return true;
		}
		catch (RemoteException e)
		{
			return false;
		}
	}
	
	@Override
	public void checkAlive() throws RemoteException
	{
		try
		{
			remote.checkAlive();
		}
		catch (RemoteException e)
		{
			onRemoteException(e);
			throw e;
		}
	}
	
	public void tryKill()
	{
		try
		{
			kill();
		}
		catch (RemoteException e)
		{
		}
	}
	
	@Override
	public void kill() throws RemoteException
	{
		remote.kill();
	}
	@Override
	public void executeCommand(String command) throws RemoteException
	{
		try
		{
			remote.executeCommand(command);
		}
		catch (RemoteException e)
		{
			onRemoteException(e);
			throw e;
		}
	}
	@Override
	public byte[] screenshot() throws RemoteException
	{
		try
		{
			return remote.screenshot();
		}
		catch (RemoteException e)
		{
			onRemoteException(e);
			throw e;
		}
	}
	@Override
	public String endpointAddress()
	{
		return cachedEndpointAddress;
	}
	
	@Override
	public int getOpenStarcraftInstanceSlotCount() throws RemoteException
	{
		try
		{
			return remote.getOpenStarcraftInstanceSlotCount();
		}
		catch (RemoteException e)
		{
			onRemoteException(e);
			throw e;
		}
	}
	
	public int getOpenStarcraftInstanceSlotCountOrZero()
	{
		try
		{
			return getOpenStarcraftInstanceSlotCount();
		}
		catch (RemoteException e)
		{
			return 0;
		}
	}
}
