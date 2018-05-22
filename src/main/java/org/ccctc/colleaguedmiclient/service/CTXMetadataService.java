package org.ccctc.colleaguedmiclient.service;

import lombok.NonNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiMetadataException;
import org.ccctc.colleaguedmiclient.model.CTXAssociation;
import org.ccctc.colleaguedmiclient.model.CTXElement;
import org.ccctc.colleaguedmiclient.model.CTXMetadata;
import org.ccctc.colleaguedmiclient.model.CTXVariable;
import org.ccctc.colleaguedmiclient.model.KeyValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ccctc.colleaguedmiclient.util.ArrayUtils.getAt;
import static org.ccctc.colleaguedmiclient.util.StringUtils.*;

/**
 * This service retrieves and caches metadata for Colleague Transactions, including variables, elements and associations.
 * <p>
 * To instantiate this object, you will need to first create a {@code DmiCTXService}. Note that the {@code DmiCTXService}
 * will first require that a {@code DmiService} is created.
 *
 * @see DmiService
 * @see DmiCTXService
 */
public class CTXMetadataService {

    private final static Log log = LogFactory.getLog(CTXMetadataService.class);
    private final static long DEFAULT_CACHE_EXPIRATION_SECONDS = 24 * 60 * 60;

    private final DmiCTXService dmiCTXService;
    private final MetadataCache<CTXMetadata> cache;


    /**
     * Create CTX Metadata Service
     *
     * @param dmiCTXService DMI Colleague Transaction Service
     */
    public CTXMetadataService(@NonNull DmiCTXService dmiCTXService) {
        this.dmiCTXService = dmiCTXService;
        this.cache = new MetadataCache<>(DEFAULT_CACHE_EXPIRATION_SECONDS);
    }


    /**
     * Get metadata for a Colleague Transaction
     *
     * @param appl            Application
     * @param transactionName Colleague Transaction Name
     * @return Entity Metadata
     */
    public CTXMetadata get(String appl, String transactionName) {
        return get(appl, transactionName, false);
    }


