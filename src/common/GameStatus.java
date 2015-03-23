package common;

import java.io.Serializable;

public enum GameStatus implements Comparable<GameStatus>, Serializable 
{
	WAITING,
	READY,
	RUNNING,
	WAITINGTOVERIFY,
	DONE,
}
