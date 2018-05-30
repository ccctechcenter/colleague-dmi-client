package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valcode
 */
@Getter
@ToString
@AllArgsConstructor
public class Valcode {

    /**
     * Valcode key
     */
    private final String key;

    /**
     * Valcode entries
     */
    private final List<Entry> entries;

    /**
     * Get the entries as a map, indexed by internal code.
     * <p>
     * In the case of bad data:
     * 1. a null internal code is not included in the map
     * 2. duplicate entries are ignored (first value is kept)
     *
     * @return Map of entries
     */
    public Map<String, Entry> asMap() {
        Map<String, Entry> map = new HashMap<>();

        for(Entry e : entries) {
            String k = e.getInternalCode();
            if (k != null) map.putIfAbsent(k, e);
        }

        return map;
    }

    /**
     * Valcode entry
     */
    @Getter
    @ToString
    @AllArgsConstructor
    public static class Entry {
        /**
         * Internal Code
         */
        private final String internalCode;

        /**
         * External Representation
         */
        private final String externalRepresentation;

        /**
         * Action #1
         */
        private final String action1;

        /**
         * Action #2
         */
        private final String action2;
    }
}
