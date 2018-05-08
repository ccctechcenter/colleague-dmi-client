package org.ccctc.colleaguedmiclient.service;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.EntityMetadata;
import org.ccctc.colleaguedmiclient.model.KeyValuePair;
import org.ccctc.colleaguedmiclient.util.StringUtils;
import org.ccctc.colleaguedmiclient.transaction.ctx.CTXResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create an Entity Metadata Service. This service retrieves metadata for tables/views from Colleague, including column
 * names, data format, presentation, etc.
 * <p>
 * To instantiate this object, you will need to first create a {@code DmiCTXService}. Note that the {@code DmiCTXService}
 * will first require that a {@code DmiService} is created.
 *
 * @see DmiService
 * @see DmiCTXService
 */
public class EntityMetadataService {

    private final DmiCTXService dmiCTXService;
    private final Map<String, CacheEntry> cache;

    /**
     * Number of seconds before a cache entry will expire. Default is 24 hours.
     */
    @Getter @Setter private long cacheExpirationSeconds = 24 * 60 * 60;

    /**
     * Create Entity Metadata Service
     *
     * @param dmiCTXService DMI Colleague Transaction Service
     */
    public EntityMetadataService(DmiCTXService dmiCTXService) {
        this.dmiCTXService = dmiCTXService;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Get metadata for an Entity
     *
     * @param appl       Application
     * @param entityName Entity Name
     * @return Entity Metadata
     */
    public EntityMetadata GetEntity(String appl, String entityName) {
        return GetEntity(appl, entityName, false);
    }

    /**
     * Get metadata for an Entity, optionally forcing a refresh of the entry if its cached
     *
     * @param appl         Application
     * @param entityName   Entity name
     * @param refreshCache Refresh Cache
     * @return Entity Metadata
     */
    public EntityMetadata GetEntity(String appl, String entityName, boolean refreshCache) {
        if (!refreshCache) {
            EntityMetadata cached = cacheGet(appl, entityName);
            if (cached != null) return cached;
        }

        List<KeyValuePair<String, String>> params = new ArrayList<>();
        params.add(new KeyValuePair<>("TV.APPLICATION", appl));
        params.add(new KeyValuePair<>("TV.ENTITY.NAME", entityName));

        CTXResponse response = dmiCTXService.execute("UT", "GET.ENTITY.METADATA", params);

        if (response != null) {

            Map<String, String> m = response.getResponse();

            String[] names = splitAtVM(m.get("TV.CDD.NAME"));
            String[] physNames = splitAtVM(m.get("TV.PHYS.CDD.NAME"));
            String[] sources = splitAtVM(m.get("TV.SOURCE"));
            String[] maxStorageSizes = splitAtVM(m.get("TV.MAXIMUM.STORAGE.SIZE"));
            String[] fieldPlacements = splitAtVM(m.get("TV.FIELD.PLACEMENT"));
            String[] usageTypes = splitAtVM(m.get("TV.DATABASE.USAGE.TYPE"));
            String[] defaultDisplaySize = splitAtVM(m.get("TV.DEFAULT.DISPLAY.SIZE"));
            String[] formatString = splitAtVM(m.get("TV.INFORM.FORMAT.STRING"));
            String[] conversionString = splitAtVM(m.get("TV.INFORM.CONVERSION.STRING"));
            String[] dataTypes = splitAtVM(m.get("TV.DATA.TYPE"));
            String[] assocNames = splitAtVM(m.get("TV.ELEMENT.ASSOC.NAME"));
            String[] assocTypes = splitAtVM(m.get("TV.ELEMENT.ASSOC.TYPE"));
            String entityType = m.get("TV.ENTITY.TYPE");
            String guidEnabled = m.get("TV.GUID.ENABLED");

            int maxFieldPlacement = 0;
            for (int y = 0; y < fieldPlacements.length; y++) {
                Integer p = getAtInt(fieldPlacements, y);
                if (p != null && p > maxFieldPlacement) maxFieldPlacement = p;
            }

            Map<String, CddEntry> map = new HashMap<>();
            CddEntry[] ordered = new CddEntry[maxFieldPlacement];

            for (int x = 0; x < names.length; x++) {
                if (names[x] == null || names[x].equals("")) continue;

                try {
                    CddEntry.CddEntryBuilder b = CddEntry.builder();

                    CddEntry e = b.name(getAt(names, x))
                            .physName(getAt(physNames, x))
                            .source(getAt(sources, x))
                            .maximumStorageSize(getAtInt(maxStorageSizes, x))
                            .fieldPlacement(getAtInt(fieldPlacements, x))
                            .databaseUsageType(getAt(usageTypes, x))
                            .defaultDisplaySize(getAt(defaultDisplaySize, x))
                            .informFormatString(getAt(formatString, x))
                            .informConversionString(getAt(conversionString, x))
                            .dataType(getAt(dataTypes, x))
                            .elementAssocName(getAt(assocNames, x))
                            .elementAssocType(getAt(assocTypes, x))
                            .build();

                    map.put(names[x], e);

                    Integer placement = getAtInt(fieldPlacements, x);
                    if (placement != null) ordered[placement - 1] = e;
                } catch (NumberFormatException e) {
                    throw new DmiTransactionException("Error reading metadata - " + e.getClass().getName() + ": " +
                            e.getMessage());
                }
            }

            EntityMetadata entityMetadata = new EntityMetadata(entityType, guidEnabled, map, ordered);

            cacheAdd(appl, entityName, entityMetadata);
            return entityMetadata;

        }

        // @TODO - throw error for null response
        return null;
    }


    /**
     * Get an entry from the cache or null if not found. If an entry has expired it will not be returned - additionally
     * it will be removed from the cache.
     *
     * @param appl       Application
     * @param entityName Entity Name
     * @return Entity Metadata
     */
    private EntityMetadata cacheGet(String appl, String entityName) {
        String key = appl + "*" + entityName;
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            if (entry.expirationDateTime.isAfter(LocalDateTime.now())) {
                return entry.value;
            } else {
                cache.remove(key);
            }
        }

        return null;
    }


