package server;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import common.Game;
import common.GameResults;
import common.file.MyFile;
import common.file.PackedFile;
import common.yaml.MyRepresenter;

public class ServerGame extends Game
{
	private static final long serialVersionUID = -4027336346001349463L;
	
	
	public enum ServerGameState {QUEUED, RUNNING, DONE};
	private ServerEnvironment env;
	private ServerGameState state;
	private MyFile yamlFile;
	private Games games;
	
	public ServerGame(ServerEnvironment env, MyFile yamlFile, Games games)
	{
		this.env = env;
		this.state = ServerGameState.QUEUED;
		this.yamlFile = yamlFile;
		this.games = games;
	}
	
	public void reportResults(GameResults results, PackedFile replay) throws IOException
	{
		FileUtils.forceMkdir(resultsDir());
		games.runningMatches.remove(this);
		replay.writeTo(resultsDir());
		try
		{
			Yaml yaml = new Yaml(new MyRepresenter());
			String resultsText = yaml.dump(results);
			FileUtils.writeStringToFile(resultsFile(), resultsText);
			FileUtils.moveFileToDirectory(yamlFile, resultsDir(), false);
		}
		catch (YAMLException | IOException e)
		{
			System.err.println("error writing to results file");
			e.printStackTrace();
		}
		state = ServerGameState.DONE;
		games.checkAndNotify();
	}

	private MyFile resultsDir()
	{
		return new MyFile(env.gameResultsDir, yamlFile.getNameWithoutExtension());
	}
	private MyFile resultsFile()
	{
		return new MyFile(resultsDir(), "results.yaml");
	}
	public ServerGameState state()
	{
		return state;
	}
}
