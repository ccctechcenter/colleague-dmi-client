package org.ccctc.colleaguedmiclient.transaction.data;

import org.ccctc.colleaguedmiclient.util.StringUtils;

public class BatchKeysRequest extends DataRequest {

    /**
     * Create a batch keys DMI Transaction. This type of data request will instruct the DMI to select one or more
     * columns from a view based on a list of keys.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param viewType     View type
     * @param columns      Columns
     * @param keys         Keys
     */
    public BatchKeysRequest(String account, String token, String controlId, String sharedSecret, String viewName,
                       ViewType viewType, Iterable<String> columns, String[] keys) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;
        String criteria = (keys != null) ? String.join(Character.toString(StringUtils.SM), keys) : null;

        super.addSubRequest(DataAccessType.BATCHKEYS, viewName, viewType, null, colNames, criteria);
        super.addHashSubRequest(sharedSecret);
    }
}