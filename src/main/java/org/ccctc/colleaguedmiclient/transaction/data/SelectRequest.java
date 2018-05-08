package org.ccctc.colleaguedmiclient.transaction.data;

import org.ccctc.colleaguedmiclient.util.StringUtils;

public class SelectRequest extends DataRequest {

    /**
     * Create a select DMI Transaction. This type of data request will instruct the DMI to select a list of keys from
     * a view based on selection criteria. If no selection critria is specified, all keys for the view will be returned.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param criteria     Selection criteria (or null for all keys)
     */
    public SelectRequest(String account, String token, String controlId, String sharedSecret, String viewName,
                         String criteria) {
        this(account, token, controlId, sharedSecret, viewName, criteria, null);

    }

    /**
     * Create a select DMI Transaction. This type of data request will instruct the DMI to select a list of keys from
     * a view based on selection criteria with limiting keys.
     *
     * @param account      Account
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared Secret
     * @param viewName     View name
     * @param criteria     Selection criteria
     * @param limitingKeys Limiting keys
     */
    public SelectRequest(String account, String token, String controlId, String sharedSecret, String viewName,
                         String criteria, String[] limitingKeys) {
        super(account, token, controlId);

        String keys = (limitingKeys != null) ? String.join(Character.toString(StringUtils.SM), limitingKeys) : null;

        if (keys == null || keys.equals("")) {
            super.addSubRequest(DataAccessType.SELECT, viewName, null, ViewOptions.L, null, criteria);
        } else {
            super.addSubRequest(DataAccessType.SUBSELECT, viewName, null, ViewOptions.L, keys, criteria);
        }

        super.addHashSubRequest(sharedSecret);
    }
}