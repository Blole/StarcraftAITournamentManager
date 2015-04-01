package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Environment
{
	//public static void main(String[] args) throws FileNotFoundException
	//{
		//Environment env = Environment.load(new File(args[0]), Environment.class);
		//System.out.println(env.yaml.dump(env.get("common_files")));
		//System.out.println(env.yaml.dump(env.get("bwapi_versions")));
		//System.out.println(env.yaml.dump(env.get("bots")));
		//System.out.println(env.yaml.dump(env.get("maps")));
	//}

	private LinkedHashMap<String, ? extends File> files = null;
	
	public String lookup(String s)
	{
		for (HashMap.Entry<String, ? extends File> e : files.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue().getPath();
			s = s.replace(key, val);
			//TODO: the following fails when inserting \
			//s = Pattern.compile(key, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(val);
		}
		return s;
	}
	
	public MyFile lookupFile(String path)
	{
		return new MyFile(lookup(path));
	}
	
	private static final ArrayList<ImmutablePair<String, Class<? extends Object>>> aliases = new ArrayList<>();
	private static final Constructor constructor = new Constructor();
	static
	{
		aliases.add(new ImmutablePair<>("!bot",			Bot.class));
		aliases.add(new ImmutablePair<>("!map",			Map.class));
		aliases.add(new ImmutablePair<>("!file",		File.class));
		aliases.add(new ImmutablePair<>("!target",		TargetFile.class));
		aliases.add(new ImmutablePair<>("!required",	RequiredFile.class));
		
		for (Pair<String, Class<? extends Object>> alias : aliases)
			constructor.addTypeDescription(new TypeDescription(alias.getRight(), alias.getLeft()));

	}

	public static <T extends Environment> T load(File file, Class<T> clazz) throws FileNotFoundException
	{
		try
		{
			Yaml yaml = new Yaml(constructor);
			
			Reader in = new BufferedReader(new FileReader(file));
			T env = yaml.loadAs(in, clazz);
			return env;
		}
		catch (YAMLException e)
		{
			System.err.printf("error reading yaml file '%s'\n", file.getAbsolutePath());
			e.printStackTrace();
			//e.getCause().getCause().printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
