package common;

import java.rmi.server.RemoteObject;

public abstract class Helper
{
	public static String getEndpointAddress(RemoteObject remote)
	{
		return remote.getRef().remoteToString().replaceFirst(".*?endpoint:\\[([0-9.:]+)\\].*", "$1");
	}
}
