package objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.Yaml;

public class Environment
{
	private static Yaml yaml = new Yaml();
	public static void main(String[] args)
	{
		File file = new File(args[0]);
		Environment env = new Environment(file);
		System.out.println("\n"+yaml.dump(env.data)+"\n\n");
		System.out.println(env.get("bots").toString());
		System.out.println(env.get("maps").toString());
	}

	private LinkedHashMap<Object,Object> data;
	
	@SuppressWarnings("unchecked")
	public Environment(File file)
	{
		try
		{
			data = yaml.loadAs(new BufferedReader(new FileReader(file)), LinkedHashMap.class);
		} catch (FileNotFoundException e)
		{
			System.err.println("error reading yaml file '"+file.getAbsolutePath()+"'");
			e.printStackTrace();
			System.exit(-1);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key)
	{
		try
		{
			return (T) data.get(key);
		}
		catch (ClassCastException e)
		{
	        return null;
	    }
	}
	
	public <T> T get(String key, Class<T> clazz)
	{
		try
		{
			return clazz.cast(data.get(key));
		}
		catch (ClassCastException e)
		{
	        return null;
	    }
	}
	
	public String lookup(String s)
	{
		for (HashMap.Entry<Object, Object> e : data.entrySet())
		{
			if (e.getKey() instanceof String)
			{
				String key = "$"+e.getKey();
				String val = e.getValue().toString();
				s = s.replace(key, val);
				//TODO: the following fails when inserting \
				//s = Pattern.compile(key, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(val);
			}
		}
		return s;
	}
	
	public File lookupFile(String path)
	{
		return new File(lookup(path));
	}
}
