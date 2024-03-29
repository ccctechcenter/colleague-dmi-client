package org.ccctc.colleaguedmiclient.service;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiServiceException;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.ColleagueData;
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable;
import org.ccctc.colleaguedmiclient.model.EntityMetadata;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
import org.ccctc.colleaguedmiclient.model.Valcode;
import org.ccctc.colleaguedmiclient.transaction.data.SelectRequest;
import org.ccctc.colleaguedmiclient.transaction.data.SelectResponse;
import org.ccctc.colleaguedmiclient.transaction.data.SingleKeyRequest;
import org.ccctc.colleaguedmiclient.transaction.data.ViewType;
import org.ccctc.colleaguedmiclient.util.CddUtils;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;
import org.ccctc.colleaguedmiclient.transaction.data.BatchKeysRequest;
import org.ccctc.colleaguedmiclient.transaction.data.DataResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving data from the DMI. This makes use of the DMI Service (to send/receive DMI transactions) and
 * the Entity Metadata Service (to map DMI results to actual field names).
 * <p>
 * To instantiate this object, you will need to create a {@code DmiService}, and a {@code EntityMetadataService}.
 * Note that the {@code EntityMetadataService} will also require creating a {@code DmiCTXService}.
 *
 * @see DmiService
 * @see DmiCTXService
 * @see EntityMetadataService
 */
public class DmiDataService {

    private final Log log = LogFactory.getLog(DmiDataService.class);
    private final DmiService dmiService;

    private final List<String> valcodeColumns = Arrays.asList("VAL.INTERNAL.CODE", "VAL.EXTERNAL.REPRESENTATION",
            "VAL.ACTION.CODE.1", "VAL.ACTION.CODE.2");

    private final List<String> elfTranslateColumns = Arrays.asList("ELFT.DESC", "ELFT.COMMENTS", "ELFT.ORIG.CODE.FIELD",
            "ELFT.ORIG.CODES", "ELFT.NEW.CODES", "ELFT.ACTION.CODES.1", "ELFT.ACTION.CODES.2");

    /**
     * Entity metadata service used to translate field names and data types from a response
     */
    @Getter private final EntityMetadataService entityMetadataService;

    /**
     * Batch size for DMI reads (max number of records to read at one time)
     */
    private static final int batchSize = 1000;

    /**
     * Create a DMI data service. This requires a DMI Service (to send/receive DMI transactions) and a DMI CTX
     * Service (to run CTX transactions). A default Entity Metadata Service created to map DMI results to actual
     * field names.
     *
     * @param dmiService    DMI Service
     * @param dmiCTXService DMI CTX Service
     */
    public DmiDataService(@NonNull DmiService dmiService, @NonNull DmiCTXService dmiCTXService) {
        this.dmiService = dmiService;
        this.entityMetadataService = new EntityMetadataService(dmiCTXService);
    }

    /**
     * Create a DMI data service. This requires a DMI Service (to send/receive DMI transactions) and an Entity
     * Metadata Service (to map DMI results to actual field names).
     *
     * @param dmiService            DMI Service
     * @param entityMetadataService Entity Metadata Service
     */
    public DmiDataService(@NonNull DmiService dmiService, @NonNull EntityMetadataService entityMetadataService) {
        this.dmiService = dmiService;
        this.entityMetadataService = entityMetadataService;
    }


    /**
     * Select a single record from a view by selection primary key. View type is assumed to be PHYS (physical).
     *
     * @param appl     Application
     * @param viewName View
     * @param columns  Columns
     * @param key      Primary key
     * @return Record
     */
    public ColleagueData singleKey(@NonNull String appl, @NonNull String viewName, @NonNull Iterable<String> columns,
                                   @NonNull String key) {
        return singleKey(appl, viewName, ViewType.PHYS, columns, key, null);
    }


