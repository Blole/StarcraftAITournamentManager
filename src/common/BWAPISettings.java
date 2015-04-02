package common;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;

public class BWAPISettings implements Serializable, Cloneable
{
	private static final long serialVersionUID = -5086509309385709470L;
	
	/**
	 * Paths and revisions for AI" + newLine);
	 *   - Use commas to specify AI for multiple instances.
	 *   - If there are more instances than the amount of
	 *         DLLs specified, then the last entry is used.
	 *   - Use a colon to forcefully load the revision specified.
	 *   - Example: SomeAI.dll:3400, SecondInstance.dll, ThirdInstance.dll
	 */
	public String ai;
	public String ai_dbg;
	/**
	 * Used only for tournaments Tournaments can only be run in RELEASE mode
	 */
	public String tournament;
	/**
	 * auto_menu = OFF | SINGLE_PLAYER | LAN | BATTLE_NET for replays, just set
	 * the map to the path of the replay file
	 */
	public String auto_menu;
	/**
	 * pause_dbg = ON | OFF This specifies if auto_menu will pause until a
	 * debugger is attached to the process. Only works in DEBUG mode.
	 */
	public String pause_dbg;
	/**
	 * lan_mode = Same as the text that appears in the multiplayer connection
	 * list -> Examples: Direct Cable Connection, Local Area Network (UDP),
	 * Local PC (UDP)
	 */
	public String lan_mode;
	/**
	 * auto_restart = ON | OFF if ON, BWAPI will automate through the end of
	 * match screen and start the next match if OFF, BWAPI will pause at the end
	 * of match screen until you manually click OK, and then BWAPI resume menu
	 * automation and start the next match
	 */
	public String auto_restart;
	/**
	 * map = path to map relative to Starcraft folder, i.e. map =
	 * maps\(2)Boxer.scm leaving this field blank will join a game instead of
	 * creating it The filename(NOT the path) can also contain wildcards,
	 * example: maps\(?)*.sc? A ? is a wildcard for a single character and * is
	 * a wildcard for a string of characters
	 */
	public String map;
	/**
	 * game = name of the game to join" + newLine);
	 * i.e., game = BWAPI" + newLine);
	 * will join the game called \"BWAPI\"" + newLine);
	 * If the game does not exist and the \"map\" entry is not blank, then the game will be created instead" + newLine);
	 * If this entry is blank, then it will follow the rules of the \"map\" entry" + newLine);
	 */
	public String game;
	/**
	 * mapiteration = RANDOM | SEQUENCE type of iteration that will be done on a
	 * map name with a wildcard
	 */
	public String mapiteration;
	/**
	 * race = [Terran | Protoss | Zerg | Random | RandomTP | RandomTZ | RandomPZ]
	 */
	public String race;
	/**
	 * enemy_count = 1-7, for 1v1 games, set enemy_count = 1 only used in single
	 * player games
	 */
	public String enemy_count;
	/**
	 * enemy_race = [Terran | Protoss | Zerg | Random | RandomTP | RandomTZ | RandomPZ]<br/>
	 * Only used in single player games
	 */
	public String enemy_race;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_1;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_2;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_3;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_4;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_5;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_6;
	/**
	 * enemy_race_# = Default -> Values for enemy_race are acceptable, Default
	 * will use the value specified in enemy_race
	 */
	public String enemy_race_7;
	/**
	 * game_type = TOP_VS_BOTTOM | MELEE | FREE_FOR_ALL | ONE_ON_ONE |
	 * USE_MAP_SETTINGS | CAPTURE_THE_FLAG | GREED | SLAUGHTER | SUDDEN_DEATH |
	 * TEAM_MELEE | TEAM_FREE_FOR_ALL | TEAM_CAPTURE_THE_FLAG
	 */
	public String game_type;
	/**
	 * save_replay = path to save replay to Accepts all environment variables
	 * including custom variables. See wiki for more info.
	 */
	public String save_replay;
	/**
	 * # of players to wait for in a network game before starting. This includes
	 * the BWAPI player. The game will start immediately when it is full.
	 */
	public String wait_for_min_players;
	/**
	 * Start immediately when the game has reached # players. This includes the
	 * BWAPI player. The game will start immediately when it is full.
	 */
	public String wait_for_max_players;
	/**
	 * The time in milliseconds (ms) to wait after the game has met the
	 * min_players requirement. The game will start immediately when it is full.
	 */
	public String wait_for_time;
	/**
	 * holiday = ON | OFF -> This will apply special easter eggs to the game
	 * when it comes time for a holiday.
	 */
	public String holiday;
	/** Control the display of startup warnings */
	public String show_warnings;
	/** Used to disable shared memory in the Windows Emulator */
	public String shared_memory;
	/**
	 * windowed = ON | OFF -> This causes BWAPI to enter windowed mode when it
	 * is injected.
	 */
	public String windowed;
	/** Determines the position of the window */
	public String left;
	/** Determines the position of the window */
	public String top;
	/** Determines the width of the client area and not the window itself */
	public String width;
	/** Determines the height of the client area and not the window itself */
	public String height;
	/** Game sound engine = ON | OFF */
	public String sound;
	/** Screenshot format = gif | pcx | tga | bmp */
	public String screenshots;
	/** The Path of the log file */
	public String log_path;
	
	
	
