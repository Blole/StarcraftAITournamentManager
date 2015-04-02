package common;

import java.io.File;
import java.io.FileNotFoundException;

import org.yaml.snakeyaml.TypeDescription;

public class RequiredFile extends MyFile
{
	private static final long serialVersionUID = -5707673290425180225L;
	public static final TypeDescription typeDescription = new TypeDescription(RequiredFile.class, "!required");
	
	
	
	public RequiredFile(String name) throws FileNotFoundException
	{
		super(name);
		
		if (!exists())
			throw new FileNotFoundException("File '" + this + "' does not exist");
	}
	
	public RequiredFile(File base, String name) throws FileNotFoundException
	{
		super(base, name);
		
		if (!exists())
			throw new FileNotFoundException("File '" + this + "' does not exist");
	}
}
