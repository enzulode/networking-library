package com.enzulode.network.concurrent.task;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.ResponseMapper;
import com.enzulode.network.model.interconnection.Response;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Response sending task
 *
 */
public class RespondingTask implements Runnable
{
	/**
	 * Logger instance
	 *
	 */
	private final Logger logger;

	/**
	 * Task lock instance
	 *
	 */
	private final Lock lock;

	/**
	 * Datagram socket instance
	 *
	 */
	private final DatagramSocket socket;

	/**
	 * Response instance
	 *
	 */
	private final Response response;

	/**
	 * Response-sending task constructor
	 *
	 * @param socket datagram socket instance
	 * @param response response instance
	 */
	public RespondingTask(DatagramSocket socket, Response response)
	{
		Objects.requireNonNull(socket, "Socket instance cannot be null");
		Objects.requireNonNull(response, "Response instance cannot be null");

		this.logger = Logger.getLogger(RespondingTask.class.getName());
		this.lock = new ReentrantLock();
		this.socket = socket;
		this.response = response;
	}

	/**
	 * The task body
	 *
	 */
	@Override
	public void run()
	{
		try
		{
			byte[] responseBytes = ResponseMapper.mapFromInstanceToBytes(response);

			if (responseBytes.length > NetworkUtils.RESPONSE_BUFFER_SIZE)
				sendResponseWithOverhead(responseBytes, response.getTo());
			else
				sendResponseNoOverhead(responseBytes, response.getTo());
		}
		catch (MappingException | NetworkException e)
		{
			logger.log(Level.SEVERE, "Something went wrong during responding", e);
		}
	}

	/**
	 * Method for sending response without an overhead
	 *
	 * @param responseBytes response bytes array
	 * @param destination response destination
	 * @throws NetworkException if it's failed to send the response
	 */
	private void sendResponseNoOverhead(byte[] responseBytes, InetSocketAddress destination) throws NetworkException
	{
//		Check that response bytes and response destination are not null
		Objects.requireNonNull(responseBytes, "Response bytes cannot be null");
		Objects.requireNonNull(destination, "Response destination cannot be null");

//		Wrap raw response bytes with UDPFrame
		UDPFrame udpFrame = new UDPFrame(responseBytes, true);

		try
		{
//			Map UDPFrame to bytes
			byte[] udpFrameBytes = FrameMapper.mapFromInstanceToBytes(udpFrame);

//			Wrap UDPFrame bytes with DatagramPacket
			DatagramPacket responsePacket = new DatagramPacket(udpFrameBytes, udpFrameBytes.length, destination);

//			Sending response to the client
			lock.lock();
			socket.send(responsePacket);
			lock.unlock();
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
	 * Method for sending the response with an overhead
	 *
	 * @param responseBytes response byte array
	 * @param destination response destination
	 * @throws NetworkException if it's failed to send the response
	 */
	private void sendResponseWithOverhead(byte[] responseBytes, InetSocketAddress destination) throws NetworkException
	{
//		Check that response bytes and response destination are not null
		Objects.requireNonNull(responseBytes, "Response bytes cannot be null");
		Objects.requireNonNull(destination, "Response destination cannot be null");

		List<DatagramPacket> responsePackets = NetworkUtils.getPacketsForOverheadedResponseBytes(responseBytes, destination);

		try
		{
			for (DatagramPacket packet : responsePackets)
			{
				NetworkUtils.timeout(10);
				lock.lock();
				socket.send(packet);
				lock.unlock();
			}
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to send the overheaded response", e);
		}
	}
}
