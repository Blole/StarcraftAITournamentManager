package common;

public interface RunnableWithShutdownHook extends Runnable
{
	void onExit();
	
	static void addShutdownHook(RunnableWithShutdownHook runnable)
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				runnable.onExit();
			}
		});
	}
}
