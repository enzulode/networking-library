package com.enzulode.network.util;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.exception.NetworkException;
import com.enzulode.network.mapper.FrameMapper;
import com.enzulode.network.model.transport.UDPFrame;
import lombok.NonNull;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	public static List<byte[]> splitIntoChunks(@NonNull byte[] array, int chunk)
	{
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
	public static List<UDPFrame> wrapChunksWithUDPFrames(@NonNull List<byte[]> chunks)
	{
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
			@NonNull List<UDPFrame> frames,
			@NonNull InetSocketAddress destination
	) throws NetworkException
	{
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
	public static byte[] concatTwoByteArrays(@NonNull byte[] first, @NonNull byte[] second)
	{
		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static List<byte[]> udpFramesToBytes(@NonNull List<UDPFrame> frames) throws NetworkException
	{
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

}
