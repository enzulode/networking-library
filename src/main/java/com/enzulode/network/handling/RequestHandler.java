package com.enzulode.network.handling;

import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;

/**
 * Request handling functional interface
 *
 */
@FunctionalInterface
public interface RequestHandler
{
	/**
	 * Handling method
	 *
	 * @param request request that is being handled
	 * @return response instance
	 */
	Response handle(Request request);
}
