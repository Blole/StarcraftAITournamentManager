package common;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.nodes.Tag;

import server.ServerEnvironment;

import common.exceptions.InvalidBwapiVersionString;

public class BwapiVersion implements Serializable, Comparable<BwapiVersion>
{
	private static final long serialVersionUID = -6896962609170302139L;
	public static final TypeDescription typeDescription = new TypeDescription(BwapiVersion.class, new Tag("!lol"));
	
	
	
	private final String versionString;

	public BwapiVersion(String versionString)
	{
		this.versionString = versionString;
	}
	
	public TargetFile getFile(ServerEnvironment env) throws InvalidBwapiVersionString
	{
		LinkedHashMap<BwapiVersion,TargetFile> m = new LinkedHashMap<>();
		for (Entry<BwapiVersion,TargetFile> a : env.bwapiVersions.entrySet())
		{
			m.put(a.getKey(), a.getValue());
		}
		System.out.println(m.containsKey(this));
		System.out.println(m);
		
		if (env.bwapiVersions.containsKey(this))
			return env.bwapiVersions.get(this);
		else
			throw new InvalidBwapiVersionString(env, this);
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
