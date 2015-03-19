package client;

import java.io.*;

import objects.BWAPISettings;

public class ClientSettings
{	
	public String			ClientStarcraftDir;
	public String			ClientChaoslauncherDir;
	public String			TournamentModuleFilename;
	
	public String			ServerAddress;
	public String 			DefaultBWAPISettingsFileName;
	
	public BWAPISettings	bwapi = new BWAPISettings();

	private static final ClientSettings INSTANCE = new ClientSettings();
	
	private ClientSettings()
	{
		
	}
	
	public static ClientSettings Instance() 
	{
        return INSTANCE;
    }
	
	public void parseSettingsFile(String filename)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				
				if (line.startsWith("#") || line.length() == 0)
				{
					continue;
				}
				
				parseLine(line);
			}
			
			br.close();
		}
		catch (Exception e)
		{
			System.err.println("Error parsing settings file, exiting\n");
			e.printStackTrace();
			System.exit(-1);
		}
		
		bwapi.loadFromFile(DefaultBWAPISettingsFileName);
	}
	
	private void parseLine(String line) throws Exception
	{
		String[] keyvalue = line.split("[ \t]+", 2);
		if (keyvalue.length != 2)
			throw new Exception("unable to split line '"+line+"' into '<key> <value>'");
		String key = keyvalue[0];
		String value = keyvalue[1];
		
		boolean unknownKey = false;
		
		switch (key.toLowerCase())
		{
		case "clientchaoslauncherdir":
			ClientChaoslauncherDir = value;
			break;
		case "clientstarcraftdir":
			ClientStarcraftDir = value;
			break;
		case "serveraddress":
			ServerAddress = value;
			break;
		case "tournamentmodule":
			TournamentModuleFilename = value;
			break;
		case "defaultbwapisettings":
			DefaultBWAPISettingsFileName = value;
			break;
		default:
			unknownKey = true;
			break;
		}
		
		if (unknownKey)
			System.err.printf("%-22s = %s ignoring unkown key '%s'\n", key, value, key);
		else
			System.out.printf("%-22s = %s\n", key, value);
	}

}