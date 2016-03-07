package client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.TypeDescription;

import common.CommonEnvironment;
import common.file.CopyFile;

public class CommonClientEnvironment extends CommonEnvironment implements Serializable
{
	private static final long serialVersionUID = -1112937899936238799L;
	public static final TypeDescription typeDescription = new TypeDescription(CommonClientEnvironment.class);
	static
	{
		typeDescription.putListPropertyType("copyFiles", CopyFile.class);
		typeDescription.putListPropertyType("injectoryArguments", String.class);
		typeDescription.putMapPropertyType("environmentVariables", String.class, String.class);
	}
	
	
	
	public final List<CopyFile> copyFiles;
	public final List<String> injectoryArguments;
	public final LinkedHashMap<String,String> environmentVariables;
	
	/** Empty constructor */
	public CommonClientEnvironment()
	{
		copyFiles = new ArrayList<>();
		injectoryArguments = new ArrayList<>();
		environmentVariables = new LinkedHashMap<>();
	}
}
