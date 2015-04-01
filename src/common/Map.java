package common;

import java.io.File;
import java.io.Serializable;

import server.ServerEnvironment;

public class Map implements Serializable
{
	private static final long serialVersionUID = 8525996517284059304L;
	
	public final String name;
	public final String path;
	
	
	public Map(String path)
	{
		this.path = path;
		this.name = new File(path).getName();
	}

	@Override
	public String toString()
	{
		return String.format("{Map:%s}", name);
	}
	
	public File getFile(ServerEnvironment env)
	{
		return env.lookupFile("$map_dir/"+path);
	}
}