	public BWAPISettings(File file)
	{
		try
		{
			for (String line : Files.readAllLines(file.toPath()))
			{
				line = line.trim().toLowerCase();
				
				if (!line.startsWith(";") && line.contains("="))
				{
					String key = line.substring(0, line.indexOf('=')).trim();
					String value = line.substring(line.indexOf('=') + 1).trim();
					
					switch(key.toLowerCase())
					{
					case "ai":					ai = value; 						break;
					case "ai_dbg":				ai_dbg = value; 					break;
					case "tournament":			tournament = value; 				break;
					case "auto_menu":			auto_menu = value; 					break;
					case "pause_dbg":			pause_dbg = value; 					break;
					case "lan_mode":			lan_mode = value; 					break;
					case "auto_restart":		auto_restart = value; 				break;
					case "map":					map = value; 						break;
					case "game":				game = value; 						break;
					case "mapiteration":		mapiteration = value; 				break;
					case "race":				race = value; 						break;
					case "enemy_count":			enemy_count = value; 				break;
					case "enemy_race":			enemy_race = value; 				break;
					case "enemy_race_1":		enemy_race_1 = value; 				break;
					case "enemy_race_2":		enemy_race_2 = value; 				break;
					case "enemy_race_3":		enemy_race_3 = value; 				break;
					case "enemy_race_4":		enemy_race_4 = value; 				break;
					case "enemy_race_5":		enemy_race_5 = value; 				break;
					case "enemy_race_6":		enemy_race_6 = value; 				break;
					case "enemy_race_7":		enemy_race_7 = value; 				break;
					case "game_type":			game_type = value; 					break;
					case "save_replay":			save_replay = value; 				break;
					case "wait_for_min_players":wait_for_min_players = value;		break;
					case "wait_for_max_players":wait_for_max_players = value;		break;
					case "wait_for_time":		wait_for_time = value; 				break;
					case "holiday":				holiday = value; 					break;
					case "show_warnings":		show_warnings = value; 				break;
					case "shared_memory":		shared_memory = value; 				break;
					case "windowed":			windowed = value; 					break;
					case "left":				left = value; 						break;
					case "top":					top = value; 						break;
					case "width":				width = value; 						break;
					case "height":				height = value; 					break;
					case "sound":				sound = value; 						break;
					case "screenshots":			screenshots = value; 				break;
					case "log_path":			log_path = value; 					break;
					default:
						System.err.printf("unrecocognized key '%s' = '%s' while parsing '%s', ignoring\n", key, value, file);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.printf("error loading BWAPI settings file '%s'\n", file);
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void setGame(Game game, int i)
	{
		ai     = game.bots[i].name;
		ai_dbg = game.bots[i].name;
		race   = game.bots[i].race+"";
        
		tournament = "bwapi-data\\TournamentModule.dll";
		this.game = game.id+"";
		map = i==0 ? game.map.path : "";
		save_replay = "maps/replays/"+game.getReplayString();
	}
	
	
	public String getContentsString()
	{
		String newLine = System.getProperty("line.separator");
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(";BWAPI written by AIIDE Tournament Manager " + newLine);
		
		sb.append("[ai]" + newLine);
		sb.append("; Paths and revisions for AI" + newLine);
		sb.append(";   - Use commas to specify AI for multiple instances." + newLine);
		sb.append(";   - If there are more instances than the amount of " + newLine);
		sb.append(";         DLLs specified, then the last entry is used." + newLine);
		sb.append(";   - Use a colon to forcefully load the revision specified." + newLine);
		sb.append(";   - Example: SomeAI.dll:3400, SecondInstance.dll, ThirdInstance.dll" + newLine);
		sb.append("ai     = bwapi-data\\AI\\" + ai + ".dll" + newLine);
		sb.append("ai_dbg = bwapi-data\\AI\\" + ai_dbg + ".dll" + newLine + newLine);

		sb.append("; Used only for tournaments" + newLine);
		sb.append("; Tournaments can only be run in RELEASE mode" + newLine);
		sb.append("tournament =" + tournament + newLine + newLine);

		sb.append("[auto_menu]" + newLine);
		sb.append("; auto_menu = OFF | SINGLE_PLAYER | LAN | BATTLE_NET" + newLine);
		sb.append("; for replays, just set the map to the path of the replay file" + newLine);
		sb.append("auto_menu = " + auto_menu + newLine + newLine);

		sb.append("; pause_dbg = ON | OFF" + newLine);
		sb.append("; This specifies if auto_menu will pause until a debugger is attached to the process." + newLine);
		sb.append("; Only works in DEBUG mode." + newLine);
		sb.append("pause_dbg = " + pause_dbg + newLine + newLine);

		sb.append("; lan_mode = Same as the text that appears in the multiplayer connection list" + newLine);			// TODO: FINISH
		sb.append(";            Examples: Local Area Network (UDP), Local PC, Direct IP" + newLine);
		sb.append("lan_mode = " + lan_mode + newLine + newLine);

		sb.append("; auto_restart = ON | OFF" + newLine);
		sb.append("; if ON, BWAPI will automate through the end of match screen and start the next match" + newLine);
		sb.append("; if OFF, BWAPI will pause at the end of match screen until you manually click OK," + newLine);
		sb.append("; and then BWAPI resume menu automation and start the next match" + newLine);
		sb.append("auto_restart = " + auto_restart + newLine + newLine);

		sb.append("; map = path to map relative to Starcraft folder, i.e. map = maps\\(2)Boxer.scm" + newLine);
		sb.append("; leaving this field blank will join a game instead of creating it" + newLine);
		sb.append("; The filename(NOT the path) can also contain wildcards, example: maps\\(?)*.sc?" + newLine);
		sb.append("; A ? is a wildcard for a single character and * is a wildcard for a string of characters" + newLine);
		sb.append("map = " + map + newLine + newLine);

		sb.append("; game = name of the game to join" + newLine);
		sb.append(";	i.e., game = BWAPI" + newLine);
		sb.append(";	will join the game called \"BWAPI\"" + newLine);
		sb.append(";	If the game does not exist and the \"map\" entry is not blank, then the game will be created instead" + newLine);
		sb.append(";	If this entry is blank, then it will follow the rules of the \"map\" entry" + newLine);
		sb.append("game =" + game + newLine + newLine);

		sb.append("; mapiteration =  RANDOM | SEQUENCE" + newLine);
		sb.append("; type of iteration that will be done on a map name with a wildcard" + newLine);
		sb.append("mapiteration = " + mapiteration + newLine + newLine);

		sb.append("; race = Terran | Protoss | Zerg | Random" + newLine);
		sb.append("race = " + race + newLine + newLine);

		sb.append("; enemy_count = 1-7, for 1v1 games, set enemy_count = 1" + newLine);
		sb.append("; only used in single player games" + newLine);
		sb.append("enemy_count = " + enemy_count + newLine + newLine);

		sb.append("; enemy_race = Terran | Protoss | Zerg | Random | RandomTP | RandomTZ | RandomPZ" + newLine);
		sb.append("; only used in single player games" + newLine);
		sb.append("enemy_race = " + enemy_race + newLine + newLine);

		sb.append("; enemy_race_# = Default" + newLine);
		sb.append("; Values for enemy_race are acceptable, Default will use the value specified in enemy_race" + newLine);
		sb.append("enemy_race_1 = " + enemy_race_1 + newLine);
		sb.append("enemy_race_2 = " + enemy_race_2 + newLine);
		sb.append("enemy_race_3 = " + enemy_race_3 + newLine);
		sb.append("enemy_race_4 = " + enemy_race_4 + newLine);
		sb.append("enemy_race_5 = " + enemy_race_5 + newLine);
		sb.append("enemy_race_6 = " + enemy_race_6 + newLine);
		sb.append("enemy_race_7 = " + enemy_race_7 + newLine);

		sb.append(";game_type = TOP_VS_BOTTOM | MELEE | FREE_FOR_ALL | ONE_ON_ONE | USE_MAP_SETTINGS | CAPTURE_THE_FLAG" + newLine);
		sb.append(";           | GREED | SLAUGHTER | SUDDEN_DEATH | TEAM_MELEE | TEAM_FREE_FOR_ALL | TEAM_CAPTURE_THE_FLAG" + newLine);
		sb.append("game_type = " + game_type + newLine + newLine);

		sb.append("; save_replay = path to save replay to" + newLine);
		sb.append("; Accepts all environment variables including custom variables. See wiki for more info." + newLine);
		sb.append("save_replay = " + save_replay + newLine + newLine);

		sb.append("; wait_for_min_players = #" + newLine);
		sb.append("; # of players to wait for in a network game before starting." + newLine);
		sb.append("; This includes the BWAPI player. The game will start immediately when it is full." + newLine);
		sb.append("wait_for_min_players = " + wait_for_min_players + newLine + newLine);

		sb.append("; wait_for_max_players = #" + newLine);
		sb.append("; Start immediately when the game has reached # players." + newLine);
		sb.append("; This includes the BWAPI player. The game will start immediately when it is full." + newLine);
		sb.append("wait_for_max_players = " + wait_for_max_players + newLine + newLine);

		sb.append("; wait_for_time = #" + newLine);
		sb.append("; The time in milliseconds (ms) to wait after the game has met the min_players requirement." + newLine);
		sb.append("; The game will start immediately when it is full." + newLine);
		sb.append("wait_for_time = " + wait_for_time + newLine + newLine);

		sb.append("[config]" + newLine);
		sb.append("; holiday = ON | OFF" + newLine);
		sb.append("; This will apply special easter eggs to the game when it comes time for a holiday." + newLine);
		sb.append("holiday = " + holiday + newLine + newLine);

		sb.append("; show_warnings = YES | NO" + newLine);
		sb.append("; Setting this to NO will disable startup Message Boxes, but also disable options that" + newLine);
		sb.append("; assist in revision choice decisions." + newLine);
		sb.append("show_warnings = " + show_warnings + newLine + newLine);

		sb.append("; shared_memory = ON | OFF" + newLine);
		sb.append("; This is specifically used to disable shared memory (BWAPI Server) in the Windows Emulator \"WINE\"" + newLine);
		sb.append("; Setting this to OFF will disable the BWAPI Server, default is ON" + newLine);
		sb.append("shared_memory = " + shared_memory + newLine + newLine);

		sb.append("[window]" + newLine);
		sb.append("; These values are saved automatically when you move, resize, or toggle windowed mode" + newLine);

		sb.append("; windowed = ON | OFF" + newLine);
		sb.append("; This causes BWAPI to enter windowed mode when it is injected." + newLine);
		sb.append("windowed = " + windowed + newLine + newLine);

		sb.append("; left, top" + newLine);
		sb.append("; Determines the position of the window" + newLine);
		sb.append("left = " + left + newLine + newLine);
		sb.append("top  = " + top + newLine + newLine);

		sb.append("; width, height" + newLine);
		sb.append("; Determines the width and height of the client area and not the window itself" + newLine);
		sb.append("width  = " + width + newLine + newLine);
		sb.append("height = " + height + newLine + newLine);

		sb.append("[starcraft]" + newLine);
		sb.append("; Game sound engine = ON | OFF" + newLine);
		sb.append("sound = " + sound + "" + newLine);
		sb.append("; Screenshot format = gif | pcx | tga | bmp" + newLine);
		sb.append("screenshots = " + screenshots + newLine + newLine);

		sb.append("[paths]" + newLine);
		sb.append("log_path = " + log_path + "" + newLine);
		
		return sb.toString();
	}
	
	
	
	@Override
	public String toString()
	{
		return "{BWAPISettings}";
	}
		
	@Override
	public BWAPISettings clone()
	{
		try
		{
			return (BWAPISettings) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException("shouldn't happen", e);
		}
	}
}
