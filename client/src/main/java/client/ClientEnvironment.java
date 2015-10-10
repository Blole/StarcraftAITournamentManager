package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.TypeDescription;

import common.CommonEnvironment;
import common.file.MyFile;
import common.file.RequiredFile;

public class ClientEnvironment extends CommonEnvironment implements Serializable
{
	private static final long serialVersionUID = -6040955128596907730L;
	public static final TypeDescription typeDescription = new TypeDescription(ClientEnvironment.class);
	static
	{
		typeDescription.putMapPropertyType("shortcuts", String.class, MyFile.class);
	}
	
	
	
	public String serverUrl = null;
	
	public boolean killOtherStarcraftProcessesOnStartup = false;
	public boolean addStarcraftFirewallExceptionOnStartup = false;
	
	public double serverAlivePollPeriod = 10;
	
	public int maxStarcraftInstances = 1;
	public double matchStartingTimeout = 60;
	public double matchAlivePollPeriod = 5;
	public double starcraftClosingTimeout = 10;
	
	public RequiredFile starcraftDir;
	public String gamestatusFileName;
	public LinkedHashMap<String, ? extends MyFile> shortcuts = null;
	
	public MyFile lookupFile(String path)
	{
		for (HashMap.Entry<String, ? extends MyFile> e : shortcuts.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue().getPath();
			path = path.replace(key, val);
			//TODO: to allow for case insensitive shorcut names, fix this.
			//the following fails when inserting '\'
			//s = Pattern.compile(key, Pattern.LITERAL | Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(val);
		}
		return new MyFile(path);
	}
}
