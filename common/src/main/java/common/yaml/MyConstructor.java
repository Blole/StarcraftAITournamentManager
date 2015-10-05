package common.yaml;

import java.io.IOException;

import common.Bot;
import common.BwapiVersion;
import common.CommonEnvironment;
import common.Game;
import common.Map;
import common.exceptions.InvalidBwapiVersionString;
import common.file.CopyFile;
import common.file.MyFile;
import common.file.RequiredFile;
import common.status.Crash;
import common.status.Done;
import common.status.Running;
import common.status.Timeout;

public class MyConstructor extends ScalarConstructor
{
	private void addTypes()
	{
		addTypeDescription(Map.typeDescription);
		addTypeDescription(Bot.typeDescription);
		addTypeDescription(Game.typeDescription);
		addTypeDescription(MyFile.typeDescription);
		addTypeDescription(CopyFile.typeDescription);
		addTypeDescription(RequiredFile.typeDescription);
		
		addTypeDescription(Done.typeDescription);
		addTypeDescription(Crash.typeDescription);
		addTypeDescription(Timeout.typeDescription);
		addTypeDescription(Running.typeDescription);
	}

	/*
	 * when not given a CommonEnvironment, it won't be able to construct Bot or BwapiVersion
	 */
	public MyConstructor()
	{
		addTypes();
	}
	
	public MyConstructor(CommonEnvironment env)
	{
		addTypes();
		addScalarConstructor(BwapiVersion.class, s ->
		{
			try
			{
				return new BwapiVersion(env, s);
			}
			catch (InvalidBwapiVersionString e)
			{
				throw new RuntimeException(e);
			}
		}
		);
		addScalarConstructor(Bot.class, s ->
		{
			try
			{
				return Bot.load(env, s);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		);
	}
}
