package com.enzulode.network.concurrent.factories;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Special thread factory to name threads
 *
 */
public class ThreadNamingFactory implements ThreadFactory
{
	/**
	 * Thread counter
	 *
	 */
	private final AtomicInteger threadNumber;

	/**
	 * Thread prefix
	 *
	 */
	private final String threadPrefix;

	/**
	 * Thread pool group
	 *
	 */
	private final ThreadGroup threadPoolGroup;

	/**
	 * Thread naming factory constructor
	 *
	 * @param threadGroupName thread group name
	 * @param threadName thread name
	 */
	public ThreadNamingFactory(final String threadGroupName, final String threadName)
	{
		Objects.requireNonNull(threadGroupName, "Thread group name cannot be null");
		Objects.requireNonNull(threadName, "Thread type cannot be null");

		threadNumber = new AtomicInteger(1);

		threadPrefix = "pool-" + threadGroupName.strip() + "-" + threadName + "-";
		threadPoolGroup = new ThreadGroup(threadGroupName);
	}

	/**
	 * Thread factory method
	 *
	 * @param r a runnable to be executed by new thread instance
	 * @return named thread instance
	 */
	@Override
	public Thread newThread(Runnable r)
	{
		Thread thread = new Thread(threadPoolGroup, r);
		thread.setName(threadPrefix + threadNumber.getAndIncrement());
		return thread;
	}
}
