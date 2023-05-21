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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class is a UDPSocket client implementation
 *
 */
public final class UDPSocketClient implements AutoCloseable
{
	/**
	 * Opened datagram socket
	 *
	 */
	private final DatagramSocket socket;

	/**
	 * Local address instance
	 *
	 */
	private final InetSocketAddress localAddress;

	/**
	 * Server address instance
	 */
	private final InetSocketAddress serverAddress;

	/**
	 * UDPSocket client constructor with default params
	 *
	 * @throws NetworkException if it's failed to open a socket and bind it to a concrete port
	 */
	public UDPSocketClient() throws NetworkException
	{
		this(0, "127.0.0.1", 8080);
	}

	/**
	 * UDPSocket client constructor.
	 *
	 * @param localPort the port, UDPSocket will be bind to (0 - any available port automatically / provide your own port)
	 * @param serverHost the remote server host
	 * @param serverPort the remote server port
	 * @throws NetworkException if it's failed to open a socket and bind it to a concrete port
	 */
	public UDPSocketClient(
			int localPort,
			String serverHost,
			int serverPort
	) throws NetworkException
	{
//		Requiring server host to be non-null
		Objects.requireNonNull(serverHost, "Server host cannot be null");

		try
		{
			this.socket = new DatagramSocket(localPort);

			this.localAddress = new InetSocketAddress("127.0.0.1", socket.getLocalPort());
			this.serverAddress = new InetSocketAddress(serverHost, serverPort);

//			Socket configuration
			this.socket.setReuseAddress(true);
			this.socket.setSoTimeout(2000);
		}
		catch (SocketException e)
		{
			throw new NetworkException("Unable to open datagram socket", e);
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
	 * @param request request to be sent
	 * @return a response instance
	 * @param <T> means the expected type of response
	 * @throws NetworkException if it failed to send the response to the server,
	 * if the server response data was corrupted, if it failed to receive response from the server or
	 * request mapping failed
	 * @throws ServerNotAvailableException if server timeout exception was caught
	 */
	@SuppressWarnings("unchecked")
	public <T extends Response> T sendRequestAndWaitResponse(Request request) throws NetworkException, ServerNotAvailableException
	{
//		Require request to be non-null
		Objects.requireNonNull(request, "Request cannot be null");

//		Readjusting request addresses
		request.setFrom(localAddress);
		request.setTo(serverAddress);

		try
		{
//			First of all, we should get our request byte representation
			byte[] requestBytes = RequestMapper.mapFromInstanceToBytes(request);

//			If request size is more than default buffer size - send with overhead : else - send without overhead
			if (requestBytes.length > NetworkUtils.REQUEST_BUFFER_SIZE)
				sendRequestWithOverhead(requestBytes);
			else
				sendRequestNoOverhead(requestBytes);

//			Waiting for response
			return (T) waitForResponse();
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map request from instance to bytes during request proceeding", e);
		}
	}

	/**
	 * This method sends request with overhead after separation
	 *
	 * @param requestBytes raw request bytes
	 * @throws NetworkException if it's failed to send some of DatagramPackets
	 * @throws ServerNotAvailableException if server timeout exception was caught
	 */
	private void sendRequestWithOverhead(byte[] requestBytes) throws NetworkException, ServerNotAvailableException
	{
//		Require request bytes array to be non-null
		Objects.requireNonNull(requestBytes, "Request bytes array cannot be null");

//		Get request chunks from raw request bytes
		List<byte[]> requestChunks = NetworkUtils.splitIntoChunks(requestBytes, NetworkUtils.REQUEST_BUFFER_SIZE);

//		Wrap chunks with UDPFrames
		List<UDPFrame> udpFrames = NetworkUtils.wrapChunksWithUDPFrames(requestChunks);

//		Wrap UDPFrames with DatagramPackets
		List<DatagramPacket> datagramPackets = NetworkUtils.wrapUDPFramesWithDatagramPackets(
				udpFrames,
				serverAddress
		);

//		Trying to send datagram packets
		try
		{
			for (DatagramPacket packet : datagramPackets)
			{
				try
				{
					TimeUnit.MILLISECONDS.sleep(10);
				}
				catch (InterruptedException ignored) {}
				socket.send(packet);
			}
		}
		catch (SocketTimeoutException e)
		{
			throw new ServerNotAvailableException("Server is not currently available", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send packets", e);
		}
	}

	/**
	 * This method sends request without overhead
	 *
	 * @param requestBytes raw request bytes
	 * @throws NetworkException if it's failed to send DatagramPacket
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

//			Wrap UDPFrame with DatagramPacket
			DatagramPacket requestPacket = new DatagramPacket(udpFrameBytes, udpFrameBytes.length, serverAddress);

//			Trying to send the request
			socket.send(requestPacket);
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
	 * @return response instance
	 * @throws NetworkException if it's failed to receive response from the server
	 */
	private Response waitForResponse() throws NetworkException
	{
//		Response byte buffer initiation
		byte[] responseBytes = new byte[NetworkUtils.RESPONSE_BUFFER_SIZE];

//		After the request was sent we should prepare a datagram packet for response
		DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);

		try
		{
			byte[] allResponseBytes = new byte[0];
			boolean gotAll = false;
			do
			{
//				Receiving a response frame
				socket.receive(responsePacket);

//				Retrieving response raw bytes
				byte[] currentFrameBytes = responsePacket.getData();

//				Mapping UDPFrame from raw bytes
				UDPFrame udpFrame = FrameMapper.mapFromBytesToInstance(currentFrameBytes);

//				Enriching response bytes with new bytes
				allResponseBytes = NetworkUtils.concatTwoByteArrays(allResponseBytes, udpFrame.data());

				if (udpFrame.last())
					gotAll = true;
			}
			while (!gotAll);

//			Mapping response bytes into an instance
			return ResponseMapper.mapFromBytesToInstance(allResponseBytes);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Mapping operation failure detected", e);
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to receive response from the server", e);
		}
	}

	/**
	 * Method forced by {@link AutoCloseable} interface.
	 * Allows to use this class in the try-with-resources construction
	 * Automatically closes datagram socket
	 */
	@Override
	public void close()
	{
		socket.close();
	}
}
