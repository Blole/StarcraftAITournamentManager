package common.yaml;

import java.util.LinkedHashSet;
import java.util.Set;

import org.yaml.snakeyaml.introspector.FieldProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;

import common.Bot;
import common.Game;
import common.Map;

public class GameRepresenter extends ScalarRepresenter
{
    public GameRepresenter()
    {
		addScalarRepresentation(Map.class, Tag.STR, m -> m.path);
		addScalarRepresentation(Bot.class, Bot.typeDescription.getTag(), b -> b.name);
        this.representers.put(Game.class, new RepresentGame());
    }
    
    private class RepresentGame implements Represent
    {
        @Override
		public Node representData(Object data) {
    		Game game = (Game) data;
    		//Set<Property> properties = getProperties(Game.class);
    		Set<Property> properties = new LinkedHashSet<>();
    		try
			{
				properties.add(new FieldProperty(Game.class.getField("id")));
				properties.add(new FieldProperty(Game.class.getField("round")));
				properties.add(new FieldProperty(Game.class.getField("map")));
				properties.add(new FieldProperty(Game.class.getField("bots")));
				if (game.special != null)
					properties.add(new FieldProperty(Game.class.getField("special")));
				if (game.results != null)
					properties.add(new FieldProperty(Game.class.getField("results")));
                return representJavaBean(properties, game);
			}
    		catch (NoSuchFieldException | SecurityException e)
			{
				throw new RuntimeException("hmm, seems I've changed the name of some field", e);
			}
        }
    }
}