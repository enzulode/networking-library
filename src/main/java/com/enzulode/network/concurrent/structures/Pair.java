package com.enzulode.network.concurrent.structures;

/**
 * Special record for paired elements containing
 *
 * @param key pair key
 * @param value pair value
 * @param <K> pair key type
 * @param <V> pair value type
 */
public record Pair<K, V>(K key, V value)
{
}
