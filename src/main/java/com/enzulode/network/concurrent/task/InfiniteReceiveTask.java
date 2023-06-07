package com.enzulode.network.concurrent.task;

import com.enzulode.network.concurrent.structures.ConcurrentFrameReceivingMap;
import com.enzulode.network.concurrent.structures.Pair;
import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.transport.UDPFrame;
import com.enzulode.network.util.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An infinite receiving task
 *
 */
public class InfiniteReceiveTask implements Runnable
{
	/**
	 * Logger instance
	 *
	 */
	private final Logger logger;

	/**
	 * Lock instance
	 *
	 */
	private final Lock lock;

	/**
	 * Datagram socket instance
	 *
	 */
	private final DatagramSocket socket;

	/**
	 * Concurrent frame receiving map instance
	 *
	 */
	private final ConcurrentFrameReceivingMap map;

	/**
	 * Request-storing concurrent map instance
	 *
	 */
	private final ConcurrentMap<SocketAddress, Request> requestMap;

	/**
	 * Infinite receive task constructor
	 *
	 * @param socket datagram socket instance
	 * @param requestMap concurrent request map instance
	 */
	public InfiniteReceiveTask(DatagramSocket socket, ConcurrentMap<SocketAddress, Request> requestMap)
	{
		this.logger = Logger.getLogger(InfiniteReceiveTask.class.getName());
		this.lock = new ReentrantLock();
		this.socket = socket;
		this.map = new ConcurrentFrameReceivingMap();
		this.requestMap = requestMap;
	}

	/**
	 * An infinite task body
	 *
	 */
	@Override
	public void run()
	{
//		Declaring incoming request bytes buffer
		byte[] incomingFrameBytes = new byte[NetworkUtils.REQUEST_BUFFER_SIZE * 2];
		DatagramPacket incomingRequestPacket = new DatagramPacket(incomingFrameBytes, incomingFrameBytes.length);

		while (true)
		{
			try
			{
				lock.lock();
				if (!socket.isClosed())
				{
					socket.receive(incomingRequestPacket);
				}
				lock.unlock();

	//			Mapping a current frame to instance from bytes
				UDPFrame currentFrame = FrameMapper.mapFromBytesToInstance(incomingRequestPacket.getData());

	//			Adding a frame into the frames map
				map.add(incomingRequestPacket.getSocketAddress(), currentFrame);

				for (Pair<SocketAddress, List<UDPFrame>> completedRequestFrameList : map.findCompletedRequestsFrameLists())
				{
					Request request = NetworkUtils.requestFromFrames(completedRequestFrameList.value());

	//				Put complete request into the completed requests map
					requestMap.put(completedRequestFrameList.key(), request);
				}
			}
			catch (IOException | MappingException | NetworkException e)
			{
				logger.log(Level.SEVERE, "Something went wrong during receiving", e);
			}
		}
	}
}
