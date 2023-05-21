package com.enzulode.network;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.exception.ServerNotAvailableException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.RequestMapper;
import com.enzulode.network.mapper.ResponseMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Objects;

/**
 * This class is a UDPChannel client implementation
 *
 */
public class UDPChannelClient implements AutoCloseable
{
	/**
	 * Local address instance
	 *
	 */
	private final InetSocketAddress localAddress;

	/**
	 * Server address instance
	 *
	 */
	private final InetSocketAddress serverAddress;

	/**
	 * Datagram channel instance
	 *
	 */
	private final DatagramChannel channel;

	/**
	 * UDPChannel client constructor with default params
	 *
	 * @throws NetworkException if it's failed to open a channel
	 */
	public UDPChannelClient() throws NetworkException
	{
		this(0, "127.0.0.1", 8080);
	}

	/**
	 * UDPSocket client constructor.
	 *
	 * @param localPort the port, UDPSocket will be bind to (0 - any available port automatically / provide your own port)
	 * @param serverHost the remote server host
	 * @param serverPort the remote server port
	 * @throws NetworkException if it's failed to open a datagram channel
	 */
	public UDPChannelClient(int localPort, String serverHost, int serverPort) throws NetworkException
	{
//		Requiring server host to be non-null
		Objects.requireNonNull(serverHost, "Server host cannot be null");

		try
		{
			this.channel = DatagramChannel.open();

			if (localPort == 0)
			{
				this.channel.bind(new InetSocketAddress("127.0.0.1", localPort));
				this.localAddress = new InetSocketAddress("127.0.0.1", this.channel.socket().getLocalPort());
			}
			else
			{
				this.localAddress = new InetSocketAddress("127.0.0.1", localPort);
				this.channel.bind(localAddress);
			}

			this.serverAddress = new InetSocketAddress(serverHost, serverPort);

//			Configure channel
			this.channel.configureBlocking(false);
			this.channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			this.channel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to open datagram channel", e);
		}
	}

	/**
	 * Client address getter
	 *
	 * @return client address instance
	 */
	public InetSocketAddress getLocalAddress()
	{
		return localAddress;
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
	 * This method allows you to send a request and receive a response for it
	 *
	 * @param <T> means the expected type of response
	 * @param request request to be sent
	 * @return a response instance
	 * @throws NetworkException if it failed to send the response to the server,
	 * if the server response data was corrupted, if it failed to receive response from the server or
	 * request mapping failed
	 * @throws ServerNotAvailableException if server is not currently available
	 */
	public <T extends Response> T sendRequestAndWaitResponse(Request request) throws NetworkException, ServerNotAvailableException
	{
//		Requiring request to be non-null
		Objects.requireNonNull(request, "Request cannot be null");

//		Readjusting request addresses
		request.setFrom(localAddress);
		request.setTo(serverAddress);

		try
		{
//			Map request instance to bytes array
			byte[] requestBytes = RequestMapper.mapFromInstanceToBytes(request);

//			If request size is more than default buffer size - send with overhead : else - send without overhead
			if (requestBytes.length > NetworkUtils.REQUEST_BUFFER_SIZE)
				sendRequestWithOverhead(requestBytes);
			else
				sendRequestNoOverhead(requestBytes);

			return waitForResponse();
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map request from instance to bytes", e);
		}
	}

	/**
	 * This method sends request with overhead after separation
	 *
	 * @param requestBytes raw request bytes
	 * @throws NetworkException if it's failed to send a frame
	 * @throws ServerNotAvailableException if server timeout exception was caught
	 */
	private void sendRequestWithOverhead(byte[] requestBytes) throws NetworkException, ServerNotAvailableException
	{
//		Require request bytes array to be non-null
		Objects.requireNonNull(requestBytes, "Request bytes array cannot be null");

//		Get response chunks from rew response bytes
		List<byte[]> requestChunks = NetworkUtils.splitIntoChunks(requestBytes, NetworkUtils.RESPONSE_BUFFER_SIZE);

//		Wrap chunks with UDPFrames
		List<UDPFrame> udpFrames = NetworkUtils.wrapChunksWithUDPFrames(requestChunks);

//		Map UDOFrames to bytes
		List<byte[]> framesBytes = NetworkUtils.udpFramesToBytes(udpFrames);

//		Sending all request frames to the server
		try
		{
			for (byte[] frameBytes : framesBytes)
				channel.send(ByteBuffer.wrap(frameBytes), serverAddress);
		}
		catch (SocketTimeoutException e)
		{
			throw new ServerNotAvailableException("Server is not currently available", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send response with an overhead", e);
		}
	}

	/**
	 * This method sends request without overhead
	 *
	 * @param requestBytes raw request bytes
	 * @throws NetworkException if it's failed to send request
	 * @throws ServerNotAvailableException if server timeout exception was caught
	 */
	private void sendRequestNoOverhead(byte[] requestBytes) throws NetworkException, ServerNotAvailableException
	{
//		Require request bytes array to be non-null
		Objects.requireNonNull(requestBytes, "Request bytes array cannot be null");

		try
		{
//			Wrap raw bytes with UDPFrame
			UDPFrame udpFrame = new UDPFrame(requestBytes, true);

//			Get UDPFrameBytes from UDPFrame instance
			byte[] udpFrameBytes = FrameMapper.mapFromInstanceToBytes(udpFrame);

//			Trying to send the request
			channel.send(ByteBuffer.wrap(udpFrameBytes), serverAddress);
		}
		catch (SocketTimeoutException e)
		{
			throw new ServerNotAvailableException("Server is not currently available", e);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map UDPFrame to raw bytes", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send request with no overhead", e);
		}
	}

	/**
	 * Method waits for response
	 *
	 * @param <T> response type param
	 * @return response instance
	 * @throws NetworkException if it's failed to receive response from the server
	 * @throws ServerNotAvailableException if server is not currently available
	 */
	private <T extends Response> T waitForResponse() throws NetworkException, ServerNotAvailableException
	{
		ByteBuffer responseBuffer = ByteBuffer.allocate(NetworkUtils.RESPONSE_BUFFER_SIZE * 2);

		try
		{
			byte[] allResponseBytes = new byte[0];
			boolean gotAll = false;

			do
			{
//				Receiving incoming byte buffer
				responseBuffer.clear();
				SocketAddress addr = channel.receive(responseBuffer);
//				Skip current iteration if nothing was got in receive
				if (addr == null) continue;

//				Retrieving current frame bytes from incoming byte buffer
				byte[] currentFrameBytes = new byte[responseBuffer.position()];
				responseBuffer.rewind();
				responseBuffer.get(currentFrameBytes);

//				Mapping UDPFrame from raw bytes
				UDPFrame currentFrame = FrameMapper.mapFromBytesToInstance(currentFrameBytes);

//				Enriching response bytes with new bytes
				allResponseBytes = NetworkUtils.concatTwoByteArrays(allResponseBytes, currentFrame.data());

//				Change gotAll state if got the last UDPFrame
				if (currentFrame.last()) gotAll = true;

			} while (!gotAll);

//			Mapping request instance from raw request bytes
			return ResponseMapper.mapFromBytesToInstance(allResponseBytes);
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
	 * Method forced by {@link AutoCloseable} interface.
	 * Allows to use this class in the try-with-resources construction
	 * Automatically closes datagram channel
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
			throw new NetworkException("Failed to close datagram channel", e);
		}
	}
}
