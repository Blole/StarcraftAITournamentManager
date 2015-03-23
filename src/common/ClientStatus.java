package common;

import java.io.Serializable;

public enum ClientStatus implements Serializable
{
	READY,
	STARTING,
	RUNNING, 
	SENDING
}
