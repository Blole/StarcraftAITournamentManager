package server;

import java.io.Serializable;
import java.util.List;

import common.Bot;
import common.Environment;
import common.Map;
import common.TargetFile;

public class ServerEnvironment extends Environment implements Serializable
{
	private static final long serialVersionUID = -6040955128596907730L;
	
	public int port = 1099;
	public String serverUrlPath = null;
	public boolean killClientsOnExit = false;
	
	public List<TargetFile> clientFiles = null;
	
	public List<Bot> bots;
	public List<Map> maps;

	public String detailedResults;
	public String clearResults;
	public String resumeTournament;
}
