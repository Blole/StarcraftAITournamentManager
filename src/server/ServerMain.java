package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import common.Environment;
import common.RunnableWithShutdownHook;


public class ServerMain
{
	public static void main(String[] args) throws RemoteException, FileNotFoundException
	{
		if (args.length < 1)
		{
			System.err.println("usage: java -jar server.jar SETTINGS.YAML");
			System.exit(-1);
		}
		else
		{
			ServerEnvironment env = Environment.load(new File(args[0]), ServerEnvironment.class);
			
			LocateRegistry.createRegistry(env.port);
			
			Server server = new Server(env);
			RunnableWithShutdownHook.addShutdownHook(server);
			server.run();
			
			/*if (System.getSecurityManager() == null) {
				System.setSecurityManager(new SecurityManager());
				System.out.println("Security manager installed.");
			} else {
				System.out.println("Security manager already exists.");
			}*/
		}
	}
}
