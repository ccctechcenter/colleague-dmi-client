package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.NonNull;

/**
 * Batch Select DMI Transaction. This type of data request will instruct the DMI to select one or more
 * columns from a view based on selection criteria. If no selection criteria is specified, all records in the table
 * will be returned.
 */
public class BatchSelectRequest extends DataRequest {

    /**
     * Create a batch select DMI Transaction. This type of data request will instruct the DMI to select one or more
     * columns from a view based on selection criteria. If no selection criteria is specified, all records in the table
     * will be returned.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param viewType     View type
     * @param columns      Columns
     * @param criteria     Selection criteria (or null for all records)
     */
    public BatchSelectRequest(@NonNull String account, @NonNull String token, @NonNull String controlId, @NonNull String sharedSecret,
                              @NonNull String viewName, @NonNull ViewType viewType, Iterable<String> columns, String criteria) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;

        super.addSubRequest(DataAccessType.BATCHSELECT, viewName, viewType, colNames, criteria);
        super.addHashSubRequest(sharedSecret);
    }
}