package common;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class RMIHelper {
	/**
	 * Function will try and create an RMI Registry
	 * If it is able to create, it means, RMI Registry is not run
	 * Hence, we instruct the user to Run the RMI Registry.
	 *  @param port: Port to run RMI registry
	 */
	public static void makeSureRegistryIsStarted(int port) {
		try {
			LocateRegistry.createRegistry(port);
			System.err.println("error: rmiregistry is not running. please run rmiregistry first");
			System.exit(1);
		} catch (RemoteException e) {
		}
	}

	/**
	 * The same as {@code Naming.lookup(hostAddress)} except that it prints a
	 * nice error message to System.out and returns null instead of casting
	 * exceptions.
	 * 
	 * @param hostAddress
	 *            example: {@code //127.0.0.1/test}
	 * @return the found stub, or null if there was an error
	 */
	public static Remote lookup(String hostAddress) {
		try {
			return Naming.lookup(hostAddress);
		} catch (MalformedURLException e) {
			System.err.println(e.getLocalizedMessage());
		} catch (RemoteException e) {
			System.err.println(e.getLocalizedMessage());
		} catch (NotBoundException e) {
			System.err.printf(
					"rmiregistry is running on the remote host (%s),\n"
							+ "   but '%s' is not bound.\n", hostAddress,
					e.getMessage());
		}
		return null;
	}

	public static Remote lookupAndWaitForRemoteToStartIfNecessary(String name,
			int retryPeriod_ms) {
		int attempts = 0;
		while (true) {
			try {
				return Naming.lookup(name);
			} catch (RemoteException | NotBoundException e) {
				if (attempts == 0) {
					System.err.printf(
							"error while connecting to remote host '%s':\n",
							name);
					if (e instanceof NotBoundException)
						System.err
								.printf("rmiregistry is running, but '%s' is not bound.\n",
										e.getLocalizedMessage());
					else
						System.err.println(e.getMessage());
					System.err.printf("\nretrying every %d seconds\n",
							retryPeriod_ms / 1000);
				} else {
					System.err.printf("retry %d failed...\r", attempts);
				}
				attempts++;

				try {
					Thread.sleep(retryPeriod_ms);
				} catch (InterruptedException e1) {
				}

			} catch (MalformedURLException e) {
				throw new RuntimeException("Error connecting to remote '"+name+"', malformed URL");
			}
		}
	}

	public static void rebindAndHookUnbind(String name, Remote stub)
	{
		try
		{
			Naming.rebind(name, stub);
		}
		catch (Exception e)
		{
			System.err.printf("error binding RMI name '%s'\n", name);
			e.printStackTrace();
			System.exit(0);
		}
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					Naming.unbind(name);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}
}
