package server;

import java.io.File;

import common.Environment;


public class ServerMain
{

	public static String serverSettingsFile;
	
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			System.err.println("usage: java -jar server.jar SETTINGS.YAML");
			System.exit(-1);
		}
		else
		{
			Environment env = new Environment(new File(args[0]));
			if (validEnvironment(env))
			{
				Server server = new Server(env);
				server.start();
			}
		}
	}
	
	private static boolean validEnvironment(Environment env)
	{
		boolean valid = true;
		
		// check if all setting variables are valid
		/*if (BotVector.size() <= 1) 		{ System.err.println("ServerSettings: Must have at least 2 bots in settings file"); valid = false; }
		if (MapVector.size() <= 0)		{ System.err.println("ServerSettings: Must have at least 1 map in settings file"); valid = false; }
		if (ServerDir == null)			{ System.err.println("ServerSettings: ServerDir not specified in settings file"); valid = false; }
		if (GamesListFile == null)		{ System.err.println("ServerSettings: GamesListFile not specified in settings file"); valid = false; }
		if (ResultsFile == null)		{ System.err.println("ServerSettings: ResultsFile must be specified in settings file"); valid = false; }
		if (env.get("ServerPort", Integer.class) == -1)			{ System.err.println("ServerSettings: ServerPort must be specified as an integer in settings file"); valid = false; }
		
		// check if all required files are present
		if (!new File(ServerReplayDir).exists()) 	{ System.err.println("ServerSettings: Replay Dir (" + ServerReplayDir + ") does not exist"); valid = false; }
		if (!new File(ServerBotDir).exists()) 		{ System.err.println("ServerSettings: Bot Dir (" + ServerBotDir + ") does not exist"); valid = false; }
		if (!new File(ServerRequiredDir).exists()) 	{ System.err.println("ServerSettings: Required Files Dir (" + ServerRequiredDir + ") does not exist"); valid = false; }
		
		// check all bot directories
		for (Bot b : BotVector)
		{
			String botDir 		= ServerBotDir + b.name + "/";
			String botAIDir 	= botDir + "AI/";
			String botDLLFile	= botAIDir + b.name + ".dll";
			String botWriteDir 	= botDir + "write/";
			String botReadDir 	= botDir + "read/";
			String proxyScript	= botAIDir + "run_proxy.bat";
			String botBWAPIReq  = ServerRequiredDir + "Required_" + b.bwapiVersion + ".zip";
			
			// Check if all the bot files exist
			if (!new File(botDir).exists()) 		{ System.err.println("Bot Error: " + b.name + " bot directory " + botDir + " does not exist."); valid = false; }
			if (!new File(botAIDir).exists()) 		{ System.err.println("Bot Error: " + b.name + " bot AI directory " + botAIDir + " does not exist."); valid = false; }
			if (!new File(botDLLFile).exists()) 	{ System.err.println("Bot Error: " + b.name + " bot dll file " + botDLLFile + " does not exist."); valid = false; }
			if (!new File(botWriteDir).exists()) 	{ System.err.println("Bot Error: " + b.name + " bot write directory " + botWriteDir + " does not exist."); valid = false; }
			if (!new File(botReadDir).exists()) 	{ System.err.println("Bot Error: " + b.name + " bot read directory " + botReadDir + " does not exist."); valid = false; }
			if (!new File(botBWAPIReq).exists()) 	{ System.err.println("Bot Error: " + b.name + " bot required BWAPI files " + botBWAPIReq + " does not exist."); valid = false; }
			
			// Check if the bot is proxy and the proxy bot exists
			if (b.isProxyBot() && !new File(proxyScript).exists())
			{
				System.err.println("Bot Error: " + b.name + " listed as proxy but " + proxyScript + " does not exist.");
				valid = false;
			}
		}
		
		// Check if all the maps exist
		/*for (Map m : MapVector)
		{
			String mapLocation = ServerRequiredDir + "Starcraft/" + m.getMapLocation();
			if (!new File(mapLocation).exists())
			{
				System.err.println("Map Error: " + m.getMapName() + " file does not exist at specified location: " + mapLocation); valid = false;
			}
		}*/
		
		return valid;
	}
}
