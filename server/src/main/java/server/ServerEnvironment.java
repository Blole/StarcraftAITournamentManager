package server;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

import common.BwapiVersion;
import common.CommonEnvironment;
import common.file.MyFile;
import common.file.RequiredFile;
import common.file.CopyFile;

public class ServerEnvironment extends CommonEnvironment implements Serializable
{
	private static final long serialVersionUID = -2475588136001739940L;
	public static final TypeDescription typeDescription = new TypeDescription(ServerEnvironment.class);
	static
	{
		typeDescription.putListPropertyType("filesToCopyToClientBeforeEachNewMatch", CopyFile.class);
		typeDescription.putMapPropertyType("bwapiVersions", BwapiVersion.class, CopyFile.class);
	}
	
	
	
	public String serverUrlPath = null;
	public int port = 1099;
	
	public double matchFinishedPollPeriod = 1.0;
	public boolean killClientsOnExit = false;
	public boolean exitWhenDone = false;
	
	public RequiredFile gameList;
	public MyFile results;
	public MyFile replaysDir;
	
	public MyFile starcraftDir()
	{
		return new MyFile(dataDir, "starcraft/");
	}
}
