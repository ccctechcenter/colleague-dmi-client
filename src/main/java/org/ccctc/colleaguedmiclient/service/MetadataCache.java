package org.ccctc.colleaguedmiclient.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cache used for metadata from Colleague. Values in the cache are stored/retrieved by application name + "*" + value,
 * for example CORE*PERSON for an entity or UT*GET.SESSION.INFO for a CTX.
 *
 * @param <T> Type of Cache
 */
class MetadataCache<T> implements Map<String, T> {

    private Map<String, MetadataCache.Entry<T>> cache = new HashMap<>();

    /**
     * Number of seconds before a cache entry will expire. Default is 24 hours.
     */
    @Getter @Setter private long cacheExpirationSeconds;


    /**
     * Create a metadata cache with a given expiration time for entries
     *
     * @param cacheExpirationSeconds Cache entry expiration time in seconds
     */
    public MetadataCache(long cacheExpirationSeconds) {
        this.cacheExpirationSeconds = cacheExpirationSeconds;
    }


    /**
     * Get an entry from the cache or null if not found. If an entry has expired it will not be returned and it will
     * be removed from the Cache.
     *
     * @param appl Entry application
     * @param name Entry name
     * @return Entry
     */
    public T get(String appl, String name) {
        String key = appl + "*" + name;
        return get(key);
    }


    /**
     * Add an entry to the cache
     *
     * @param appl  Entry application
     * @param name  Entry name
     * @param entry Entry
     * @return Entry
     */
    public T put(String appl, String name, T entry) {
        String key = appl + "*" + name;
        return put(key, entry);
    }


    // Implemented methods of Map

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.entrySet().stream()
                .anyMatch(i -> (value == null && i == null || (value != null && value.equals(i.getValue()))));
    }

    @Override
    public T get(Object key) {
        Entry<T> entry = cache.get(key);
        if (entry != null) {
            if (!entry.isExpired()) {
                return entry.getValue();
            } else {
                cache.remove(key.toString());
            }
        }

        return null;
    }

    @Override
    public T put(String key, T value) {
        Entry<T> entry = new Entry<>(key, value, LocalDateTime.now().plusSeconds(cacheExpirationSeconds));
        cache.put(key, entry);
        return value;
    }

    @Override
    public T remove(Object key) {
        Entry<T> entry = cache.remove(key);
        if (entry != null) return entry.getValue();
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Set<String> keySet() {
        removeExpired();
        return cache.keySet();
    }

    @Override
    public Collection<T> values() {
        removeExpired();
        return cache.values().stream()
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<String, T>> entrySet() {
        removeExpired();
        return cache.entrySet().stream()
                .map(i -> new AbstractMap.SimpleEntry<>(i.getKey(), i.getValue().getValue()))
                .collect(Collectors.toSet());
    }


    /**
     * Remove expired values from the cache
     */
    private void removeExpired() {
        for (Map.Entry<String, MetadataCache.Entry<T>> e : cache.entrySet()) {
            if (e.getValue().isExpired())
                remove(e.getKey());
        }
    }


    /**
     * Cached entry with an expiration date/time
     */
    @AllArgsConstructor
    private static class Entry<T> {
        /**
         * Key of the entry in the format application + "*" + entry name. Example: CORE*PERSON.
         */
        private final String key;

        /**
         * Value of the entry
         */
        @Getter private final T value;

        /**
         * Expiration date and time of the entry
         */
        @Getter private final LocalDateTime expirationDateTime;

        boolean isExpired() {
            return !expirationDateTime.isAfter(LocalDateTime.now());
        }
    }
}
