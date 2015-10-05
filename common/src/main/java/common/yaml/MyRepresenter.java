package common.yaml;

import org.yaml.snakeyaml.nodes.Tag;

import common.Bot;
import common.Game;
import common.Map;
import common.file.CopyFile;
import common.file.MyFile;
import common.file.RequiredFile;

public class MyRepresenter extends ScalarRepresenter
{
	public MyRepresenter()
	{
		addClassTag(Map.class,				Map.typeDescription.getTag());
		addClassTag(Bot.class,				Bot.typeDescription.getTag());
		addClassTag(Game.class,				Game.typeDescription.getTag());
		addClassTag(MyFile.class,			MyFile.typeDescription.getTag());
		addClassTag(CopyFile.class,		 	CopyFile.typeDescription.getTag());
		addClassTag(RequiredFile.class,		RequiredFile.typeDescription.getTag());
		
		addScalarRepresentation(Map.class, Tag.STR, m -> m.path);
		addScalarRepresentation(Bot.class, Tag.STR, b -> b.name);
	}
}
