package common.exceptions;

import org.apache.commons.lang3.StringUtils;

import common.BwapiVersion;
import common.file.TargetFile;

public class InvalidBwapiVersionString extends Exception
{
	private static final long serialVersionUID = 64267823794499591L;
	
	
	
	public InvalidBwapiVersionString(java.util.Map<BwapiVersion, TargetFile> knownBwapiVersions, BwapiVersion bwapiVersion)
	{
		super("bwapiVersion '"+bwapiVersion+ "' not recognized, known versions are:\n- '"+
				StringUtils.join(knownBwapiVersions.keySet(), "'\n- '")+"'");
	}
}
