package com.enzulode.network.model.transport;

import lombok.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * This class represents a piece of data sent over the UPD proto
 *
 * @param data The data
 * @param last The isLast flag
 */
public record UDPFrame(@NonNull byte[] data, boolean last) implements Serializable
{
	/**
     * Response serial version uid
     *
     */
    @Serial
    private static final long serialVersionUID = -2423240935234456363L;

	/**
	 * UDPFrame data getter
	 *
	 * @return data bytes
	 */
	@Override
	public byte[] data()
	{
		return data;
	}

	/**
	 * Last flag getter
	 *
	 * @return true if the frame is last and false otherwise
	 */
	@Override
	public boolean last()
	{
		return last;
	}
}
