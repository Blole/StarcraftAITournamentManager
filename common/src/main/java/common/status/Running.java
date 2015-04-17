package common.status;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;

public class Running extends GameStatusFile implements Serializable
{
	private static final long serialVersionUID = 1345921565509098963L;
	public static final TypeDescription typeDescription = new TypeDescription(Running.class, "!running");
	
	public Running() {}
	public Running(String dummy) {}
	
	@Override
	public GameStatus getStatus()
	{
		return GameStatus.Running;
	}
}
