package common.exceptions;

import common.protocols.RemoteClient;

@SuppressWarnings("serial")
public class StarcraftException extends Exception
{
	public final RemoteClient client;

	public StarcraftException(RemoteClient client, String msg)
	{
		super(msg);
		this.client = client;
	}
}
