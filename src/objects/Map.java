package objects;

import java.io.File;

public class Map
{
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
		return name;
	}
}
