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

	public void connected(RemoteClient client_) throws RemoteException
	{
		ProxyClient client = new ProxyClient(client_);
		clients.add(client);
		server.log("%s connected", client);
	}

	public void disconnected(RemoteClient client_)
	{
		ProxyClient client;
		if (client_ instanceof ProxyClient)
			client = (ProxyClient) client_;
		else
		{
			Optional<ProxyClient> cliento = clients.stream().filter(pc->pc.remote.equals(client_)).findFirst();
			if (!cliento.isPresent())
				return;
			client = cliento.get();
		}
		
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
		{
			try
			{
				sum += client.getNumberOfUnusedStarcraftInstanceSlots();
			}
			catch (RemoteException e)
			{
				disconnected(client);
			}
		}
		
		return sum;
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
				disconnected(client);
			}
		}
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
				server.log("error killing "+client);
			}
		}
		clients.clear();
	}
}
