package common.exceptions;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import common.BwapiVersion;
import common.CommonEnvironment;

public class InvalidBwapiVersionString extends Exception
{
	private static final long serialVersionUID = 64267823794499591L;
	
	
	
	public InvalidBwapiVersionString(CommonEnvironment env, BwapiVersion bwapiVersion)
	{
		super("bwapiVersion '"+bwapiVersion+ "' not recognized, known versions are:\n- "+
				StringUtils.join(FileUtils.listFiles(env.bwapiVersionsDir(), null, false), "\n- "));
	}
}
