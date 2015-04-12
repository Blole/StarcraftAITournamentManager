package common;

import java.io.Serializable;

import org.yaml.snakeyaml.TypeDescription;


public class Game implements Serializable
{
	private static final long serialVersionUID = 7845211498799719118L;
	public static final TypeDescription typeDescription = new TypeDescription(Game.class, "!game");
	static
	{
		typeDescription.putListPropertyType("bots", Bot.class);
	}
	
	
	
	public final int id;
	public final int round;
	public final Map map;
	public final Bot[] bots;
	public GameResult results = null;
	
	
	public Game() //dummy constructor for yaml
	{
		id = 0;
		round = 0;
		map = null;
		bots = null;
	}
	
	public Game(int id, int round, Map map, Bot... bots)
	{
		this.id = id;
		this.round = round;
		this.map = map;
		this.bots = bots;
	}

	public String getReplayString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("%05d_", id));
		
		for (int i=0; i<bots.length; i++)
			sb.append(bots[i].name+"-");
		
		sb.setLength(sb.length()-1);
		sb.append(".rep");
		
		return sb.toString();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (Bot bot : bots)
			sb.append(bot.name + " vs. ");
		sb.setLength(sb.length()-5);
		
		return String.format("{Game %d/%d: %s}", id, round, sb.toString());
	}
}
