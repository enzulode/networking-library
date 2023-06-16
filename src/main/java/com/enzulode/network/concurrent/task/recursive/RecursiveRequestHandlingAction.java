package com.enzulode.network.concurrent.task.recursive;

import com.enzulode.network.concurrent.task.RespondingTask;
import com.enzulode.network.handling.RequestHandler;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.interconnection.impl.PingRequest;
import com.enzulode.network.model.interconnection.impl.PongResponse;
import com.enzulode.network.model.interconnection.util.ResponseCode;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;

public class RecursiveRequestHandlingAction extends RecursiveAction
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

	public RecursiveRequestHandlingAction(
			DatagramSocket socket,
			Request request,
			RequestHandler handler,
			ExecutorService responseSendingThreadPool
	)
	{
		super();

		this.socket = socket;
		this.request = request;
		this.handler = handler;
		this.responseSendingThreadPool = responseSendingThreadPool;
	}

	/**
	 * The main computation performed by this task.
	 */
	@Override
	protected void compute()
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
