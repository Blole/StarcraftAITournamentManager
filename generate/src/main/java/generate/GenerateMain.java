package generate;

import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


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
			Constructor constructor = new Constructor();
			constructor.addTypeDescription(GenerationEnvironment.typeDescription);
			Yaml yaml = new Yaml(constructor);
			GenerationEnvironment settings = yaml.loadAs(System.in, GenerationEnvironment.class);
			
			List<GameMockup> games = generate1v1RoundRobin(settings.rounds, settings.maps, settings.bots);
			yaml.dump(games, new OutputStreamWriter(System.out));
		}
	}
	
	public static List<GameMockup> generate1v1RoundRobin(int rounds, List<String> maps, List<String> bots)
	{
		List<GameMockup> games = new ArrayList<>();
		
		for (int round=0; round<rounds; round++)
		{
			for(String map : maps)
			{
				for (int i = 0; i < bots.size(); i++)
				{
					for (int j = i+1; j < bots.size(); j++)
					{
						String a = bots.get(i);
						String b = bots.get(j);
						games.add(new GameMockup(round, map, a, b));
					}
				}
			}
		}
		
		return games;
	}
}
