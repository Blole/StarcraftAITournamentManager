package server;

import java.net.Inet4Address;
import java.net.ServerSocket;

class ServerListenerThread extends Thread
{
	private ServerSocket 	serverSocket = null;
	private Server 			server;

	ServerListenerThread(Server server)
	{
		this.server = server;
		int port = server.env.get("port");
		
		try
		{
			serverSocket = new ServerSocket(port);
			server.log("Server: Server address " + Inet4Address.getLocalHost().getHostAddress() + " Listening on port " + port + "\n");
		}
		catch (Exception e)
		{
			System.out.println("Listener: Could not listen on port " + port);
			System.exit(-1);
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			ServerClientThread c = null;
			try
			{
				c = new ServerClientThread(serverSocket.accept(), server);
				server.log("Listener: New client connected\n");
				c.start();
				server.log("Listener: New client thread started\n");
			}
			catch (Exception e)
			{
				System.out.println("Listener: Error in run() main loop");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
	}

	synchronized void stopThread()
	{
		this.interrupt();
	}

	@Override
	protected void finalize()
	{
		try
		{
			serverSocket.close();
		}
		catch (Exception e)
		{
			System.out.println("Listener: Could not close.");
			e.printStackTrace();
		}
	}
}
