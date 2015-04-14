package common;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class RunnableUnicastRemoteObject extends UnicastRemoteObject implements Runnable
{
	private static final long serialVersionUID = -2098573351518535490L;
	
	
	
	private boolean hasExited = false;
	
	protected RunnableUnicastRemoteObject() throws RemoteException
	{
	}

	@Override
	final public void run()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				exit();
			}
		});
		
		Throwable exception = null;
		try
		{
			onRun();
		}
		catch (Throwable e)
		{
			exception = e;
		}
		
		try
		{
			exit();
		}
		catch (Throwable e)
		{
			if (exception == null)
				exception = e;
			else
				exception.addSuppressed(e);
		}
		
		ExceptionUtils.printRootCauseStackTrace(exception);
		try
		{
			unexportObject(this, true);
		}
		catch (NoSuchObjectException e1)
		{
			//don't care
		}
		
		if (exception != null)
		{
			if (exception instanceof RuntimeException)
				throw (RuntimeException)exception;
			else
				throw new RuntimeException(exception);
		}
	}
	
	private void exit()
	{
		if (!hasExited)
		{
			hasExited = true;
			onExit();
		}
	}
	
	public abstract void onRun() throws Throwable;
	public abstract void onExit();
}
