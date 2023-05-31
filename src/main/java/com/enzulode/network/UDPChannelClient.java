package com.enzulode.network;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.exception.ServerNotAvailableException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.RequestMapper;
import com.enzulode.network.mapper.ResponseMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.interconnection.impl.PingRequest;
import com.enzulode.network.model.interconnection.impl.PongResponse;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
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
			long idx = 0;
			for (byte[] frameBytes : framesBytes)
			{
				checkServerConnection();
				channel.send(ByteBuffer.wrap(frameBytes), serverAddress);
			}
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
			checkServerConnection();
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
	 * Method checks the server availability
	 *
	 * @throws ServerNotAvailableException if server is not currently available
	 * @throws NetworkException if something went wrong during sending or receiving something (but the server is ok)
	 */
	private void checkServerConnection() throws NetworkException, ServerNotAvailableException
	{
		try
		{
//			Creating PING request
			Request request = new PingRequest();
			request.setFrom(localAddress);
			request.setTo(serverAddress);

//			Mapping PING request into bytes
			byte[] pingRequestBytes = RequestMapper.mapFromInstanceToBytes(request);

//			Wrapping request bytes with udp frame
			UDPFrame frame = new UDPFrame(pingRequestBytes, true);

//			Mapping pingFrame into bytes
			byte[] pingFrameBytes = FrameMapper.mapFromInstanceToBytes(frame);

//			Sending ping request
			channel.send(ByteBuffer.wrap(pingFrameBytes), serverAddress);
			ByteBuffer pingResponseBuffer = ByteBuffer.allocate(NetworkUtils.RESPONSE_BUFFER_SIZE * 2);

			long startTime = System.currentTimeMillis();
			int timeout = 5000;
			while (true)
			{
				SocketAddress addr = channel.receive(pingResponseBuffer);

				if (System.currentTimeMillis() > startTime + timeout)
					throw new ServerNotAvailableException("Server is not available");

				if (addr == null) continue;

				byte[] currentFrameBytes = new byte[pingResponseBuffer.position()];
				pingResponseBuffer.rewind();
				pingResponseBuffer.get(currentFrameBytes);
				UDPFrame responseFrame = FrameMapper.mapFromBytesToInstance(currentFrameBytes);
				Response response = ResponseMapper.mapFromBytesToInstance(responseFrame.data());
				if (!(response instanceof PongResponse)) throw new ServerNotAvailableException("Server is not available");
				break;
			}
		} catch (IOException | MappingException e)
		{
			throw new NetworkException("Failed to send ping request", e);
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

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
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
				baos.writeBytes(currentFrame.data());

//				Change gotAll state if got the last UDPFrame
				if (currentFrame.last()) gotAll = true;

			} while (!gotAll);

//			Mapping request instance from raw request bytes
			byte[] responseBytes = baos.toByteArray();
			return ResponseMapper.mapFromBytesToInstance(responseBytes);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to receive response: mapping failure detected", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to receive response from server", e);
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
