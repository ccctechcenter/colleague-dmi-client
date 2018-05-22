package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Key / value pair
 * @param <K> Key Type
 * @param <V> Value Type
 */
@Getter
@AllArgsConstructor
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
