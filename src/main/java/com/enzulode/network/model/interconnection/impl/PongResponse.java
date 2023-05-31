package com.enzulode.network.model.interconnection.impl;

import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.interconnection.util.ResponseCode;

import java.io.Serial;

/**
 * The purpose of this response is in checking another side availability
 *
 */
public final class PongResponse extends Response
{
	/**
	 * Serial UID
	 *
	 */
	@Serial
	private static final long serialVersionUID = 3899919185329016906L;

	/**
	 * Response constructor without source and destination address provided
	 *
	 * @param code response code
	 */
	public PongResponse(ResponseCode code)
	{
		super(code);
	}
}