    /**
     * Get metadata for a Colleague Transaction, optionally forcing a refresh of the entry if its cached
     *
     * @param appl            Application
     * @param transactionName Colleague Transaction name
     * @param refreshCache    Refresh Cache
     * @return Entity Metadata
     */
    public CTXMetadata get(String appl, String transactionName, boolean refreshCache) {
        if (!refreshCache) {
            CTXMetadata cached = cache.get(appl, transactionName);
            if (cached != null) return cached;
        }

        List<KeyValuePair<String, String>> params = new ArrayList<>();
        params.add(new KeyValuePair<>("TV.APPLICATION", appl));
        params.add(new KeyValuePair<>("TV.PRCS.ID", transactionName));

        Map<String, String> m = dmiCTXService.executeRaw("UT", "GET.CTX.DETAILS", params);

        if (m != null) {
            String prcsAliasName = m.get("TV.PRCS.ALIAS.NAME");
            String isAnonymousCtx = m.get("TV.IS.ANONYMOUS.CTX");
            String prcsInquiryOnly = m.get("TV.PRCS.INQUIRY.ONLY");
            Integer prcsVersion = parseIntOrNull(m.get("TV.PRCS.VERSION"));

            String[] elementName = split(m.get("TV.ELEMENT.NAME"), VM);
            String[] pointsToFile = split(m.get("TV.POINTS.TO.FILE"), VM);
            String[] elementRequired = split(m.get("TV.ELEMENT.REQUIRED"), VM);
            String[] elementDirection = split(m.get("TV.ELEMENT.DIRECTION"), VM);
            String[] assocName = split(m.get("TV.ASSOC.NAME"), VM);
            String[] assocAliasName = split(m.get("TV.ASSOC.ALIAS.NAME"), VM);
            String[] assocRange = split(m.get("TV.ASSOC.RANGE"), VM);
            String[] varName = split(m.get("TV.VAR.NAME"), VM);
            String[] varAliasName = split(m.get("TV.VAR.ALIAS.NAME"), VM);
            String[] varRequired = split(m.get("TV.VAR.REQUIRED"), VM);
            String[] varDirection = split(m.get("TV.VAR.DIRECTION"), VM);
            String[] varDataType = split(m.get("TV.VAR.DATA.TYPE"), VM);
            String[] elementAliasName = split(m.get("TV.ELEMENT.ALIAS.NAME"), VM);
            String[] elementDispOnly = split(m.get("TV.ELEMENT.DISP.ONLY"), VM);
            String[] elementConv = split(m.get("TV.ELEMENT.CONV"), VM);
            String[] varConv = split(m.get("TV.VAR.CONV"), VM);
            String[] assocType = split(m.get("TV.ASSOC.TYPE"), VM);
            String[] assocMembers = split(m.get("TV.ASSOC.MEMBERS"), VM);
            String[] elementGroup = split(m.get("TV.ELEMENT.GROUP"), VM);
            String[] varGroup = split(m.get("TV.VAR.GROUP"), VM);
            String[] elementIsBool = split(m.get("TV.ELEMENT.IS.BOOL"), VM);
            String[] varIsBool = split(m.get("TV.VAR.IS.BOOL"), VM);
            String[] elementSize = split(m.get("TV.ELEMENT.SIZE"), VM);
            String[] varSize = split(m.get("TV.VAR.SIZE"), VM);
            String[] elementDataType = split(m.get("TV.ELEMENT.DATA.TYPE"), VM);
            String[] varIsUri = split(m.get("TV.VAR.IS.URI"), VM);

            List<CTXElement> elements = new ArrayList<>();
            List<CTXVariable> variables = new ArrayList<>();
            List<CTXAssociation> associations = new ArrayList<>();

            if (elementName != null) {
                for (int x = 0; x < elementName.length; x++) {
                    CTXElement e = CTXElement.builder()
                            .elementName(getAt(elementName, x))
                            .pointsToFile(getAt(pointsToFile, x))
                            .elementRequired(getAt(elementRequired, x))
                            .elementDirection(getAt(elementDirection, x))
                            .elementAliasName(getAt(elementAliasName, x))
                            .elementDispOnly(getAt(elementDispOnly, x))
                            .elementConv(getAt(elementConv, x))
                            .elementGroup(getAt(elementGroup, x))
                            .elementIsBool(getAt(elementIsBool, x))
                            .elementSize(getAt(elementSize, x))
                            .elementDataType(getAt(elementDataType, x))
                            .build();

                    elements.add(e);
                }
            }

            if (varName != null) {
                for (int x = 0; x < varName.length; x++) {
                    CTXVariable v = CTXVariable.builder()
                            .varName(getAt(varName, x))
                            .varAliasName(getAt(varAliasName, x))
                            .varRequired(getAt(varRequired, x))
                            .varDirection(getAt(varDirection, x))
                            .varDataType(getAt(varDataType, x))
                            .varConv(getAt(varConv, x))
                            .varGroup(getAt(varGroup, x))
                            .varIsBool(getAt(varIsBool, x))
                            .varSize(getAt(varSize, x))
                            .varIsUri(getAt(varIsUri, x))
                            .build();

                    variables.add(v);
                }
            }

            if (assocName != null) {
                for (int x = 0; x < assocName.length; x++) {
                    CTXAssociation a = CTXAssociation.builder()
                            .assocName(getAt(assocName, x))
                            .assocAliasName(getAt(assocAliasName, x))
                            .assocRange(getAt(assocRange, x))
                            .assocType(getAt(assocType, x))
                            .assocMembers(split(getAt(assocMembers, x), ','))
                            .build();

                    associations.add(a);
                }
            }


            CTXMetadata c = CTXMetadata.builder()
                    .prcsAliasName(prcsAliasName)
                    .isAnonymousCtx(isAnonymousCtx)
                    .prcsInquiryOnly(prcsInquiryOnly)
                    .prcsVersion(prcsVersion)
                    .variables(variables)
                    .elements(elements)
                    .associations(associations)
                    .build();

            cache.put(appl, transactionName, c);
            return c;
        }

        throw new DmiMetadataException("Error reading metadata - unexpected response from DMI");
    }


    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
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
}
