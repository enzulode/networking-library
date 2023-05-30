package com.enzulode.network.exception;

/**
 * This exception represents time timeout ended situation
 */
public class TimeoutException extends NetworkException
{
    /**
     * Network Exception constructor with no cause provided
     *
     * @param message exception message
     */
    public TimeoutException(String message) {
        super(message);
    }

    /**
     * Network Exception constructor
     *
     * @param message exception message
     * @param cause   exception cause
     */
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
