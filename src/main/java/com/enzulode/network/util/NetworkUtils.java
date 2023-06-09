package com.enzulode.network.util;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.mapper.RequestMapper;
import com.enzulode.network.model.interconnection.Request;
import com.enzulode.network.model.transport.UDPFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for network interactions
 *
 */
public class NetworkUtils
{

	/**
	 * Default request buffer size
	 *
	 */
	public static final int REQUEST_BUFFER_SIZE = 1024 * 4;

	/**
	 * Default response buffer size
	 *
	 */
	public static final int RESPONSE_BUFFER_SIZE = 1024 * 4;

	/**
	 * This method divides an array of bytes into separate chunks
	 *
	 * @param array byte array to be divided
	 * @param chunk chunk size
	 * @return list of chunks
	 */
	public static List<byte[]> splitIntoChunks(byte[] array, int chunk)
	{
//		Requiring splittable array to be non-null
		Objects.requireNonNull(array, "Splittable array cannot be null");

		List<byte[]> chunks = new ArrayList<>();

//		Prevent excessive operations if chunk size is 1
		if (chunk == 1)
		{
			for (byte item : array)
				chunks.add(new byte[] { item });

			return chunks;
		}

//		Array should be simply wrapped with a list if chunk size is equal to array length
		if (chunk == array.length)
			return List.of(array);

//		Pointer initialization
		int pointer = 0;

		while(pointer <= array.length)
		{
			if (pointer == array.length)
				chunks.add(new byte[] {array[array.length - 1]});

			if (pointer < array.length && pointer + chunk > array.length)
				chunks.add(Arrays.copyOfRange(array, pointer, pointer + (array.length - pointer)));
			else
				chunks.add(Arrays.copyOfRange(array, pointer, pointer + chunk));

			pointer += chunk;
		}

		return chunks;
	}

	/**
	 * This method wraps chunks with frames ({@link UDPFrame})
	 *
	 * @param chunks request chunks
	 * @return list of {@link UDPFrame}
	 */
	public static List<UDPFrame> wrapChunksWithUDPFrames(List<byte[]> chunks)
	{
//		Requiring list of chunks to be non-null
		Objects.requireNonNull(chunks, "Byte chunks list cannot be null");

//		Getting request chunks from raw bytes
		List<UDPFrame> frames = new ArrayList<>();

//		Wrapping separate chunks into separate frames
		for (int i = 0; i < chunks.size() - 1; i++)
			frames.add(new UDPFrame(chunks.get(i), false));

		frames.add(new UDPFrame(chunks.get(chunks.size() - 1), true));
		return frames;
	}

	/**
	 * This method wraps every frame ({@link UDPFrame}) with {@link DatagramPacket}
	 *
	 * @param frames frames to be wrapped
	 * @param destination packet destination
	 * @return list of {@link DatagramPacket}
	 * @throws NetworkException if it's failed to map frames to bytes
	 */
	public static List<DatagramPacket> wrapUDPFramesWithDatagramPackets(
			List<UDPFrame> frames,
			InetSocketAddress destination
	) throws NetworkException
	{
//		Requiring list of frames and destination address to be non-null
		Objects.requireNonNull(frames, "List of frames cannot be null");
		Objects.requireNonNull(destination, "Destination inet address cannot be null");

		List<DatagramPacket> packets = new ArrayList<>();

		try
		{
			for (UDPFrame frame : frames)
			{
//				Mapping every frame to raw bytes
				byte[] frameBytes = FrameMapper.mapFromInstanceToBytes(frame);
//				Wrapping every frame byte array with datagram packet
				packets.add(new DatagramPacket(frameBytes, frameBytes.length, destination));
			}

			return packets;
		}
		catch (MappingException e)
		{
			throw new NetworkException(
					"Failed to wrap UDPFrames with datagram packets: mapping exception occurred", e
			);
		}
	}

	/**
	 * This method concatenates two byte arrays
	 *
	 * @param first first array to be concatenated
	 * @param second second array to be concatenated
	 * @return concatenation result
	 */
	public static byte[] concatTwoByteArrays(byte[] first, byte[] second)
	{
//		Requiring arrays to be non-null
		Objects.requireNonNull(first, "First array cannot be null");
		Objects.requireNonNull(second, "Second array cannot be null");

		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * Remap UDPFrames list to list of byte arrays
	 *
	 * @param frames list of UDPFrames to be unwrapped
	 * @return a list of byte arrays
	 * @throws NetworkException if some of the frames was not mapped to bytes
	 */
	public static List<byte[]> udpFramesToBytes(List<UDPFrame> frames) throws NetworkException
	{
//		Requiring list of frames to be non-null
		Objects.requireNonNull(frames, "List of frames cannot be null");

		List<byte[]> bytes = new ArrayList<>();

		try
		{
			for (UDPFrame frame : frames)
				bytes.add(FrameMapper.mapFromInstanceToBytes(frame));
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map frame to bytes", e);
		}

		return bytes;
	}

	/**
	 * This method makes a thread sleeping for a specified period of time
	 *
	 * @param timeout timeout in milliseconds
	 */
	public static void timeout(long timeout)
	{
		try
		{
			TimeUnit.MILLISECONDS.sleep(timeout);
		}
		catch (InterruptedException ignored)
		{
		}
	}

	/**
	 * This method maps the request from UDPFrames to a java instance
	 *
	 * @param frames list of udp frames
	 * @return a request instance
	 * @param <T> request type param
	 * @throws NetworkException if something went wrong during byte array output stream operations or during mapping operations
	 */
	public static <T extends Request> T requestFromFrames(List<UDPFrame> frames) throws NetworkException
	{
//		Check the frame list is not null
		Objects.requireNonNull(frames, "Frame list cannot be null");

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();)
		{
//			Byte arrays concatenation
			for (UDPFrame frame : frames)
				baos.writeBytes(frame.data());

			return RequestMapper.mapFromBytesToInstance(baos.toByteArray());
		}
		catch (IOException e)
		{
			throw new NetworkException("Failed to close byte array output stream", e);
		}
		catch (MappingException e)
		{
			throw new NetworkException("Failed to map request", e);
		}
	}

	/**
	 * This method wraps overheaded response bytes with datagram packets
	 *
	 * @param responseBytes overheaded response bytes
	 * @param destination response destination
	 * @return list of datagram packets
	 * @throws NetworkException if it's failed to wrap response frames with datagram packets
	 */
	public static List<DatagramPacket> getPacketsForOverheadedResponseBytes(byte[] responseBytes, InetSocketAddress destination) throws NetworkException
	{
//		Check that response bytes and response destination are not null
		Objects.requireNonNull(responseBytes, "Response bytes cannot be null");
		Objects.requireNonNull(destination, "Response destination cannot be null");

//		Get response chunks from rew response bytes
		List<byte[]> responseChunks = NetworkUtils.splitIntoChunks(responseBytes, NetworkUtils.RESPONSE_BUFFER_SIZE);

//		Wrap chunks with UDPFrames
		List<UDPFrame> udpFrames = NetworkUtils.wrapChunksWithUDPFrames(responseChunks);

		return NetworkUtils.wrapUDPFramesWithDatagramPackets(udpFrames, destination);
	}

}
