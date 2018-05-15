package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.NonNull;
import org.ccctc.colleaguedmiclient.util.StringUtils;

/**
 * Select DMI Transaction. This type of data request will instruct the DMI to select a list of keys from
 * a view based on selection criteria. If no selection criteria is specified, all keys for the view will be returned.
 */
public class SelectRequest extends DataRequest {

    private static final String EMPTY_STRING = "";

    /**
     * Create a select DMI Transaction. This type of data request will instruct the DMI to select a list of keys from
     * a view based on selection criteria with limiting keys.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param criteria     Selection criteria (or null for all records)
     * @param limitingKeys Limiting keys (optional)
     */
    public SelectRequest(@NonNull String account, @NonNull String token, @NonNull String controlId, @NonNull String sharedSecret,
                         @NonNull String viewName, String criteria, Iterable<String> limitingKeys) {
        super(account, token, controlId);

        String keys = (limitingKeys != null) ? String.join(Character.toString(StringUtils.SM), limitingKeys) : null;

        if (keys == null || EMPTY_STRING.equals(keys)) {
            super.addSubRequest(DataAccessType.SELECT, viewName, null, null, criteria);
        } else {
            super.addSubRequest(DataAccessType.SUBSELECT, viewName, null, keys, criteria);
        }

        super.addHashSubRequest(sharedSecret);
    }
}