package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import common.file.CopyFile;
import common.file.MyFile;
import common.yaml.MyConstructor;

public class Bot implements Serializable
{
	private static final long serialVersionUID = 2690734629985126222L;
	public static final TypeDescription typeDescription = new TypeDescription(Bot.class, "!bot");
	static
	{
		typeDescription.putListPropertyType("copyFiles", CopyFile.class);
		typeDescription.putListPropertyType("injectoryArguments", String.class);
		typeDescription.putMapPropertyType("environmentVariables", String.class, String.class);
	}
	
	
	
	public final String name;
	public final String nick;
	public final Race race;
	public final BotExecutableType type;
	public final BwapiVersion bwapiVersion;
	public final List<CopyFile> copyFiles;
	public final List<String> injectoryArguments;
	public final LinkedHashMap<String,String> environmentVariables;
	
	private Bot(String name, BotLoadable o)
	{
		this.name = name;
		this.nick = o.nick;
		this.race = o.race;
		this.type = o.type;
		this.bwapiVersion = o.bwapiVersion;
		this.copyFiles = o.copyFiles!=null ? o.copyFiles : new ArrayList<CopyFile>();
		this.injectoryArguments = o.injectoryArguments!=null ? o.injectoryArguments : new ArrayList<>();
		this.environmentVariables = o.environmentVariables!=null ? o.environmentVariables : new LinkedHashMap<String,String>();
	}
	
	private static class BotLoadable
	{
		public String nick;
		public Race race;
		public BotExecutableType type;
		public BwapiVersion bwapiVersion;
		public List<CopyFile> copyFiles;
		public List<String> injectoryArguments;
		public LinkedHashMap<String,String> environmentVariables;
	}

	
	public static Bot load(CommonEnvironment env, String name) throws IOException
	{
		MyFile dir = new MyFile(env.botDir(), name);
		if (!dir.exists())
			throw new FileNotFoundException("bot directory '"+dir+"' does not exist");
		Yaml yaml = new Yaml(new MyConstructor(env));
		String yamlData = FileUtils.readFileToString(new File(dir, "bot.yaml"));
		BotLoadable o = yaml.loadAs(yamlData, BotLoadable.class);
		return new Bot(name, o);
	}
	
	
	
	@Override
	public String toString()
	{
		return String.format("{Bot %s}", name);
	}
	
	public MyFile getDir(CommonEnvironment env)
	{
		return new MyFile(env.botDir(), name);
	}

	public MyFile getReadDir(CommonEnvironment env)
	{
		return new MyFile(getDir(env), "read");
	}

	public MyFile getWriteDir(CommonEnvironment env)
	{
		return new MyFile(getDir(env), "write");
	}

	public MyFile getDll(CommonEnvironment env)
	{
		return new MyFile(getDir(env), name+".dll");
	}
}
