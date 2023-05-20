package com.enzulode.network.model.interconnection;

import com.enzulode.network.model.interconnection.util.ResponseCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * An abstract response entity
 *
 */
@Getter
@Setter
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
    @NonNull
	private InetSocketAddress from;

	/**
	 * The destination address
	 *
	 */
    @NonNull
	private InetSocketAddress to;

	/**
	 * Response code
     *
	 */
    @NonNull
	protected final ResponseCode code;

	/**
	 * Response constructor
	 *
	 * @param from source address
	 * @param to destination address
	 * @param code response code
	 */
	public Response(@NonNull InetSocketAddress from, @NonNull InetSocketAddress to, @NonNull ResponseCode code)
	{
		this.from = from;
		this.to = to;
		this.code = code;
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
}
