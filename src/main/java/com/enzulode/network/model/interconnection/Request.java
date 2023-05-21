package com.enzulode.network.model.interconnection;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * An abstract request entity
 *
 */
public abstract class Request implements Serializable
{
    /**
     * Request serial version uid
	 *
     */
    @Serial
    private static final long serialVersionUID = -6487298602742247864L;

    /**
	 * The source address
	 *
	 */
	private InetSocketAddress from;

	/**
	 * The destination address
	 *
	 */
	private InetSocketAddress to;

	/**
	 * Empty request constructor
	 *
	 */
	public Request()
	{
	}

	/**
	 * Request constructor
	 *
	 * @param from source address
	 * @param to destination address
	 */
	public Request(InetSocketAddress from, InetSocketAddress to)
	{
//		Check source and destination addresses to be non-null
		Objects.requireNonNull(from, "Request source address cannot be null");
		Objects.requireNonNull(to, "Request destination address cannot be null");

		this.from = from;
		this.to = to;
	}

	/**
	 * Request source address getter
	 *
	 * @return current request source address
	 */
	public InetSocketAddress getFrom()
	{
		return from;
	}

	/**
	 * Request destination address getter
	 *
	 * @return current request destination address
	 */
	public InetSocketAddress getTo()
	{
		return to;
	}

	/**
	 * Request source address setter
	 *
	 * @param from source address to be set
	 */
	public void setFrom(InetSocketAddress from)
	{
//		Requiring request source address to be non-null
		Objects.requireNonNull(from, "Request source address cannot be null");

		this.from = from;
	}

	/**
	 * Request destination address setter
	 *
	 * @param to destination address to be set
	 */
	public void setTo(InetSocketAddress to)
	{
//		Requiring request destination address to be non-null
		Objects.requireNonNull(to, "Request destination address cannot be null");

		this.to = to;
	}
}
