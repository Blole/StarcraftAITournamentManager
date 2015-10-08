package server.exceptions;

import java.io.IOException;

import common.file.MyFile;

public class ServerGameResultsDirAlreadyExistsException extends IOException
{
	private static final long serialVersionUID = 7697224758615216681L;
	
	
	public ServerGameResultsDirAlreadyExistsException(MyFile gameFile, MyFile resultsDir)
	{
		super(String.format("error parsing queued game file '%s', the corresponding results dir already exists '%s'", gameFile, resultsDir));
	}
}
