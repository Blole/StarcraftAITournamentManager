package server;

import java.rmi.ConnectException;
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

	public void clientCalledDisconnect(RemoteClient remoteClient)
	{
		Optional<ProxyClient> optionalClient = clients.stream().filter(pc->pc.remote.equals(remoteClient)).findFirst();
		if (optionalClient.isPresent())
		{
			ProxyClient client = optionalClient.get();
			clients.remove(client);
			server.log("%s disconnected", client);
		}
	}
	
	public void onRemoteException(ProxyClient client, RemoteException e)
	{
		if (clients.remove(client))
		{
			if (e instanceof ConnectException)
				server.log("%s died", client);
			else
			{
				server.log("%s disconnected due to exception: %s", client, e.getMessage());
				e.printStackTrace();
			}
		}
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
		if (clients.size() > 0)
		{
			server.log("killing all clients (%d)", clients.size());
			for (ProxyClient client : clients())
				client.tryKill();
			clients.clear();
		}
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
