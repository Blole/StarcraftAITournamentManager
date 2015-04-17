package common.yaml;

import java.io.File;
import java.io.IOException;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;

import common.Bot;

public class GameConstructor extends MyConstructor
{
	public File botDir;

	public GameConstructor(File botDir)
	{
		this.botDir = botDir;
		this.yamlClassConstructors.put(NodeId.scalar, new ConstructBot());
    }

    private class ConstructBot extends ConstructScalar
    {
        @Override
		public Object construct(Node nnode)
        {
        	if (nnode.getType().equals(Bot.class))
        	{
	            String name = (String) constructScalar((ScalarNode)nnode);
	            try
				{
					return Bot.load(botDir, name);
				}
	            catch (IOException e)
				{
					throw new RuntimeException(e);
				}
        	}
        	else
        		return super.construct(nnode);
        }
    }
}
