package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.NonNull;
import org.ccctc.colleaguedmiclient.util.StringUtils;

/**
 * Batch Keys DMI Transaction. This type of data request will instruct the DMI to select one or more
 * columns from a view based on a list of keys.
 */
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
    public BatchKeysRequest(@NonNull String account, @NonNull String token, @NonNull String controlId, @NonNull String sharedSecret,
                            @NonNull String viewName, @NonNull ViewType viewType, Iterable<String> columns, Iterable<String> keys) {
        super(account, token, controlId);
        String colNames = (columns != null) ? String.join(",", columns) : null;
        String criteria = (keys != null) ? String.join(Character.toString(StringUtils.SM), keys) : null;

        super.addSubRequest(DataAccessType.BATCHKEYS, viewName, viewType, colNames, criteria);
        super.addHashSubRequest(sharedSecret);
    }
}