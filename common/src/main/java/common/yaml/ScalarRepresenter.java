package common.yaml;

import java.util.function.Function;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class ScalarRepresenter extends Representer
{
    public <T> void addScalarRepresentation(Class<T> clazz, Tag tag, Function<T,String> function)
    {
    	this.representers.put(clazz, new ScalarRepresent<T>(clazz, tag, function));
    }
    
    private class ScalarRepresent <T> implements Represent
    {
    	private Class<T> clazz;
    	private Tag tag;
		private Function<T,String> function;

		public ScalarRepresent(Class<T> clazz, Tag tag, Function<T,String> scalarFunction)
		{
			this.clazz = clazz;
    		this.tag = tag;
    		this.function = scalarFunction;
		}
    	
    	@Override
    	public Node representData(Object data)
    	{
    		return representScalar(tag, function.apply(clazz.cast(data)));
    	}
    }
}

