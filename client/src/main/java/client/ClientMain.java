package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import common.RunnableUnicastRemoteObject;
import common.yaml.MyConstructor;


public class ClientMain
{
	public static void main(String[] args) throws RemoteException, FileNotFoundException, IOException
	{
		if (args.length < 1)
		{
			System.err.println("usage: java -jar client.jar SETTINGS.YAML");
			System.exit(-1);
		}
		else
		{
			String environmentText = FileUtils.readFileToString(new File(args[0]));
			MyConstructor constructor = new MyConstructor();
			constructor.addTypeDescription(ClientEnvironment.typeDescription);
			Yaml yaml = new Yaml(constructor);
			ClientEnvironment env = yaml.loadAs(environmentText, ClientEnvironment.class);
			
			Client client = new Client(env);
			client.run();
			RunnableUnicastRemoteObject.unexportAll(true);
		}
	}
}
