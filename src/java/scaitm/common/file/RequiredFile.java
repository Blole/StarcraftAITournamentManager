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
		this(null, name);
	}
	
	public RequiredFile(File base, String name) throws FileNotFoundException
	{
		super(base, name);
		
		if (!exists())
			throw new FileNotFoundException("File '" + this + "' does not exist");
	}
}
