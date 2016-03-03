package server.exceptions;

import java.io.IOException;

import common.file.MyFile;

public class ServerGameResultsDirAlreadyExistsException extends IOException
{
	private static final long serialVersionUID = 7697224758615216681L;
	public final MyFile gameFile;
	public final MyFile resultsDir;
	
	
	public ServerGameResultsDirAlreadyExistsException(MyFile gameFile, MyFile resultsDir)
	{
		super(String.format("the results dir '%s' for queued game '%s' already exists", resultsDir, gameFile));
		this.gameFile = gameFile;
		this.resultsDir = resultsDir;
	}
}
