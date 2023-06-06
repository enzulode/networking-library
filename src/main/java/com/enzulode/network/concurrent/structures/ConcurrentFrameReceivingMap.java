package com.enzulode.network.concurrent.structures;

import com.enzulode.network.model.transport.UDPFrame;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This map is designed for udp frame receiving
 *
 */
public class ConcurrentFrameReceivingMap
{
	/**
	 * Concurrent map instance
	 *
	 */
	private final ConcurrentMap<SocketAddress, List<UDPFrame>> map;

	/**
	 * Map lock instance
	 *
	 */
	private final Lock lock;

	/**
	 * Concurrent frame receiving map constructor
	 *
	 */
	public ConcurrentFrameReceivingMap()
	{
		map = new ConcurrentHashMap<>();
		lock = new ReentrantLock();
	}

	/**
	 * This method puts a new udp frame into the map using sender {@link SocketAddress} as key
	 *
	 * @param address sender address
	 * @param frame frame to be added into the map
	 */
	public void add(SocketAddress address, UDPFrame frame)
	{
		if (map.containsKey(address) && map.get(address) != null)
		{
			map.get(address).add(frame);
			return;
		}

		lock.lock();
		List<UDPFrame> frames = new LinkedList<>();
		frames.add(frame);
		map.put(address, frames);
		lock.unlock();
	}

	/**
	 * This method finds a list of udp frames for a specific socket address
	 *
	 * @param address requested frames address
	 * @return unmodifiable list of udp frames
	 */
	public List<UDPFrame> findFramesByAddress(SocketAddress address)
	{
		return (map.containsKey(address) && map.get(address) != null)
				? Collections.unmodifiableList(map.get(address))
				: Collections.emptyList();
	}

	/**
	 * This method returns a list of {@link Pair}. Each pair contains a frames sender address and
	 * the list of frame referring to it
	 *
	 * @return a list of pairs of socket address and list frames referring to this specific address
	 */
	public List<Pair<SocketAddress, List<UDPFrame>>> findCompletedRequestsFrameLists()
	{
//		TODO: remove this informational debug block
		System.out.println(map);

		List<Pair<SocketAddress, List<UDPFrame>>> completedRequestsFramesList = new ArrayList<>();

		for (Iterator<Map.Entry<SocketAddress, List<UDPFrame>>> i = map.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry<SocketAddress, List<UDPFrame>> entry = i.next();

//			TODO: remove this informational debug block
			System.out.println(entry.getValue());

			if (validateFrameListCompleted(entry.getValue()))
			{
				lock.lock();
				completedRequestsFramesList.add(new Pair<>(entry.getKey(), entry.getValue()));
				i.remove();
				lock.unlock();
			}
		}

		return Collections.unmodifiableList(completedRequestsFramesList);
	}

	/**
	 * This method validates a list of frames for completion
	 *
	 * @param frames list of frames to be validated
	 * @return validation result. True if the last frame of the request contains frame that is marked as last
	 * and false otherwise
	 */
	private boolean validateFrameListCompleted(List<UDPFrame> frames)
	{
		return frames.get(frames.size() - 1).last();
	}
}
