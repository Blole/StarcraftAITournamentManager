package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.TypeDescription;

import common.MyFile;
import common.RequiredFile;

public class ClientEnvironment implements Serializable
{
	private static final long serialVersionUID = -6040955128596907730L;
	public static final TypeDescription typeDescription = new TypeDescription(ClientEnvironment.class);
	
	
	
	public String serverUrl = null;
	public double starcraftStartingTimeout = 10;
	public double matchStartingTimeout = 60;
	public double matchEndingTimeout = 60;
	private LinkedHashMap<String, ? extends MyFile> shortcuts = null;
	public RequiredFile starcraftDir;
	
	
	
	public MyFile lookupFile(String path)
	{
		for (HashMap.Entry<String, ? extends MyFile> e : shortcuts.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue().getPath();
			path = path.replace(key, val);
			//TODO: to allow for case insensitive shorcuts, fix this.
			//the following fails when inserting '\'
			//s = Pattern.compile(key, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(val);
		}
		return new MyFile(path);
	}
}
