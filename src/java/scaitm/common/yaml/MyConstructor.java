package common.yaml;

import org.yaml.snakeyaml.constructor.Constructor;

import server.ServerEnvironment;
import client.ClientEnvironment;

import common.Bot;
import common.Game;
import common.Map;
import common.MyFile;
import common.RequiredFile;
import common.TargetFile;

public class MyConstructor extends Constructor
{
	public MyConstructor()
	{
		addTypeDescription(Map.typeDescription);
		addTypeDescription(Bot.typeDescription);
		addTypeDescription(Game.typeDescription);
		addTypeDescription(MyFile.typeDescription);
		addTypeDescription(TargetFile.typeDescription);
		addTypeDescription(RequiredFile.typeDescription);
		addTypeDescription(ClientEnvironment.typeDescription);
		addTypeDescription(ServerEnvironment.typeDescription);
	}
}
