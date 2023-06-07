package com.enzulode.network.concurrent.task;

import com.enzulode.network.handling.RequestHandler;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.interconnection.impl.PingRequest;
import com.enzulode.network.model.interconnection.impl.PongResponse;
import com.enzulode.network.model.interconnection.util.ResponseCode;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;

/**
 * Request handling task
 *
 */
public class HandlingTask implements Runnable
{
	/**
	 * Datagram socket instance
	 *
	 */
	private final DatagramSocket socket;

	/**
	 * Request to be handled
	 *
	 */
	private final Request request;

	/**
	 * Request handler instance
	 *
	 */
	private final RequestHandler handler;

	/**
	 * Specific executor service instance
	 *
	 */
	private final ExecutorService responseSendingThreadPool;

	/**
	 * Request handling task constructor
	 *
	 * @param socket datagram socket instance
	 * @param request handling request instance
	 * @param handler request handler instance
	 * @param responseSendingThreadPool a response-sending thread pool
	 */
	public HandlingTask(DatagramSocket socket, Request request, RequestHandler handler, ExecutorService responseSendingThreadPool)
	{
		this.socket = socket;
		this.request = request;
		this.handler = handler;
		this.responseSendingThreadPool = responseSendingThreadPool;
	}

	/**
	 * The task body
	 *
	 */
	@Override
	public void run()
	{
		Response response;
		if (request instanceof PingRequest)
			response = new PongResponse(ResponseCode.SUCCEED);
		else
			response = handler.handle(request);

		response.setFrom(request.getTo());
		response.setTo(request.getFrom());

		responseSendingThreadPool.submit(new RespondingTask(socket, response));
	}
}
