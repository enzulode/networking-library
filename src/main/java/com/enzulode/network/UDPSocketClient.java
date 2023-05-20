package com.enzulode.network;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.RequestMapper;
import com.enzulode.network.mapper.ResponseMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is a UDPSocket client implementation
 *
 */
@Getter
public class UDPSocketClient implements AutoCloseable
{
	/**
	 * Opened datagram socket
	 *
	 */
	@NonNull
	private final DatagramSocket socket;

	/**
	 * Local address instance
	 *
	 */
	@NonNull
	private final InetSocketAddress localAddress;

	/**
	 * Server address instance
	 */
	@NonNull
	private final InetSocketAddress serverAddress;

	/**
	 * UDPSocket client constructor with default params
	 *
	 */
	public UDPSocketClient() throws NetworkException
	{
		this(0, "127.0.0.1", 8080);
	}

	/**
	 * UDPSocket client constructor.
	 *
	 * @param localPort the port, UDPSocket will be bind to (0 - any available port automatically / provide your own port)
	 * @throws NetworkException if it's failed to open a socket and bind it to a concrete port
	 */
	public UDPSocketClient(
			int localPort,
			@NonNull String serverHost,
			int serverPort
	) throws NetworkException
	{
		try
		{
			this.socket = new DatagramSocket(localPort);

			this.localAddress = new InetSocketAddress("127.0.0.1", socket.getLocalPort());
			this.serverAddress = new InetSocketAddress(serverHost, serverPort);

//			Socket configuration
			this.socket.setReuseAddress(true);
		}
		catch (SocketException e)
		{
			throw new NetworkException("Unable to open datagram socket", e);
		}
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
	 */
	public <T extends Response> T sendRequestAndWaitResponse(@NonNull Request request) throws NetworkException
	{
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
			return waitForResponse();
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
	 */
	private void sendRequestWithOverhead(@NonNull byte[] requestBytes) throws NetworkException
	{
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
	 */
	private void sendRequestNoOverhead(@NonNull byte[] requestBytes) throws NetworkException
	{
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
	 * @param <T> means the expected type of response
	 * @throws NetworkException if it's failed to receive response from the server
	 */
	@SuppressWarnings("unchecked")
	private <T extends Response> T waitForResponse() throws NetworkException
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
			Response response = ResponseMapper.mapFromBytesToInstance(allResponseBytes);

			return (T) response;
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
