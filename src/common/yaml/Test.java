package common.yaml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import common.Bot;
import common.Game;
import common.Map;
import common.MyFile;
import common.TargetFile;

public final class Test
{
	public static void main(String[] args) throws IOException
	{
		String yamlData = FileUtils.readFileToString(new File("games.yaml"));
		
		Constructor constructor = new GameConstructor(new MyFile("data/bots/"));
		constructor.addTypeDescription(TargetFile.typeDescription);
		constructor.addTypeDescription(Game.typeDescription);
		constructor.addTypeDescription(Map.typeDescription);
		constructor.addTypeDescription(Bot.typeDescription);
		
		Representer representer = new GameRepresenter();
		representer.addClassTag(Game.class, new Tag("!game"));
		
		DumperOptions dumperOptions = new DumperOptions();
		
		Yaml yaml = new Yaml(constructor, representer, dumperOptions);
		
		Object loaded = yaml.loadAs(yamlData, List.class);
		
		System.out.println("#---read---");
		System.out.println(yamlData);
		System.out.println("#---loaded---");
		System.out.println(loaded);
		System.out.println();
		System.out.println("#---dumped---");
		System.out.println(yaml.dump(loaded));
		System.out.println();
		//List<Bot> bots = BotDocument.load(FileUtils.readFileToString(new File(args[0])));
		//List<Map> maps = MapDocument.load(new File(args[1]));
		
		//List<Game> games = GameListGenerator.generate1v1RoundRobin(2, maps, bots);
		
		/*
		System.out.println(games);
		String y = GamesDocument.dump(games);
		System.out.println(y);
		String yamlData = FileUtils.readFileToString(new File("games.yaml"));
		System.out.println(GamesDocument.load(yamlData));
		*/
	}
}
