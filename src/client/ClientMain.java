package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;

import common.Environment;
import common.RunnableWithShutdownHook;


public class ClientMain
{
	public static void main(String[] args) throws RemoteException, FileNotFoundException
	{
		if (args.length < 1)
		{
			System.err.println("usage: java -jar client.jar SETTINGS.YAML");
			System.exit(-1);
		}
		else
		{
			ClientEnvironment env = Environment.load(new File(args[0]), ClientEnvironment.class);
			
			
			
			Client client = new Client(env);
			RunnableWithShutdownHook.addShutdownHook(client);
			client.run();
		}
	}
}