    /**
     * Add an entry to the cache
     *
     * @param appl           Application
     * @param entityName     Entity Name
     * @param entityMetadata Entity Metadata
     */
    private void cacheAdd(String appl, String entityName, EntityMetadata entityMetadata) {
        String key = appl + "*" + entityName;
        CacheEntry c = new CacheEntry(key, entityMetadata, LocalDateTime.now().plusSeconds(cacheExpirationSeconds));
        cache.put(appl + "*" + entityName, c);
    }


    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
    }


    /**
     * Utility for this class that gets a value from a string array, but only if the array is large enough and only
     * if the value is not an empty string. Otherwise null is returned.
     *
     * @param array String array
     * @param index Index in array
     * @return String
     */
    private String getAt(String[] array, int index) {
        if (array.length <= index) return null;
        String q = array[index];
        if ("".equals(q)) return null;
        return q;
    }


    /**
     * Utility for this class like with the same functionality as {@code getAt} but that additionally parses teh value
     * and returns an integer value. If the value is empty or is beyond the size of the array null is returned. If the
     * value cannot be parsed into an integer, {@code NumberFormatException} is thrown.
     *
     * @param array String array
     * @param index Index in array
     * @return Integer
     * @throws NumberFormatException if the string can't be parsed into an integer
     */
    private Integer getAtInt(String[] array, int index) throws NumberFormatException {
        if (array.length <= index) return null;
        String q = array[index];
        if ("".equals(q)) return null;

        return Integer.parseInt(q);
    }

    /**
     * Utility for this class that splits a String value delimited by VM into an array. If the string is null, an
     * empty array is returned.
     *
     * @param val String
     * @return String Array
     */
    private String[] splitAtVM(String val) {
        if (val == null) return new String[0];
        return StringUtils.split(val, StringUtils.VM);
    }


    /**
     * Cache Entry class
     */
    @Data
    private static class CacheEntry {
        /**
         * Key of entry in the format application + "*" + entity name, ie CORE*PERSON
         */
        private final String key;

        /**
         * Entity Metadata
         */
        private final EntityMetadata value;

        /**
         * Expiration date and time
         */
        private final LocalDateTime expirationDateTime;
    }
}