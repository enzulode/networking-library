package com.enzulode.network.model.interconnection;

import com.enzulode.network.model.interconnection.util.ResponseCode;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * An abstract response entity
 *
 */
public abstract class Response implements Serializable
{
    /**
     * Response serial version uid
     *
     */
    @Serial
    private static final long serialVersionUID = -5875730935109456363L;

    /**
	 * The address of sender
	 *
	 */
	private InetSocketAddress from;

	/**
	 * The destination address
	 *
	 */
	private InetSocketAddress to;

	/**
	 * Response code
     *
	 */
	protected final ResponseCode code;

	/**
	 * Response constructor without source and destination address provided
	 *
	 * @param code response code
	 */
	public Response(ResponseCode code)
	{
//		Requiring response code to be non-null
		Objects.requireNonNull(code, "Response code cannot be null");

		this.code = code;
	}

	/**
	 * Response constructor
	 *
	 * @param from source address
	 * @param to destination address
	 * @param code response code
	 */
	public Response(InetSocketAddress from, InetSocketAddress to, ResponseCode code)
	{
//		Requiring response params to be non-null
		Objects.requireNonNull(from, "Response source address cannot be null");
		Objects.requireNonNull(to, "Response destination address cannot be null");
		Objects.requireNonNull(code, "Response code cannot be null");

		this.from = from;
		this.to = to;
		this.code = code;
	}

	/**
	 * Response source address getter
	 *
	 * @return current response source address
	 */
	public InetSocketAddress getFrom()
	{
		return from;
	}

	/**
	 * Response destination address getter
	 *
	 * @return current response destination address
	 */
	public InetSocketAddress getTo()
	{
		return to;
	}

	/**
	 * Response status code getter
	 *
	 * @return response code
	 */
	public ResponseCode getCode()
	{
		return code;
	}

	/**
	 * Response source address setter
	 *
	 * @param from source address to be set
	 */
	public void setFrom(InetSocketAddress from)
	{
//		Requiring response source address to be non-null
		Objects.requireNonNull(from, "Response source address cannot be null");

		this.from = from;
	}

	/**
	 * Response destination address setter
	 *
	 * @param to destination address to be set
	 */
	public void setTo(InetSocketAddress to)
	{
//		Requiring response destination address to be non-null
		Objects.requireNonNull(to, "Response destination address cannot be null");

		this.to = to;
	}
}
