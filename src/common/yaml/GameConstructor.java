package common.yaml;

import java.io.File;
import java.io.IOException;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import common.Bot;

public class GameConstructor extends MyConstructor
{
	public File botDir;

	public GameConstructor(File botDir)
	{
		this.botDir = botDir;
        this.yamlConstructors.put(Bot.typeDescription.getTag(), new ConstructBot());
    }

    private class ConstructBot extends AbstractConstruct
    {
        @Override
		public Object construct(Node node)
        {
            String name = (String) constructScalar((ScalarNode)node);
            try
			{
				return Bot.load(botDir, name);
			}
            catch (IOException e)
			{
				throw new RuntimeException(e);
			}
        }
    }
}
