package common;

import java.io.Serializable;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.TypeDescription;


public class Game implements Serializable
{
	private static final long serialVersionUID = 7845211498799719118L;
	public static final TypeDescription typeDescription = new TypeDescription(Game.class, "!game");
	static
	{
		typeDescription.putListPropertyType("bots", Bot.class);
	}
	
	
	
	public String id = null;
	public final int round;
	public final Map map;
	public final GameType type;
	public final Bot[] bots;
	
	
	public Game() //dummy constructor for yaml
	{
		round = 0;
		map = null;
		type = null;
		bots = null;
	}
	
	public Game(String id, int round, Map map, GameType type, Bot... bots)
	{
		this.id = id;
		this.round = round;
		this.map = map;
		this.type = type;
		this.bots = bots;
	}
	
	@Override
	public String toString()
	{
		return String.format("{Game %s}", id);
	}
	
	public String botsString()
	{
		if (bots == null)
			return "";
		else
			return StringUtils.join(Stream.of(bots).map(b->b.name).iterator(), " vs. ");
	}
}
