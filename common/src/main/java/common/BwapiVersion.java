package common;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.exceptions.InvalidBwapiVersionString;
import common.file.MyFile;

public class BwapiVersion implements Serializable, Comparable<BwapiVersion>
{
	private static final long serialVersionUID = -6896962609170302139L;
	public static final TypeDescription typeDescription = new TypeDescription(BwapiVersion.class, "!bwapi_ver");
	
	
	
	private final String versionString;

	public BwapiVersion(CommonEnvironment env, String versionString) throws InvalidBwapiVersionString
	{
		this.versionString = versionString;
		
		MyFile dir = new MyFile(env.bwapiVersionsDir(), versionString);
		if (!dir.exists())
			throw new InvalidBwapiVersionString(env, this);
	}

	public MyFile getDir(CommonEnvironment env)
	{
		return new MyFile(env.bwapiVersionsDir(), versionString);
	}
	
	public MyFile getDll(CommonEnvironment env)
	{
		return new MyFile(getDir(env), "BWAPI.dll");
	}

	public MyFile getTournamentDll(CommonEnvironment env)
	{
		return new MyFile(getDir(env), "tournamentmodule.dll");
	}
	
	@Override
	public String toString()
	{
		return versionString;
	}
	
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BwapiVersion other = (BwapiVersion) obj;
        return versionString.equals(other.versionString);
	}
	
	@Override
	public int hashCode()
	{
		return versionString.hashCode();
	}

	@Override
	public int compareTo(BwapiVersion o)
	{
		return versionString.compareTo(o.versionString);
	}
}
