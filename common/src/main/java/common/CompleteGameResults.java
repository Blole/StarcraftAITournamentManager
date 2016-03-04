package common;

import java.io.Serializable;

import common.file.PackedFile;
import common.status.Done;

public class CompleteGameResults implements Serializable
{
	private static final long serialVersionUID = -717095011912042622L;
	
	public final Done result;
	public final PackedFile replay;
	public final PackedFile writeDirectory;
	
	public CompleteGameResults(Done result, PackedFile replay, PackedFile writeDirectory)
	{
		this.result = result;
		this.replay = replay;
		this.writeDirectory = writeDirectory;
	}
}
