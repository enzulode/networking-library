package com.enzulode.network.mapper;

import com.enzulode.network.exception.MappingException;
import com.enzulode.network.model.interconnection.Request;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.util.Objects;

/**
 * This class converts element request instance into a byte array and in an opposite way
 *
 */
public final class RequestMapper
{
    /**
	 * This method maps request instance into raw request bytes
	 *
	 * @param request request instance
	 * @return request raw bytes
	 * @throws MappingException if serialization not succeed
	 */
	public static byte[] mapFromInstanceToBytes(Request request) throws MappingException
	{
//		Requiring request instance to be non-null
		Objects.requireNonNull(request, "Request instance cannot be null");
		
		try
		{
			return SerializationUtils.serialize(request);
		}
		catch (SerializationException e)
		{
			throw new MappingException("Failed to map Request instance to bytes", e);
		}
	}

	/**
	 * This method maps raw request bytes into request instance
	 *
	 * @param bytes raw request bytes
	 * @return request instance
	 * @param <T> request type parameter
	 * @throws MappingException if deserialization not succeed
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Request> T mapFromBytesToInstance(byte[] bytes) throws MappingException
	{
//		Requiring request bytes to be non-null
		Objects.requireNonNull(bytes, "Request bytes array cannot be null");

		try
		{
			return SerializationUtils.deserialize(bytes);
		}
		catch (SerializationException e)
		{
			throw new MappingException("Failed to map Request bytes to instance", e);
		}
	}
}
