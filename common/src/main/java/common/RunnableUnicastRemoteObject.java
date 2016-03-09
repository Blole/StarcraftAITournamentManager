package common;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public abstract class RunnableUnicastRemoteObject extends UnicastRemoteObject implements Runnable
{
	private static final long serialVersionUID = -2098573351518535490L;
	private static ArrayList<RunnableUnicastRemoteObject> exported = new ArrayList<>();
	
	
	
	private boolean hasExited = false;
	private Exception exception = null;
	private Thread thread = null;
	private final boolean unexportAfterExit;
	
	
	protected RunnableUnicastRemoteObject() throws RemoteException
	{
		this(true);
	}
	
	protected RunnableUnicastRemoteObject(boolean unexportAfterExit) throws RemoteException
	{
		this.unexportAfterExit = unexportAfterExit;
		exported.add(this);
	}
	
	protected Exception getException()
	{
		return exception;
	}
	
	public Thread thread()
	{
		return thread;
	}

	protected void tryUnexport(boolean force)
	{
		try
		{
			unexportObject(this, force);
			exported.remove(this);
		}
		catch (NoSuchObjectException e1)
		{
			//already unexported
		}
	}

	@Override
	final public void run()
	{
		thread = Thread.currentThread();
		thread.setName(toString());
		
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
		
		if (unexportAfterExit)
			tryUnexport(true);
		
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
	
	
	
	public static void unexportAll(boolean force)
	{
		for (RunnableUnicastRemoteObject o : exported)
			o.tryUnexport(force);
	}
}
