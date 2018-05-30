package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ELF Translation Table
 */
@Getter
@ToString
@AllArgsConstructor
public class ElfTranslateTable {

    /**
     * ELF Translate Table key
     */
    private final String key;

    /**
     * Description
     */
    private final String description;

    /**
     * Comments
     */
    private final List<String> comments;

    /**
     * Original field codes
     */
    private final String origCodeField;

    /**
     * New field codes
     */
    private final String newCodeField;

    /**
     * ELF Translations
     */
    private final List<Entry> translations;

    /**
     * Get the translations as a map, indexed by original code code.
     * <p>
     * Notes:
     * 1. a null internal code is stored in the map as an empty string instead
     * 2. duplicate entries are not allowed, but if present (ie bad data) are ignored (first value is kept)
     *
     * @return Map of translations
     */
    public Map<String, Entry> asMap() {
        Map<String, Entry> map = new HashMap<>();

        for(Entry e : translations) {
            String k = (e.getOriginalCode() == null) ? "" : e.getOriginalCode();
            map.putIfAbsent(k, e);
        }

        return map;
    }

    /**
     * ELF Translation Table entry
     */
    @Getter
    @ToString
    @AllArgsConstructor
    public static class Entry {
        /**
         * Original code, or an empty string for a null value
         */
        private final String originalCode;

        /**
         * External Representation
         */
        private final String newCode;

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
