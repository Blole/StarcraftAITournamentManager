package common.yaml;

import org.yaml.snakeyaml.representer.Representer;

import common.Bot;
import common.Game;
import common.Map;
import common.file.MyFile;
import common.file.RequiredFile;
import common.file.TargetFile;

public class MyRepresenter extends Representer
{
	public MyRepresenter()
	{
		addClassTag(Map.class,				 Map.typeDescription.getTag());
		addClassTag(Bot.class,				 Bot.typeDescription.getTag());
		addClassTag(Game.class,				 Game.typeDescription.getTag());
		addClassTag(MyFile.class,			 MyFile.typeDescription.getTag());
		addClassTag(TargetFile.class,		 TargetFile.typeDescription.getTag());
		addClassTag(RequiredFile.class,		 RequiredFile.typeDescription.getTag());
	}
}
