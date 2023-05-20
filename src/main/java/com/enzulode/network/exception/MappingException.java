package com.enzulode.network.exception;

/**
 * Exception represents mapping issues
 *
 */
public class MappingException extends Exception
{
	/**
	 * Mapper Exception constructor with no cause provided
	 *
	 * @param message exception message
	 */
	public MappingException(String message)
	{
		super(message);
	}

	/**
	 * Mapper Exception constructor
	 *
	 * @param message exception message
	 * @param cause exception cause
	 */
	public MappingException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
