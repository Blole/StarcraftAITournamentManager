package client;

import java.io.File;

import common.Environment;


public class ClientMain
{
	public static void main(String[] args) throws Exception
	{
		if (!System.getProperty("os.name").contains("Windows"))
		{
			System.err.println("Sorry, Client can only be run on Windows.");
		}
		else if (args.length < 1)
		{
			System.err.println("usage: java -jar client.jar SETTINGS.YAML");
			System.exit(-1);
		}
		else
		{
			Environment env = new Environment(new File(args[0]));
			Client client = new Client(env);
			ClientListenerThread listener = new ClientListenerThread(client);
			client.setListener(listener);
			
			client.start();
		}
	}
}