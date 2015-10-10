package common.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.server.RemoteObject;

public abstract class Helper
{
	public static String getEndpointAddress(RemoteObject remote)
	{
		String endpointAddress = remote.getRef().remoteToString().replaceFirst(".*?endpoint:\\[([0-9.:]+)\\].*", "$1");
		try
		{
			//this'll work if remote is the local computer
			endpointAddress = endpointAddress.replaceFirst(
					Inet4Address.getLocalHost().getHostAddress(),
					Inet4Address.getLocalHost().getHostName());
		}
		catch (UnknownHostException e)
		{}
		return endpointAddress;
	}
	
	public int getUnusedPortAbove(int start) throws IOException
	{
		try (ServerSocket socket = new ServerSocket(start))
		{
			return socket.getLocalPort();
		}
		catch (IOException e)
		{
			throw new IOException("Error finding unused port above "+start, e);
		}
	}
}
