package common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import common.Bot;
import common.Game;
import common.GameStorage;
import common.Map;

import server.Server;

public class GameParser
{
	private static GameStorage games;
	private static ArrayList<Bot> bots;
	private static ArrayList<Map> maps;

	public static GameStorage getGames(ArrayList<Bot> bots, ArrayList<Map> maps)
	{
		GameParser.bots = bots;
		GameParser.maps = maps;
		try
		{
			parse();
		}
		catch (Exception e)
		{
			System.err.println("Couldn't load games file list\n");
			e.printStackTrace();
			System.exit(-1);
		}
		
		return games;
	}

	private static void parse() throws NumberFormatException, Exception
	{
		games = new GameStorage();
		try
		{
			File gameslist = Server.Instance().env.lookupFile("$GamesListFile");
			if (gameslist.exists())
			{
				BufferedReader br = new BufferedReader(new FileReader(gameslist));
				parseGames(br);
				br.close();
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Could not read settings file");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.out.println("IOException while reading settings.ini");
			e.printStackTrace();
		}
	}
	
	private static void parseGames(BufferedReader br) throws NumberFormatException, Exception
	{
		String line;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			if (!line.startsWith("#") && line.length() > 0)
			{
				String[] args = line.split("\\s+");
				Game newGame = new Game(Integer.parseInt(args[0]), Integer.parseInt(args[1]), findBot(args[2]), findBot(args[3]), findMap(args[4]));
				games.addGame(newGame, newGame.getRound());
			}
		}
	}
	
	private static Bot findBot(String name) throws Exception
	{
		for(Bot bot : bots)
		{
			if(bot.name.equals(name))
			{
				return bot;
			}
		}
		
		throw new Exception("Bot '" + name + "' not found!");
	}
	
	private static Map findMap(String name) throws Exception
	{
		for (Map map : maps)
		{
			if (map.name.equals(name))
			{
				return map;
			}
		}
		
		throw new Exception("Map '" + name + "' not found!");
	}
}
