package common;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Environment
{
	private LinkedHashMap<String, ? extends File> shortcuts = null;
	
	public String lookup(String s)
	{
		for (HashMap.Entry<String, ? extends File> e : shortcuts.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue().getPath();
			s = s.replace(key, val);
			//TODO: to allow for case insensitive shorcuts, fix this.
			//the following fails when inserting '\'
			//s = Pattern.compile(key, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(val);
		}
		return s;
	}
	
	public MyFile lookupFile(String path)
	{
		return new MyFile(lookup(path));
	}
}
