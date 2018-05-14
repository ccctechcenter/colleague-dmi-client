package org.ccctc.colleaguedmiclient.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiMetadataException;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.EntityMetadata;
import org.ccctc.colleaguedmiclient.model.KeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ccctc.colleaguedmiclient.util.ArrayUtils.getAt;
import static org.ccctc.colleaguedmiclient.util.ArrayUtils.getAtInt;
import static org.ccctc.colleaguedmiclient.util.StringUtils.VM;
import static org.ccctc.colleaguedmiclient.util.StringUtils.split;

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

    private final static Log log = LogFactory.getLog(EntityMetadataService.class);
    private final static long DEFAULT_CACHE_EXPIRATION_SECONDS = 24 * 60 * 60;

    private final DmiCTXService dmiCTXService;
    private final MetadataCache<EntityMetadata> cache;


    /**
     * Create Entity Metadata Service
     *
     * @param dmiCTXService DMI Colleague Transaction Service
     */
    public EntityMetadataService(DmiCTXService dmiCTXService) {
        this.dmiCTXService = dmiCTXService;
        this.cache = new MetadataCache<>(DEFAULT_CACHE_EXPIRATION_SECONDS);
    }


    /**
     * Get number of seconds before a new cache entry will expire.
     *
     * @return Cache entry expiration time in seconds
     */
    public long getCacheExpirationSeconds() {
        return cache.getCacheExpirationSeconds();
    }


    /**
     * Set number of seconds before a new cache entry will expire.
     *
     * @param cacheExpirationSeconds Cache entry expiration time in seconds
     */
    public void setCacheExpirationSeconds(long cacheExpirationSeconds) {
        cache.setCacheExpirationSeconds(cacheExpirationSeconds);
    }


    /**
     * Get metadata for an Entity
     *
     * @param appl       Application
     * @param entityName Entity Name
     * @return Entity Metadata
     */
    public EntityMetadata get(String appl, String entityName) {
        return get(appl, entityName, false);
    }


    /**
     * Get metadata for an Entity, optionally forcing a refresh of the entry if its cached
     *
     * @param appl         Application
     * @param entityName   Entity name
     * @param refreshCache Refresh Cache
     * @return Entity Metadata
     */
    public EntityMetadata get(String appl, String entityName, boolean refreshCache) {
        if (!refreshCache) {
            EntityMetadata cached = cache.get(appl, entityName);
            if (cached != null) return cached;
        }

        List<KeyValuePair<String, String>> params = new ArrayList<>();
        params.add(new KeyValuePair<>("TV.APPLICATION", appl));
        params.add(new KeyValuePair<>("TV.ENTITY.NAME", entityName));

        Map<String, String> m = dmiCTXService.executeRaw("UT", "GET.ENTITY.METADATA", params);

        if (m != null) {

            String[] names = split(m.get("TV.CDD.NAME"), VM);
            String[] physNames = split(m.get("TV.PHYS.CDD.NAME"), VM);
            String[] sources = split(m.get("TV.SOURCE"), VM);
            String[] maxStorageSizes = split(m.get("TV.MAXIMUM.STORAGE.SIZE"), VM);
            String[] fieldPlacements = split(m.get("TV.FIELD.PLACEMENT"), VM);
            String[] usageTypes = split(m.get("TV.DATABASE.USAGE.TYPE"), VM);
            String[] defaultDisplaySize = split(m.get("TV.DEFAULT.DISPLAY.SIZE"), VM);
            String[] formatString = split(m.get("TV.INFORM.FORMAT.STRING"), VM);
            String[] conversionString = split(m.get("TV.INFORM.CONVERSION.STRING"), VM);
            String[] dataTypes = split(m.get("TV.DATA.TYPE"), VM);
            String[] assocNames = split(m.get("TV.ELEMENT.ASSOC.NAME"), VM);
            String[] assocTypes = split(m.get("TV.ELEMENT.ASSOC.TYPE"), VM);
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
                    throw new DmiMetadataException("Error reading metadata: " + e.getClass().getName() + ": " +
                            e.getMessage(), e);
                }
            }

            EntityMetadata entityMetadata = new EntityMetadata(entityType, guidEnabled, map, ordered);

            cache.put(appl, entityName, entityMetadata);
            return entityMetadata;

        }

        throw new DmiMetadataException("Error reading metadata - unexpected response from DMI");
    }
}