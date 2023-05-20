package com.enzulode.network.model.interconnection;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * An abstract request entity
 *
 */
@Getter
@Setter
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
    @NonNull
	private InetSocketAddress from;

	/**
	 * The destination address
	 *
	 */
    @NonNull
	private InetSocketAddress to;

	/**
	 * Request constructor
	 *
	 * @param from source address
	 * @param to destination address
	 */
	public Request(@NonNull InetSocketAddress from, @NonNull InetSocketAddress to)
	{
		this.from = from;
		this.to = to;
	}
}
