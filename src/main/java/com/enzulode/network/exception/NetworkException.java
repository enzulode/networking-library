package com.enzulode.network.exception;

import lombok.NonNull;

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
	public NetworkException(@NonNull String message)
	{
		super(message);
	}

	/**
	 * Network Exception constructor
	 *
	 * @param message exception message
	 * @param cause exception cause
	 */
	public NetworkException(@NonNull String message, @NonNull Throwable cause)
	{
		super(message, cause);
	}
}
