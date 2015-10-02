package common.status;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

public class Done extends GameStatusFile implements Serializable
{
	private static final long serialVersionUID = -5345577906937910410L;
	public static final TypeDescription typeDescription = new TypeDescription(Done.class, "!done");
	
	
	
	public Object common;
	public Object individual;
	
	public Done()
	{
		common = null;
		individual = null;
	}
}
