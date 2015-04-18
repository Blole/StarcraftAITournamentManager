package common;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.nodes.Tag;

import common.exceptions.InvalidBwapiVersionString;
import common.file.TargetFile;

public class BwapiVersion implements Serializable, Comparable<BwapiVersion>
{
	private static final long serialVersionUID = -6896962609170302139L;
	public static final TypeDescription typeDescription = new TypeDescription(BwapiVersion.class, new Tag("!lol"));
	
	
	
	private final String versionString;

	public BwapiVersion(String versionString)
	{
		this.versionString = versionString;
	}
	
	public TargetFile getFile(java.util.Map<BwapiVersion, TargetFile> knownBwapiVersions) throws InvalidBwapiVersionString
	{
		TargetFile file = knownBwapiVersions.get(this);
		
		if (file != null)
			return file;
		else
			throw new InvalidBwapiVersionString(knownBwapiVersions, this);
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
