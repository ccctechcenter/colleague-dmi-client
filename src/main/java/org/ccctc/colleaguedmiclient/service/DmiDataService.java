package org.ccctc.colleaguedmiclient.service;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiServiceException;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.ColleagueData;
import org.ccctc.colleaguedmiclient.model.EntityMetadata;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
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
        return singleKey(appl, viewName, ViewType.PHYS, columns, key);
    }


    /**
     * Select a single record from a view by selection primary key.
     *
     * @param appl     Application
     * @param viewName View
     * @param columns  Columns
     * @param viewType View type
     * @param key      Primary key
     * @return Record
     */
    public ColleagueData singleKey(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                   @NonNull Iterable<String> columns, @NonNull String key) {
        SessionCredentials creds = dmiService.getSessionCredentials();
        SingleKeyRequest request = new SingleKeyRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), viewName, viewType, columns, key);

        logSend("singleKey", viewName, columns, Collections.singleton(key), null);

        DmiTransaction dmiResponse = dmiService.send(request);
        List<ColleagueData> data = processResponse(dmiResponse, appl, viewName, columns);

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
        return batchKeys(appl, viewName, ViewType.PHYS, columns, keys);
    }


    /**
     * Select a list of records from a view by selection primary key(s).
     * <p>
     * For larger requests, records are read in batches as large read requsts can overwhelm and even crash the DMI.
     *
     * @param appl     Application
     * @param viewName View
     * @param columns  Columns
     * @param viewType View type
     * @param keys     Primary keys
     * @return List of records
     */
    public List<ColleagueData> batchKeys(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                         @NonNull Iterable<String> columns, @NonNull Iterable<String> keys) {

        List<String> keysList = (keys instanceof List) ? (List) keys : IteratorUtils.toList(keys.iterator());

        if (keysList.size() == 0)
            return new ArrayList<>();

        SessionCredentials creds = dmiService.getSessionCredentials();

        if (keysList.size() < batchSize) {
            BatchKeysRequest request = new BatchKeysRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                    dmiService.getSharedSecret(), viewName, viewType, columns, keys);

            logSend("batchKeys", viewName, columns, keys, null);

            DmiTransaction dmiReponse = dmiService.send(request);
            List<ColleagueData> data = processResponse(dmiReponse, appl, viewName, columns);

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

                List<ColleagueData> data = processResponse(dmiReponse, appl, viewName, columns);

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
        return batchSelect(appl, viewName, ViewType.PHYS, columns, criteria);
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
     * @param appl     Application
     * @param viewName View
     * @param viewType View type
     * @param columns  Columns
     * @param criteria Selection criteria
     * @return List of records
     */
    public List<ColleagueData> batchSelect(@NonNull String appl, @NonNull String viewName, @NonNull ViewType viewType,
                                           @NonNull Iterable<String> columns, String criteria) {

        // get keys
        String[] keys = selectKeys(viewName, criteria);
        Iterable<String> keysIterable = () -> Arrays.stream(keys).iterator();
        return batchKeys(appl, viewName, viewType, columns, keysIterable);

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

        DmiTransaction dmiReponse = dmiService.send(request);
        SelectResponse selectResponse = SelectResponse.fromDmiTransaction(dmiReponse);

        logReceive("selectKeys", viewName, selectResponse.getKeys().length);

        return selectResponse.getKeys();
    }


    /**
     * Process a DMI response for this data request. The result is a list of records with the field names
     * and data types mapped based on the entity metadata from EntityMetadataService.
     *
     * @param dmiResponse DMI response
     * @param appl        Application
     * @param viewName    View name
     * @param columns     Columns
     * @return List of records
     */
    private List<ColleagueData> processResponse(DmiTransaction dmiResponse, String appl, String viewName,
                                                Iterable<String> columns) {

        List<ColleagueData> result = new ArrayList<>();

        String entityName = viewName;
        if (viewName.length() >= appl.length() + 1 && (appl + ".").equals(viewName.substring(0, appl.length() + 1)))
            entityName = "appl." + viewName.substring(appl.length() + 1);

        EntityMetadata entityMetadata = entityMetadataService.get(appl, entityName);
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