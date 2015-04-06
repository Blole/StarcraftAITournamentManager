package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import server.ServerEnvironment;

import common.yaml.MyConstructor;

public class Bot implements Serializable
{
	private static final long serialVersionUID = 2690734629985126222L;
	public static final TypeDescription typeDescription = new TypeDescription(Bot.class, "!bot");
	static
	{
		typeDescription.putListPropertyType("extraFiles", TargetFile.class);
	}
	
	
	
	public final String name;
	public final Race race;
	public final BotExecutableType type;
	public final BwapiVersion bwapiVersion;
	public final List<TargetFile> extraFiles;
	
	private Bot() //dummy constructor for yaml
	{
		name = null;
		race = null;
		type = null;
		bwapiVersion = null;
		extraFiles = null;
	}
	
	public static Bot load(ServerEnvironment env, String name) throws IOException
	{
		return load(env.botDir, name);
	}
	
	public static Bot load(File botDir, String name) throws IOException
	{
		File thisBotDir = new MyFile(botDir, name+"/");
		if (!thisBotDir.exists())
			throw new FileNotFoundException("Bot directory '"+thisBotDir+"' does not exist");
		Yaml yaml = new Yaml(new MyConstructor());
		String yamlData = FileUtils.readFileToString(new File(thisBotDir, "bot.yaml"));
		return yaml.loadAs(yamlData, Bot.class);
	}
	
	
	
	@Override
	public String toString()
	{
		return String.format("{Bot:%s}", name);
	}
	
	public MyFile getDir(ServerEnvironment env)
	{
		return new MyFile(env.botDir, name);
	}

	public MyFile getReadDir(ServerEnvironment env)
	{
		return new MyFile(getDir(env), "read");
	}

	public MyFile getWriteDir(ServerEnvironment env)
	{
		return new MyFile(getDir(env), "write");
	}
}
