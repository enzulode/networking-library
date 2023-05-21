package com.enzulode.network.model.transport;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * This record represents a piece of data sent over the UPD proto
 *
 * @param data The data stored in UDPFrame
 * @param last This property defines is the frame last
 */
public record UDPFrame(byte[] data, boolean last) implements Serializable
{
	/**
	 * UDPFrame serial version uid
	 *
	 */
	@Serial
	private static final long serialVersionUID = -2423240935234456363L;

	/**
	 * This record represents a piece of data sent over the UPD proto
	 *
	 * @param data The data stored in UDPFrame
	 * @param last This property defines is the frame last
	 */
	public UDPFrame
	{
//		Requiring UDPFrame stored data to be non-null
		Objects.requireNonNull(data, "UDPFrame stored data cannot be null");
	}

	/**
	 * UDPFrame stored data getter
	 *
	 * @return data bytes array
	 */
	@Override
	public byte[] data()
	{
		return data;
	}

	/**
	 * Is UDPFrame last property getter
	 *
	 * @return true if the frame is last and false otherwise
	 */
	@Override
	public boolean last()
	{
		return last;
	}
}
