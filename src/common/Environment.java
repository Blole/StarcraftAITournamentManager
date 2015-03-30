package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Environment
{
	public static void main(String[] args)
	{
		File envFile = new File(args[0]);
		Environment env = new Environment(envFile);
		//System.out.println(env.yaml.dump(env.get("common_files")));
		//System.out.println(env.yaml.dump(env.get("bwapi_versions")));
		System.out.println(env.yaml.dump(env.get("bots")));
		//System.out.println(env.yaml.dump(env.get("maps")));
	}

	private Map<String,Object> data;
	private Yaml yaml;
	
	@SuppressWarnings("unchecked")
	public Environment(File file)
	{
		try
		{
			Reader reader = new BufferedReader(new FileReader(file));
			yaml = new Yaml();
			data = (Map<String,Object>) yaml.load(reader);
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
		for (HashMap.Entry<String, Object> e : data.entrySet())
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
	
	public MyFile lookupFile(String path)
	{
		return new MyFile(lookup(path));
	}
}
