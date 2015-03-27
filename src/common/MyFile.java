package common;

import java.io.File;

@SuppressWarnings("serial")
public class MyFile extends File
{
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
	 * Returns true if this is a directory entry. A directory entry is defined to be one whose name ends with a '/'.
	 * @return true if this is a directory entry
	 */
	@Override
	public boolean isDirectory()
	{
		return super.isDirectory() || endsWithSlash;
	}
}
