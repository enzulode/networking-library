package com.enzulode.network.exception;

/**
 * Exception represents the situation, when the server is not available
 *
 */
public class ServerNotAvailableException extends Exception
{
	/**
	 * Server not available exception constructor with no cause provided
	 *
	 * @param message exception message
	 */
	public ServerNotAvailableException(String message)
	{
		super(message);
	}

	/**
	 * Server not available exception constructor
	 *
	 * @param message exception message
	 * @param cause exception cause
	 */
	public ServerNotAvailableException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