    /**
     * Select a single record from a view by selection primary key.
     *
     * @param appl                Application
     * @param viewName            View
     * @param columns             Columns
     * @param viewType            View type
     * @param key                 Primary key
     * @param cddViewNameOverride View name override in CDD
     * @return Record
     */
    public ColleagueData singleKey(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                   @NonNull Iterable<String> columns, @NonNull String key, String cddViewNameOverride) {
        SessionCredentials creds = dmiService.getSessionCredentials();
        SingleKeyRequest request = new SingleKeyRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), viewName, viewType, columns, key);

        logSend("singleKey", viewName, columns, Collections.singleton(key), null);

        DmiTransaction dmiResponse = dmiService.send(request);
        List<ColleagueData> data = processResponse(dmiResponse, appl, viewName, columns, cddViewNameOverride);

        logReceive("singleKey", viewName, data.size());

        if (data.size() > 0) return data.get(0);

        return null;
    }

    /**
     * Select a list of records from a view by selection primary key(s). View type is assumed to be PHYS (physical).
     * <p>
     * For larger requests, records are read in batches as large read requsts can overwhelm and even crash the DMI.
     *
     * @param appl     Application
     * @param viewName View
     * @param columns  Columns
     * @param keys     Primary keys
     * @return List of records
     */
    public List<ColleagueData> batchKeys(@NonNull String appl, @NonNull String viewName, @NonNull Iterable<String> columns,
                                         @NonNull Iterable<String> keys) {
        return batchKeys(appl, viewName, ViewType.PHYS, columns, keys, null);
    }


    /**
     * Select a list of records from a view by selection primary key(s).
     * <p>
     * For larger requests, records are read in batches as large read requsts can overwhelm and even crash the DMI.
     *
     * @param appl                Application
     * @param viewName            View
     * @param columns             Columns
     * @param viewType            View type
     * @param keys                Primary keys
     * @param cddViewNameOverride View name override in CDD
     * @return List of records
     */
    public List<ColleagueData> batchKeys(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                         @NonNull Iterable<String> columns, @NonNull Iterable<String> keys,
                                         String cddViewNameOverride) {

        List<String> keysList = (keys instanceof List) ? (List) keys : IteratorUtils.toList(keys.iterator());

        if (keysList.size() == 0)
            return new ArrayList<>();

        SessionCredentials creds = dmiService.getSessionCredentials();

        if (keysList.size() < batchSize) {
            BatchKeysRequest request = new BatchKeysRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                    dmiService.getSharedSecret(), viewName, viewType, columns, keys);

            logSend("batchKeys", viewName, columns, keys, null);

            DmiTransaction dmiReponse = dmiService.send(request);
            List<ColleagueData> data = processResponse(dmiReponse, appl, viewName, columns, cddViewNameOverride);

            logReceive("batchKeys", viewName, data.size());

            return data;
        } else {
            List<ColleagueData> result = new ArrayList<>();

            // process in batches
            for (int x = 0; x < keysList.size(); x += batchSize) {
                int to = x + batchSize;
                if (to > keysList.size()) to = keysList.size();

                List<String> keysSubList = keysList.subList(x, to);

                BatchKeysRequest request = new BatchKeysRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                        dmiService.getSharedSecret(), viewName, viewType, columns, keysSubList);

                logSend("batchKeys", viewName, columns, keysSubList, null);

                DmiTransaction dmiReponse = dmiService.send(request);

                List<ColleagueData> data = processResponse(dmiReponse, appl, viewName, columns, cddViewNameOverride);

                logReceive("batchKeys", viewName, data.size());

                result.addAll(data);
            }

            return result;
        }
    }


    /**
     * Select a list of records from a view by selection criteria. View type is assumed to be PHYS (physical).
     * <p>
     * If no selection criteria is specified, all records from the table will be returned.
     * <p>
     * Note: Selection criteria is in UniQuery syntax (which is standard for Colleague), not SQL.
     * <p>
     * For larger requests, records are read in batches as large read requests can overwhelm and even crash the DMI.
     *
     * @param appl     Application
     * @param viewName View
     * @param columns  Columns
     * @param criteria Selection criteria
     * @return List of records
     */
    public List<ColleagueData> batchSelect(@NonNull String appl, @NonNull String viewName,
                                           @NonNull Iterable<String> columns, String criteria) {
        return batchSelect(appl, viewName, ViewType.PHYS, columns, criteria, null);
    }


    /**
     * Select a list of records from a view by selection criteria.
     * <p>
     * If no selection criteria is specified, all records from the table will be returned.
     * <p>
     * Note: Selection criteria is in UniQuery syntax (which is standard for Colleague), not SQL.
     * <p>
     * For larger requests, records are read in batches as large read requests can overwhelm and even crash the DMI.
     *
     * @param appl                Application
     * @param viewName            View
     * @param viewType            View type
     * @param columns             Columns
     * @param criteria            Selection criteria
     * @param cddViewNameOverride View name override in CDD
     * @return List of records
     */
    public List<ColleagueData> batchSelect(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                           @NonNull Iterable<String> columns, String criteria, String cddViewNameOverride) {

        // get keys
        String[] keys = selectKeys(viewName, criteria);
        Iterable<String> keysIterable = () -> Arrays.stream(keys).iterator();
        return batchKeys(appl, viewName, viewType, columns, keysIterable, cddViewNameOverride);

        ///
        /// Due to the capability of overwhelming the server with a large select request, this functionality has been removed
        /// in favor of selecting the keys up front and processing them in batches.
        ///

        /*

        SessionCredentials creds = dmiService.getSessionCredentials();
        BatchSelectRequest request = new BatchSelectRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), viewName, viewType, columns, criteria);

        DmiTransaction dmiResponse = dmiService.send(request);
        return processResponse(dmiResponse, appl, viewName, columns);

        */
    }


    /**
     * Get all primary keys to a view.
     *
     * @param viewName View
     * @return List of keys
     */
    public String[] selectKeys(@NonNull String viewName) {
        return selectKeys(viewName, null, null);
    }


    /**
     * Get a list of primary keys to a view based on selection criteria.
     *
     * @param viewName View
     * @param criteria Criteria
     * @return List of keys
     */
    public String[] selectKeys(@NonNull String viewName, String criteria) {
        return selectKeys(viewName, criteria, null);
    }


    /**
     * Get a list of primary keys to a view based on selection criteria and optionally limiting the results to a list
     * of keys.
     *
     * @param viewName     View
     * @param criteria     Criteria
     * @param limitingKeys Limiting keys
     * @return List of keys
     */
    public String[] selectKeys(@NonNull String viewName, String criteria, Iterable<String> limitingKeys) {
        SessionCredentials creds = dmiService.getSessionCredentials();
        SelectRequest request = new SelectRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), viewName, criteria, limitingKeys);

        logSend("selectKeys", viewName, null, null, criteria);

        DmiTransaction dmiResponse = dmiService.send(request);
        SelectResponse selectResponse = SelectResponse.fromDmiTransaction(dmiResponse);

        logReceive("selectKeys", viewName, selectResponse.getKeys().length);

        return selectResponse.getKeys();
    }


    /**
     * Get a valcode record
     *
     * @param appl Application
     * @param key  Valcode key
     * @return Valcode
     */
    public Valcode valcode(String appl, String key) {
        ColleagueData data = singleKey(appl, appl + ".VALCODES", valcodeColumns, key);
        if (data != null) return processValcode(data);
        return null;
    }


    /**
     * Get multiple valcode records
     *
     * @param appl Application
     * @param keys Valcode keys
     * @return Valcodes
     */
    public List<Valcode> valcodes(String appl, Iterable<String> keys) {
        List<ColleagueData> data = batchKeys(appl, appl + ".VALCODES", valcodeColumns, keys);

        if (data != null) {
            List<Valcode> result = new ArrayList<>();

            for (ColleagueData d : data) {
                Valcode v = processValcode(d);
                if (v != null) result.add(v);
            }

            return result;
        }

        return null;
    }


    /**
     * Get an ELF Translation Table record
     *
     * @param key  ELF Translation Table key
     * @return ELF Translation Table
     */
    public ElfTranslateTable elfTranslationTable(String key) {
        ColleagueData data = singleKey("CORE", "ELF.TRANSLATE.TABLES", elfTranslateColumns, key);
        if (data != null) return processElfTranslationTable(data);
        return null;
    }


    /**
     * Get multiple ELF Translation Table records
     *
     * @param keys ELF Translation Table keys
     * @return ELF Translation Tables
     */
    public List<ElfTranslateTable> elfTranslationTables(Iterable<String> keys) {
        List<ColleagueData> data = batchKeys("CORE", "ELF.TRANSLATE.TABLES", elfTranslateColumns, keys);

        if (data != null) {
            List<ElfTranslateTable> result = new ArrayList<>();

            for (ColleagueData d : data) {
                ElfTranslateTable v = processElfTranslationTable(d);
                if (v != null) result.add(v);
            }

            return result;
        }

        return null;
    }

    /**
     * Process a DMI response for this data request. The result is a list of records with the field names
     * and data types mapped based on the entity metadata from EntityMetadataService.
     *
     * @param dmiResponse DMI response
     * @param appl        Application
     * @param viewName
     * @param columns     Columns
     * @param cddViewNameOverride View name override in CDD
     * @return List of records
     */
    private List<ColleagueData> processResponse(DmiTransaction dmiResponse, String appl, String viewName,
                                                Iterable<String> columns, String cddViewNameOverride) {

        List<ColleagueData> result = new ArrayList<>();

        if (cddViewNameOverride == null) {
            if (viewName.length() >= appl.length() + 1 && (appl + ".").equals(viewName.substring(0, appl.length() + 1)))
                cddViewNameOverride = "appl." + viewName.substring(appl.length() + 1);
            else
                cddViewNameOverride = viewName;
        }

        EntityMetadata entityMetadata = entityMetadataService.get(appl, cddViewNameOverride);
        DataResponse dataResponse = DataResponse.fromDmiTransaction(dmiResponse);

        if (dataResponse.getOrder().size() > 0) {

            if (entityMetadata == null)
                throw new DmiServiceException("No entity information found for " + appl + "." + viewName);

            // match field names
            for (String key : dataResponse.getOrder()) {
                String[] record = dataResponse.getData().get(key);

                Map<String, Object> values = new HashMap<>();
                for (String column : columns) {
                    CddEntry cddEntry = entityMetadata.getEntries().get(column);

                    if (cddEntry == null)
                        throw new DmiServiceException("Invalid field requested: " + column + " for " + appl + "." + viewName);

                    Object value = mapField(record, cddEntry);
                    values.put(column, value);
                }

                result.add(new ColleagueData(key, values));
            }
        }

        return result;
    }


    /**
     * Map a field from a record from the DMI response based on a CDD entry
     * <p>
     * The following data types are possible: String, Long, Integer, BigDecimal, LocalDate, LocalTime
     *
     * @param record   Record from the DMI response. This includes all fields from the response.
     * @param cddEntry CDD Entry of the field we want to map
     * @return Mapped field
     */
    private Object mapField(String[] record, CddEntry cddEntry) {
        String stringValue = null;

        // find value in record based on field placement from CDD entry
        Integer placement = cddEntry.getFieldPlacement();
        if (placement != null && record.length >= placement) {
            stringValue = record[placement - 1];
            if ("".equals(stringValue)) stringValue = null;
        }

        if (stringValue != null)
            return CddUtils.convertToValue(stringValue, cddEntry);

        return null;
    }


    /**
     * Process Valcode data
     *
     * @param data Data
     * @return Valcode
     */
    private Valcode processValcode(ColleagueData data) {
        if (data != null) {
            List<Valcode.Entry> entries = new ArrayList<>();

            String[] internalCodes = (String[]) data.getValues().get("VAL.INTERNAL.CODE");
            String[] externalRepresentations = (String[]) data.getValues().get("VAL.EXTERNAL.REPRESENTATION");
            String[] action1s = (String[]) data.getValues().get("VAL.ACTION.CODE.1");
            String[] action2s = (String[]) data.getValues().get("VAL.ACTION.CODE.2");

            int count = (internalCodes != null) ? internalCodes.length : 0;
            for (int x = 0; x < count; x++) {
                String i = internalCodes[x];
                String e = (externalRepresentations != null && externalRepresentations.length > x) ? externalRepresentations[x] : null;
                String a1 = (action1s != null && action1s.length > x) ? action1s[x] : null;
                String a2 = (action2s != null && action2s.length > x) ? action2s[x] : null;
                Valcode.Entry entry = new Valcode.Entry(i, e, a1, a2);
                entries.add(entry);
            }

            return new Valcode(data.getKey(), entries);
        }

        return null;
    }


    /**
     * Process ELF Translation Table data
     *
     * @param data Data
     * @return ELF Translation Table
     */
    private ElfTranslateTable processElfTranslationTable(ColleagueData data) {
        if (data != null) {
            List<ElfTranslateTable.Entry> translations = new ArrayList<>();

            String description = (String) data.getValues().get("ELFT.DESC");
            String origCodeField = (String) data.getValues().get("ELFT.ORIG.CODE.FIELD");
            String newCodeField = (String) data.getValues().get("ELFT.NEW.CODE.FIELD");
            String[] comments = (String[]) data.getValues().get("ELFT.COMMENTS");
            String[] originalCodes = (String[]) data.getValues().get("ELFT.ORIG.CODES");
            String[] newCodes = (String[]) data.getValues().get("ELFT.NEW.CODES");
            String[] action1s = (String[]) data.getValues().get("ELFT.ACTION.CODES.1");
            String[] action2s = (String[]) data.getValues().get("ELFT.ACTION.CODES.2");

            int count = (originalCodes != null) ? originalCodes.length : 0;
            for (int x = 0; x < count; x++) {
                String o = originalCodes[x];
                String n = (newCodes != null && newCodes.length > x) ? newCodes[x] : null;
                String a1 = (action1s != null && action1s.length > x) ? action1s[x] : null;
                String a2 = (action2s != null && action2s.length > x) ? action2s[x] : null;
                ElfTranslateTable.Entry entry = new ElfTranslateTable.Entry(o, n, a1, a2);
                translations.add(entry);
            }

            List<String> c = (comments != null) ? Arrays.asList(comments) : null;

            return new ElfTranslateTable(data.getKey(), description, c, origCodeField, newCodeField, translations);
        }

        return null;
    }


    /**
     * Logging prior to sending data to the DMI
     */
    private void logSend(String type, String table, Iterable<String> columns, Iterable<String> keys, String criteria) {
        if (log.isInfoEnabled()) {
            String c = (columns != null ? String.join(",", columns) : "(none)");
            String k = (keys != null ? String.join(",", keys) : "(none)");

            if (c.length() > 97) c = c.substring(0, 97) + "...";
            if (k.length() > 97) k = k.substring(0, 97) + "...";

            log.info("Sending DMI Data Request.   Type = " + type + ", table = " + table
                    + ", columns = " + c + ", keys = " + k
                    + ", criteria = " + criteria);
        }
    }


    /**
     * Logging after receiving data from the DMI
     */
    private void logReceive(String type, String table, int recordCount) {
        if (log.isInfoEnabled())
            log.info("Received DMI Data Response. Type = " + type + ", table = " + table
                    + ", records = " + recordCount);
    }
}