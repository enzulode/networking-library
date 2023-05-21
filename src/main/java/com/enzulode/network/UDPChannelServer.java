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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Objects;

/**
 * This class is a UDPChannel server implementation
 *
 */
public final class UDPChannelServer implements AutoCloseable
{
	/**
	 * Default server port
	 *
	 */
	private static final int DEFAULT_PORT = 8080;

	/**
	 * Datagram channel instance
	 *
	 */
	private final DatagramChannel channel;

	/**
	 * Server address instance
	 *
	 */
	private final InetSocketAddress serverAddress;

	/**
	 * Request handler instance
	 */
	private RequestHandler handler;

	/**
	 * UDPChannelServer constructor without port specified.
	 * Server will be bind to DEFAULT_PORT
	 *
	 * @throws NetworkException if it's failed to open DatagramChannel
	 */
	public UDPChannelServer() throws NetworkException
	{
		this(DEFAULT_PORT);
	}

	/**
	 * UDPChannelServer constructor with port specified.
	 * Server will be bind to provided port
	 *
	 * @param port the {@link DatagramChannel} will be bind to this
	 * @throws NetworkException if it's failed to open DatagramChannel
	 */
	public UDPChannelServer(
			int port
	) throws NetworkException
	{
		this(new InetSocketAddress("127.0.0.1", port));
	}

	/**
	 * UDPChannelServer constructor with server address specified
	 * Server will be bind to provided address
	 *
	 * @param address an address to bind a socket
	 * @throws NetworkException if it's failed to open DatagramChannel
	 */
	public UDPChannelServer(
			InetSocketAddress address
	) throws NetworkException
	{
//		Requiring socket address to be non-null
		Objects.requireNonNull(address, "Socket binding address cannot be null");

		try
		{
			this.channel = DatagramChannel.open();

//			Channel configuration
			channel.bind(address);

			if (address.getPort() == 0)
				serverAddress = new InetSocketAddress("localhost", channel.socket().getLocalPort());
			else
				serverAddress = address;

			this.channel.configureBlocking(false);
			this.channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			this.channel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to open DatagramChannel", e);
		}

	}

	/**
	 * UDPChannelServer channel getter
	 *
	 * @return server channel getter
	 */
	public DatagramChannel getChannel()
	{
		return channel;
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
	 * This method sets current request handler
	 *
	 * @param handler request handler
	 */
	public void addRequestHandler(RequestHandler handler)
	{
//		Requiring request handler to be non-null
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
			throw new NetworkException("Failed to handle the request: RequestHandler was not set");

		Request request = waitRequest();
		Response response = handler.handle(request);

		sendResponse(response);
	}

	/**
	 * Waiting request from clients
	 *
	 * @param <T> request type parameter
	 * @return request instance
	 * @throws NetworkException if it's failed to receive the request from client
	 */
	@SuppressWarnings("unchecked")
	private <T extends Request> T waitRequest() throws NetworkException
	{
		ByteBuffer incomingBuffer = ByteBuffer.allocate(NetworkUtils.REQUEST_BUFFER_SIZE * 2);

		try
		{
			byte[] allRequestBytes = new byte[0];
			boolean gotAll = false;

			do
			{
//				Receiving incoming byte buffer
				incomingBuffer.clear();
				SocketAddress addr = channel.receive(incomingBuffer);
//				Skip current iteration if nothing was got in receive
				if (addr == null) continue;

//				Retrieving current frame bytes from incoming byte buffer
				byte[] currentFrameBytes = new byte[incomingBuffer.position()];
				incomingBuffer.rewind();
				incomingBuffer.get(currentFrameBytes);

//				Mapping UDPFrame from raw bytes
				UDPFrame currentFrame = FrameMapper.mapFromBytesToInstance(currentFrameBytes);
//				Enriching request bytes with new bytes
				allRequestBytes = NetworkUtils.concatTwoByteArrays(allRequestBytes, currentFrame.data());

//				Change gotAll state if got the last UDPFrame
				if (currentFrame.last()) gotAll = true;

			} while (!gotAll);

//			Mapping request instance from raw request bytes
			Request request = RequestMapper.mapFromBytesToInstance(allRequestBytes);
			request.setTo(serverAddress);
			return (T) request;
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to receive request: mapping failure detected", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to receive request from server", e);
		}
	}

	/**
	 * This method sends response
	 *
	 * @param response response instance
	 * @throws NetworkException if it's failed to send response with an overhead or
	 * if it's failed to send response without an overhead
	 */
	private void sendResponse(Response response) throws NetworkException
	{
//		Requiring response instance to be non-null
		Objects.requireNonNull(response, "Response cannot be null");

		response.setFrom(serverAddress);

		try
		{
//			Mapping response to a byte array
			byte[] responseBytes = ResponseMapper.mapFromInstanceToBytes(response);

//			Check if response should be divided into separate chunks
			if (responseBytes.length > NetworkUtils.RESPONSE_BUFFER_SIZE)
				sendResponseWithOverhead(responseBytes, response.getTo());
			else
				sendResponseNoOverhead(responseBytes, response.getTo());
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
	private void sendResponseWithOverhead(
			byte[] responseBytes,
			InetSocketAddress destination
	) throws NetworkException
	{
//		Requiring response bytes and destination address to be non-null
		Objects.requireNonNull(responseBytes, "Response bytes array cannot be null");
		Objects.requireNonNull(destination, "Destination address cannot be null");

//		Get response chunks from rew response bytes
		List<byte[]> responseChunks = NetworkUtils.splitIntoChunks(responseBytes, NetworkUtils.RESPONSE_BUFFER_SIZE);

//		Wrap chunks with UDPFrames
		List<UDPFrame> udpFrames = NetworkUtils.wrapChunksWithUDPFrames(responseChunks);

//		Map udpFrames to bytes
		List<byte[]> framesBytes = NetworkUtils.udpFramesToBytes(udpFrames);

//		Sending all response frames to the client
		try
		{
			for (byte[] frameBytes : framesBytes)
				channel.send(ByteBuffer.wrap(frameBytes), destination);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send response with an overhead", e);
		}
	}

	/**
	 * This method sends response without an overhead
	 *
	 * @param responseBytes raw response bytes
	 * @param destination response destination
	 * @throws NetworkException if it's failed to send response without an overhead
	 */
	private void sendResponseNoOverhead(
			byte[] responseBytes,
			InetSocketAddress destination
	) throws NetworkException
	{
//		Requiring response bytes and destination address to be non-null
		Objects.requireNonNull(responseBytes, "Response bytes array cannot be null");
		Objects.requireNonNull(destination, "Destination address cannot be null");

		try
		{
//			Wrap raw response bytes with UDPFrame
			UDPFrame udpFrame = new UDPFrame(responseBytes, true);

//			Get UDPFrame bytes
			byte[] udpFrameBytes = FrameMapper.mapFromInstanceToBytes(udpFrame);

//			Sending response frame to the client
			channel.send(ByteBuffer.wrap(udpFrameBytes), destination);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map frame to bytes", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send response without an overhead", e);
		}
	}

	/**
	 * Method provided by {@link AutoCloseable} interface.
	 * Allows to use this class in the try-with-resources construction.
	 * Automatically closes selector and datagram channel
	 *
	 */
	@Override
	public void close() throws NetworkException
	{
		try
		{
			channel.close();
		}
		catch (IOException e)
		{
			throw new NetworkException("Unable to close DatagramChannel", e);
		}
	}
}
