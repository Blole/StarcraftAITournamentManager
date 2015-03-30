package common;

import java.io.File;
import java.io.Serializable;

public class MyFile extends File implements Serializable
{
	private static final long serialVersionUID = -7563594135120571872L;
	
	private final boolean endsWithSlash;

	public MyFile(String name)
	{
		super(name);
		endsWithSlash = name.endsWith("/") || name.endsWith("\\");
	}
	
	public MyFile(File base, String name)
	{
		super(base, name);
		endsWithSlash = name.endsWith("/") || name.endsWith("\\");
	}
	
	/**
	 * @return true if this file exists and is a directory, or if it does not exist and it's name ends with a '/' or '\'
	 */
	@Override
	public boolean isDirectory()
	{
		if (exists())
			return super.isDirectory();
		else
			return endsWithSlash;
	}
	
	@Override
	public String getName()
	{
		return super.getName() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String toString()
	{
		return super.toString() + (isDirectory() ? File.separatorChar : "");
	}
}
