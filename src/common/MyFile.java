package common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

public class MyFile extends File implements Serializable
{
	private static final long serialVersionUID = -7563594135120571872L;
	public static final TypeDescription typeDescription = new TypeDescription(MyFile.class, "!file");
	
	
	
	private final boolean endsWithSlash;

	public MyFile(String name)
	{
		this(null, name);
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
	public String getAbsolutePath()
	{
		return super.getAbsolutePath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getCanonicalPath() throws IOException
	{
		return super.getCanonicalPath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getPath()
	{
		return super.getPath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getName()
	{
		return super.getName() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String toString()
	{
		return super.toString();
	}
}
