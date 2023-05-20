package com.enzulode.network.model.interconnection.util;

/**
 * Possible response codes
 */
public enum ResponseCode
{
    /**
     * If previous request succeed - you have to send this response code
     *
     */
    SUCCEED,

    /**
     * If previous request failed - you have to send this response code
     *
     */
	FAILED;
}
