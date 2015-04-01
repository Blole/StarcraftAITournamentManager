package client;

import java.io.Serializable;

import common.Environment;

public class ClientEnvironment extends Environment implements Serializable
{
	private static final long serialVersionUID = -6040955128596907730L;
	
	public String serverUrl = null;
	public int starcraftStartingTimeout = 0;
	public int matchStartingTimeout = 0;
	public int matchEndingTimeout = 0;
}
