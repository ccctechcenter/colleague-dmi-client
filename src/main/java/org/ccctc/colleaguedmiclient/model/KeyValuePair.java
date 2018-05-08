package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

/**
 * Key / value pair
 * @param <K> Key Type
 * @param <V> Value Type
 */
@Data
public class KeyValuePair<K, V> {
    /**
     * Key
     */
    private final K key;

    /**
     * Value
     */
    private final V value;
}
