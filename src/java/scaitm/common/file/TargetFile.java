package common.file;

import java.io.FileNotFoundException;
import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

public class TargetFile extends RequiredFile implements Serializable
{
	private static final long serialVersionUID = 7552783803485220425L;
	public static final TypeDescription typeDescription = new TypeDescription(TargetFile.class, "!target");
	
	
	
	public final String extractTo;

	public TargetFile(String name, String extractTo) throws FileNotFoundException
	{
		super(name);
		this.extractTo = extractTo;
	}
}
