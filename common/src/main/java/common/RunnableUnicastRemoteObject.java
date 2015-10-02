package common;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class RunnableUnicastRemoteObject extends UnicastRemoteObject implements Runnable
{
	private static final long serialVersionUID = -2098573351518535490L;
	
	
	
	private boolean hasExited = false;
	private Exception exception = null;
	public Thread thread;
	
	protected RunnableUnicastRemoteObject() throws RemoteException
	{
	}
	
	protected Exception getException()
	{
		return exception;
	}

	@Override
	final public void run()
	{
		thread = Thread.currentThread();
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					exit();
				}
				catch (Throwable e)
				{
					System.err.println("error running shutdown-hooked exit()");
					e.printStackTrace();
				}
			}
		});
		
		try
		{
			onRun();
		}
		catch (InterruptedException e)
		{
			//interruptions are kind of expected
		}
		catch (Exception e)
		{
			exception = e;
		}
		
		try
		{
			exit();
		}
		catch (Exception e)
		{
			if (exception == null)
				exception = e;
			else
				exception.addSuppressed(e);
		}
		
		try
		{
			unexportObject(this, true);
		}
		catch (NoSuchObjectException e1)
		{
			//not that important
		}
		
		if (exception != null)
		{
			if (exception instanceof RuntimeException)
				throw (RuntimeException)exception;
			else
				throw new RuntimeException(exception);
		}
	}
	
	private void exit() throws Exception
	{
		if (!hasExited)
		{
			hasExited = true;
			onExit();
		}
	}
	
	protected abstract void onRun() throws Exception;
	
	/**
	 * To be run after onRun finished, even if an exception was thrown.
	 * Should never call System.exit() as this method may be called by a
	 * shutdown hook, and would then deadlock.
	 */
	protected abstract void onExit() throws Exception;
}
