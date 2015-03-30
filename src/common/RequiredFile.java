package common;

import java.io.File;
import java.io.FileNotFoundException;

public class RequiredFile extends MyFile
{
	private static final long serialVersionUID = -5707673290425180225L;

	public RequiredFile(String name) throws FileNotFoundException
	{
		super(name);
		throwIfNotFound();
	}
	
	public RequiredFile(File file, String name) throws FileNotFoundException
	{
		super(file, name);
		throwIfNotFound();
	}
	
	private void throwIfNotFound() throws FileNotFoundException
	{
		if (!exists())
			throw new FileNotFoundException(String.format("File '%s' not found", getAbsolutePath()));
	}
}
