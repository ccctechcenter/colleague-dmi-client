package org.ccctc.colleaguedmiclient.transaction.data;

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
    public BatchSelectRequest(String account, String token, String controlId, String sharedSecret, String viewName,
                              ViewType viewType, Iterable<String> columns, String criteria) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;

        super.addSubRequest(DataAccessType.BATCHSELECT, viewName, viewType, null, colNames, criteria);
        super.addHashSubRequest(sharedSecret);
    }
}