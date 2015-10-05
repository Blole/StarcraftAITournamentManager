package common.yaml;

import java.util.HashMap;
import java.util.function.Function;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

public class ScalarConstructor extends Constructor
{
	private final ClassTypeConstructor classTypeConstructor = new ClassTypeConstructor();
	
	
	
	public ScalarConstructor()
	{
		this.yamlClassConstructors.put(NodeId.scalar, classTypeConstructor);
	}
	
	
	
	public <T> void addScalarConstructor(Tag tag, Function<String,T> function)
	{
		yamlConstructors.put(tag, new TagConstructor<T>(function));
	}
	public <T> void addScalarConstructor(Class<T> clazz, Function<String,T> function)
	{
		classTypeConstructor.map.put(clazz, function);
	}
	
	
	
	private class TagConstructor <T> extends AbstractConstruct
	{
		private final Function<String,T> function;
		
		TagConstructor(Function<String,T> function)
		{
			this.function = function;
		}
		
		@Override
		public T construct(Node node)
		{
			String string = (String) constructScalar((ScalarNode)node);
			return function.apply(string);
		}
	}
	
	public class ClassTypeConstructor extends ConstructScalar
	{
		private final HashMap<Class<?>, Function<String,? extends Object>> map = new HashMap<>();
		
		@Override
		public Object construct(Node node)
		{
        	Function<String, ? extends Object> function = map.get(node.getType());
        	if (function != null)
        	{
            	String string = (String) constructScalar((ScalarNode)node);
        		return function.apply(string);
        	}
        	else
        		return super.construct(node);
		}
	}
}
