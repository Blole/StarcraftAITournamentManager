package server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Optional;

import common.protocols.RemoteClient;

public class ClientManager
{
	private final Server server;
	private final ArrayList<ProxyClient> clients = new ArrayList<>();

	public ClientManager(Server server)
	{
		this.server = server;
	}

	public void connected(RemoteClient remoteClient) throws RemoteException
	{
		ProxyClient client = new ProxyClient(server, remoteClient);
		clients.add(client);
		server.log("%s connected", client);
	}

	public void clientWantsToDisconnect(RemoteClient remoteClient)
	{
		Optional<ProxyClient> cliento = clients.stream().filter(pc->pc.remote.equals(remoteClient)).findFirst();
		if (cliento.isPresent())
			disconnected(cliento.get());
	}
	
	public void disconnected(ProxyClient client)
	{
		clients.remove(client);
		server.log("%s disconnected", client);
	}
	
	public ArrayList<ProxyClient> clients()
	{
		return new ArrayList<ProxyClient>(clients);
	}
	
	public int getOpenSlotCount()
	{
		int sum = 0;
		
		for (ProxyClient client : clients())
			sum += client.getOpenStarcraftInstanceSlotCountOrZero();
		
		return sum;
	}
	
	void killAll()
	{
		server.log("killing all clients");
		for (ProxyClient client : clients())
		{
			try
			{
				client.kill();
			}
			catch (RemoteException e)
			{
			}
		}
		clients.clear();
	}

	public void sendCommandToAll(String command)
	{
		server.log("sending command to all clients: %s", command);
		for (ProxyClient client : clients())
		{
			try
			{
				client.executeCommand(command);
			}
			catch (RemoteException e)
			{
				server.log("error sending command to "+client);
			}
		}
	}
}
