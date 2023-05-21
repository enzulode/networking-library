package com.enzulode.network.exception;

/**
 * Exception represents network issues
 *
 */
public class NetworkException extends Exception
{
	/**
	 * Network Exception constructor with no cause provided
	 *
	 * @param message exception message
	 */
	public NetworkException(String message)
	{
		super(message);
	}

	/**
	 * Network Exception constructor
	 *
	 * @param message exception message
	 * @param cause exception cause
	 */
	public NetworkException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
