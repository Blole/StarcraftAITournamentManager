package common;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.file.MyFile;

public class CommonEnvironment implements Serializable
{
	private static final long serialVersionUID = 406861622905465217L;

	public static final TypeDescription typeDescription = new TypeDescription(CommonEnvironment.class);
	
	public MyFile dataDir = new MyFile("data/");
	
	
	public MyFile botDir()
	{
		return new MyFile(dataDir, "bots/");
	}
	public MyFile mapDir()
	{
		return new MyFile(dataDir, "maps/");
	}
	public MyFile bwapiVersionsDir()
	{
		return new MyFile(dataDir, "bwapiversions/");
	}
	public MyFile iniFile()
	{
		return new MyFile(dataDir, "bwapi.ini");
	}
	public MyFile tournamentModuleYaml()
	{
		return new MyFile(dataDir, "tournamentmodule.yaml");
	}
}
