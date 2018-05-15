package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.NonNull;

/**
 * Single key DMI Transaction. This type of data request will instruct the DMI to select one or more
 * columns from a view based on a single key.
 */
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
    public SingleKeyRequest(@NonNull String account, @NonNull String token, @NonNull String controlId, @NonNull String sharedSecret,
                            @NonNull String viewName, @NonNull ViewType viewType, Iterable<String> columns, String key) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;

        super.addSubRequest(DataAccessType.SINGLEKEY, viewName, viewType, colNames, key);
        super.addHashSubRequest(sharedSecret);
    }
}