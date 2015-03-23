package objects;

import java.util.Iterator;
import java.util.Vector;

import utility.ResultsParser;

public class GameStorage 
{
	private Vector<Game> allGames = new Vector<Game>();
	private int nextGameIndex = 0;
	
	public GameStorage()
	{
	}
	
	public void addGame(Game game, int round)
	{
		allGames.add(game);
	}
	
	public void removePlayedGames(ResultsParser rp)
	{
		Iterator<Game> it = allGames.iterator();
		while (it.hasNext()) 
		{
		    if (rp.hasGameResult(it.next().getGameID())) 
		    {
		        it.remove();
		    }
		}
	}
	
	public boolean hasMoreGames()
	{
		return nextGameIndex < allGames.size();
	}
	
	public Game peekNextGame()
	{
		return allGames.get(nextGameIndex);
	}
	
	public Game getNextGame()
	{
		return allGames.get(nextGameIndex++);
	}
	
	public void advanceToNextGame()
	{
		nextGameIndex++;
	}

	public Game lookupGame(int gameID, int round) 
	{
		for(Game g : allGames)
		{
			if(g.getGameID() == gameID)
			{
				return g;
			}
		}
		
		return null;
	}
}
