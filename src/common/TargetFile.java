package common;

import java.io.FileNotFoundException;

public class TargetFile extends RequiredFile
{
	private static final long serialVersionUID = 7552783803485220425L;
	
	public final String extractTo;

	public TargetFile(String name, String extractTo) throws FileNotFoundException
	{
		super(name);
		this.extractTo = extractTo;
	}
}
