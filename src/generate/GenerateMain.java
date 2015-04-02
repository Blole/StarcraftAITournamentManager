package generate;

import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import common.Bot;
import common.Game;
import common.Map;
import common.MyFile;
import common.yaml.GameConstructor;
import common.yaml.GameRepresenter;


public final class GenerateMain
{
	public static void main(String[] args) throws FileNotFoundException
	{
		if (args.length != 0)
		{
			System.err.println("usage: java -jar generate.jar < IN.YAML > OUT.YAML");
			System.exit(-1);
		}
		else
		{
			//System.setIn(new FileInputStream("generate.yaml")); //TODO: remove line
			GameConstructor constructor = new GameConstructor(new MyFile("data/bots/")); //TODO: read this 'data/bots/' from somewhere...
			//but reading it from the same yaml doesn't work, as the bots we'll read are dependent on the value...
			constructor.addTypeDescription(GenerationSettings.typeDescription);
			
			Yaml yaml = new Yaml(constructor, new GameRepresenter());
			GenerationSettings settings = yaml.loadAs(System.in, GenerationSettings.class);
			
			List<Game> games = generate1v1RoundRobin(settings.rounds, settings.maps, settings.bots);
			
			yaml.dump(games, new OutputStreamWriter(System.out));
		}
	}
	
	public static List<Game> generate1v1RoundRobin(int rounds, List<Map> maps, List<Bot> bots)
	{
		List<Game> games = new ArrayList<>();
		
		int id = 0;
		
		for (int round=0; round<rounds; round++)
		{
			for(Map map : maps)
			{
				for (int i = 0; i < bots.size(); i++)
				{
					for (int j = i+1; j < bots.size(); j++)
					{
						Bot a = bots.get(i);
						Bot b = bots.get(j);
						games.add(new Game(id++, round, map, a, b));
					}
				}
			}
		}
		
		return games;
	}
	
	private static class GenerationSettings
	{
		public static final TypeDescription typeDescription = new TypeDescription(GenerationSettings.class);
		static
		{
			typeDescription.putListPropertyType("bots", Bot.class);
			typeDescription.putListPropertyType("maps", Map.class);
		}
		
		public int rounds;
		public List<Bot> bots;
		public List<Map> maps;
		
	}
}
