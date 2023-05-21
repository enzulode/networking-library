package com.enzulode.network;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.handling.RequestHandler;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.RequestMapper;
import com.enzulode.network.mapper.ResponseMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

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
	public void addRequestHandler(RequestHandler handler)
	{
//		Require request handler to be non-null
		Objects.requireNonNull(handler, "Request handler cannot be null");

		this.handler = handler;
	}

	/**
	 * This method handles the request with provided {@link RequestHandler} and
	 * sends response
	 *
	 * @throws NetworkException if it's failed to select a channel or send the response
	 */
	public void handleRequest() throws NetworkException
	{
		if (handler == null)
			throw new NetworkException("Request handler is not currently set");

		Request request = waitRequest();
		Response response = handler.handle(request);

		sendResponse(response, request.getFrom());
	}

	/**
	 * Waiting request from clients
	 *
	 * @param <T> request type parameter
	 * @return request instance
	 * @throws NetworkException if it's failed to receive the request from client
	 */
	private <T extends Request> T waitRequest() throws NetworkException
	{
//		ByteBuffer incomingFrameBytes = ByteBuffer.allocate(NetworkUtils.REQUEST_BUFFER_SIZE * 2);

		byte[] incomingFrameBytes = new byte[NetworkUtils.REQUEST_BUFFER_SIZE * 2];

//		Preparing a packet for incoming request
		DatagramPacket incomingRequestPacket = new DatagramPacket(
				incomingFrameBytes,
				incomingFrameBytes.length
		);

		try
		{
			byte[] allRequestBytes = new byte[0];
			boolean gotAll = false;

			do
			{
//				Receiving udp frame bytes
				socket.receive(incomingRequestPacket);

//				Mapping frame instance from bytes
				UDPFrame currentFrame = FrameMapper.mapFromBytesToInstance(incomingRequestPacket.getData());

//				Enriching request bytes with new bytes from the current frame
				allRequestBytes = NetworkUtils.concatTwoByteArrays(allRequestBytes, currentFrame.data());

				if (currentFrame.last()) gotAll = true;
			}
			while (!gotAll);

			return RequestMapper.mapFromBytesToInstance(allRequestBytes);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map UDPFrame from bytes to instance");
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to receive request via datagram socket", e);
		}
	}

	/**
	 * This method sends response
	 *
	 * @param response response instance
	 * @throws NetworkException if it's failed to send response with an overhead or
	 * if it's failed to send response without an overhead
	 */
	private void sendResponse(Response response, InetSocketAddress destination) throws NetworkException
	{
//		Requiring response instance and destination address to be non-null
		Objects.requireNonNull(response, "Response cannot be null");
		Objects.requireNonNull(destination, "Destination cannot be null");

		response.setFrom(serverAddress);
		response.setTo(destination);

		try
		{
//			Mapping response to bytes
			byte[] responseBytes = ResponseMapper.mapFromInstanceToBytes(response);

//			Check if response should be divided into separate chunks
			if (responseBytes.length > NetworkUtils.RESPONSE_BUFFER_SIZE)
				sendResponseWithOverhead(responseBytes, destination);
			else
				sendResponseNoOverhead(responseBytes, destination);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map response instance to bytes", e);
		}
	}

	/**
	 * This method sends response with an overhead
	 *
	 * @param responseBytes raw response bytes
	 * @param destination response destination
	 * @throws NetworkException if it's failed to send response with an overhead
	 */
	private void sendResponseWithOverhead(byte[] responseBytes, InetSocketAddress destination) throws NetworkException
	{
//		Requiring response bytes and destination address to be non-null
		Objects.requireNonNull(responseBytes, "Response bytes array cannot be null");
		Objects.requireNonNull(destination, "Destination cannot be null");

//		Get response chunks from rew response bytes
		List<byte[]> responseChunks = NetworkUtils.splitIntoChunks(responseBytes, NetworkUtils.RESPONSE_BUFFER_SIZE);

//		Wrap chunks with UDPFrames
		List<UDPFrame> udpFrames = NetworkUtils.wrapChunksWithUDPFrames(responseChunks);

//		Wrap UDPFrames with datagram packets
		List<DatagramPacket> responsePackets = NetworkUtils.wrapUDPFramesWithDatagramPackets(udpFrames, destination);

//		Sending all response packets to the client
		try
		{
			for (DatagramPacket packet : responsePackets)
				socket.send(packet);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send response packet", e);
		}
	}

	/**
	 * This method sends response without an overhead
	 *
	 * @param responseBytes raw response bytes
	 * @param destination response destination
	 * @throws NetworkException if it's failed to send response without an overhead
	 */
	private void sendResponseNoOverhead(byte[] responseBytes, InetSocketAddress destination) throws NetworkException
	{
//		Requiring response bytes and destination address to be non-null
		Objects.requireNonNull(responseBytes, "Response bytes array cannot be null");
		Objects.requireNonNull(destination, "Destination cannot be null");

//		Wrap raw response bytes with UDPFrame
		UDPFrame udpFrame = new UDPFrame(responseBytes, true);

		try
		{
//			Map UDPFrame to bytes
			byte[] udpFrameBytes = FrameMapper.mapFromInstanceToBytes(udpFrame);

//			Wrap UDPFrame bytes with DatagramPacket
			DatagramPacket responsePacket = new DatagramPacket(udpFrameBytes, udpFrameBytes.length, destination);

//			Sending response to the client
			socket.send(responsePacket);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map frame to bytes", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send response to the client", e);
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