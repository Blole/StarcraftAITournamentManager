package common.exceptions;

import org.apache.commons.lang3.StringUtils;

import server.ServerEnvironment;

import common.BwapiVersion;

public class InvalidBwapiVersionString extends Exception
{
	private static final long serialVersionUID = 64267823794499591L;
	
	
	
	public InvalidBwapiVersionString(ServerEnvironment env, BwapiVersion bwapiVersion)
	{
		super("bwapiVersion '"+bwapiVersion+ "' not recognized, known versions are:\n- '"+
				StringUtils.join(env.bwapiVersions.keySet(), "'\n- '")+"'");
	}
}
