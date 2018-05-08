package org.ccctc.colleaguedmiclient.transaction.data;

public class SingleKeyRequest extends DataRequest {

    /**
     * Create a single key DMI Transaction. This type of data request will instruct the DMI to select one or more
     * columns from a view based on a single key.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param viewType     View type
     * @param columns      Columns
     * @param key          Key
     */
    public SingleKeyRequest(String account, String token, String controlId, String sharedSecret, String viewName,
                            ViewType viewType, Iterable<String> columns, String key) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;

        super.addSubRequest(DataAccessType.SINGLEKEY, viewName, viewType, null, colNames, key);
        super.addHashSubRequest(sharedSecret);
    }
}