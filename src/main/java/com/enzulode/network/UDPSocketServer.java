package com.enzulode.network;

import com.enzulode.network.concurrent.factories.ThreadNamingFactory;
import com.enzulode.network.concurrent.task.recursive.RecursiveRequestHandlingAction;
import com.enzulode.network.concurrent.task.recursive.RecursiveRequestReceivingAction;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.handling.RequestHandler;
import com.enzulode.network.model.interconnection.Request;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * This class is a UDPSocket server implementation
 *
 */
public final class UDPSocketServer implements AutoCloseable
{
	/**
	 * Default server port
	 *
	 */
	public static final int DEFAULT_PORT = 8080;

	/**
	 * DatagramSocket instance
	 *
	 */
	private final DatagramSocket socket;

	/**
	 * Server address instance
	 *
	 */
	private final InetSocketAddress serverAddress;

	/**
	 * Current request handler instance
	 *
	 */
	private RequestHandler handler;

	/**
	 * Request receiving executors
	 *
	 */
	private final ForkJoinPool requestReceivingPool;

	/**
	 * Request handling thread pool
	 *
	 */
	private final ForkJoinPool requestHandlingPool;

	/**
	 * Response sending thread pool
	 *
	 */
	private final ExecutorService responseSendingExecutors;

	/**
	 * A concurrent map instance for resolved requests
	 *
	 */
	private final ConcurrentMap<SocketAddress, Request> requestsMap;

	/**
	 * UDPChannelServer constructor without port specified.
	 * Server will be bind to DEFAULT_PORT
	 *
	 * @throws NetworkException if it's failed to bind DatagramSocket
	 */
	public UDPSocketServer() throws NetworkException
	{
		this(DEFAULT_PORT);
	}

	/**
	 * UDPChannelServer constructor with port specified.
	 * Server will be bind to provided port
	 *
	 * @param port the {@link DatagramSocket} will be bind to this
	 * @throws NetworkException if it's failed to bind DatagramSocket
	 */
	public UDPSocketServer(int port) throws NetworkException
	{
		this(new InetSocketAddress("127.0.0.1", port));
	}

	/**
	 * UDPSocketServer constructor
	 *
	 * @param serverAddress address to bind a server socket
	 * @throws NetworkException if it's failed to bind socket
	 */
	public UDPSocketServer(InetSocketAddress serverAddress) throws NetworkException
	{
//		Requiring server socket address to be non-null
		Objects.requireNonNull(serverAddress, "Socket binding address cannot be null");

		try
		{
			this.socket = new DatagramSocket(serverAddress);

			if (serverAddress.getPort() == 0)
				this.serverAddress = new InetSocketAddress("127.0.0.1", socket.getLocalPort());
			else
				this.serverAddress = serverAddress;

//			Configure socket
			this.socket.setReuseAddress(true);

			this.requestReceivingPool = new ForkJoinPool(1);
			this.requestHandlingPool = new ForkJoinPool(4);

			this.responseSendingExecutors = Executors.newCachedThreadPool(
					new ThreadNamingFactory("responding", "thread")
			);

			this.requestsMap = new ConcurrentHashMap<>();
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to create a datagram socket", e);
		}
	}

	/**
	 * Server address getter
	 *
	 * @return server address instance
	 */
	public InetSocketAddress getServerAddress()
	{
		return serverAddress;
	}

	/**
	 * Current request handler getter
	 *
	 * @return current request handler instance
	 */
	public RequestHandler getHandler()
	{
		return handler;
	}

	/**
	 * This method sets current request handler
	 *
	 * @param handler request handler
	 */
	public void subscribe(RequestHandler handler)
	{
//		Require request handler to be non-null
		Objects.requireNonNull(handler, "Request handler cannot be null");

		this.handler = handler;
	}

	/**
	 * This method handles incoming requests with provided {@link RequestHandler} and
	 * sends a specific response
	 *
	 * @throws NetworkException if it's failed to select a channel or send the response
	 */
	public void handleIncomingRequests() throws NetworkException
	{
		if (handler == null)
			throw new NetworkException("Request handler is not currently set");

		requestReceivingPool.submit(new RecursiveRequestReceivingAction(socket, requestsMap));

		while (true)
		{
			if (requestsMap.isEmpty()) continue;

			for (Iterator<Request> i = requestsMap.values().iterator(); i.hasNext();)
			{
				Request req = i.next();
				i.remove();

				var requestHandlingAction = new RecursiveRequestHandlingAction(socket, req, handler, responseSendingExecutors);
				requestHandlingPool.submit(requestHandlingAction);
			}
		}
	}

	/**
	 * Method forced by {@link AutoCloseable} interface.
	 * Automatically closes socket in case of using inside try-with-resources code block
	 *
	 */
	@Override
	public void close()
	{
		socket.close();
	}
}