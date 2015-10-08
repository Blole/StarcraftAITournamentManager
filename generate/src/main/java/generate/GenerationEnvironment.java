package generate;

import java.util.List;

import org.yaml.snakeyaml.TypeDescription;

public class GenerationEnvironment
{
	public static final TypeDescription typeDescription = new TypeDescription(GenerationEnvironment.class);
	static
	{
		typeDescription.putListPropertyType("bots", String.class);
		typeDescription.putListPropertyType("maps", String.class);
	}
	
	public int rounds;
	public List<String> bots;
	public List<String> maps;
	
}
