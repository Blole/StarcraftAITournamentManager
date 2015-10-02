package common.yaml;

import java.io.IOException;

import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;

import common.Bot;
import common.BwapiVersion;
import common.CommonEnvironment;
import common.Game;
import common.Map;
import common.exceptions.InvalidBwapiVersionString;
import common.file.MyFile;
import common.file.RequiredFile;
import common.file.CopyFile;
import common.status.Crash;
import common.status.Done;
import common.status.Running;
import common.status.Timeout;

public class MyConstructor extends Constructor
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
		this.yamlClassConstructors.put(NodeId.scalar, new Constructor(env));
	}

    private class Constructor extends ConstructScalar
    {
    	private CommonEnvironment env = null;
    	
    	public Constructor(CommonEnvironment env)
    	{
    		this.env = env;
    	}
    	
        @Override
		public Object construct(Node nnode)
        {
        	String string = (String) constructScalar((ScalarNode)nnode);
            try
			{
	        	if (nnode.getType().equals(BwapiVersion.class))
					return new BwapiVersion(env, string);
	        	else if (nnode.getType().equals(Bot.class))
					return Bot.load(env, string);
        	}
            catch (IOException | InvalidBwapiVersionString e)
			{
				throw new RuntimeException(e);
			}
            
            return super.construct(nnode);
        }
    }
}
