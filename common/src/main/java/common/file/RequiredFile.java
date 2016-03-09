package common.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

public class RequiredFile extends MyFile implements Serializable
{
	private static final long serialVersionUID = -5707673290425180225L;
	public static final TypeDescription typeDescription = new TypeDescription(RequiredFile.class, "!required");
	
	
	
	public RequiredFile(String name) throws FileNotFoundException
	{
		super(name);
		check();
	}
	
	public RequiredFile(File file) throws FileNotFoundException
	{
		super(file);
		check();
	}
	
	public RequiredFile(File base, String name) throws FileNotFoundException
	{
		super(base, name);
		check();
	}
	
	@Override
	public RequiredFile require() throws FileNotFoundException
	{
		check(); //check again
		return this;
	}
	
	private void check() throws FileNotFoundException
	{
		if (!exists())
			throw new FileNotFoundException("file '" + this + "' does not exist");
	}
}
