package client;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.Environment;

public class ClientEnvironment extends Environment implements Serializable
{
	private static final long serialVersionUID = -6040955128596907730L;
	public static final TypeDescription typeDescription = new TypeDescription(ClientEnvironment.class);
	
	
	
	public String serverUrl = null;
	public double starcraftStartingTimeout = 10;
	public double matchStartingTimeout = 60;
	public double matchEndingTimeout = 60;
}
