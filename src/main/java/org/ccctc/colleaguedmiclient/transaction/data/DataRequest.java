package org.ccctc.colleaguedmiclient.transaction.data;

import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

/**
 * DMI Transaction template for data requests, to be implemented for specific types of data requests.
 *
 * @see BatchKeysRequest
 * @see BatchSelectRequest
 * @see SelectRequest
 * @see SingleKeyRequest
 */
abstract class DataRequest extends DmiTransaction {

    private static final String DAFQ = "DAFQ";
    private static final String SDAFQ = "SDAFQ";

    private static final String VIEW = "VIEW";
    private static final String STANDARD = "STANDARD";
    private static final String F = "F";
    private static final String L = "L";

    DataRequest(String account, String token, String controlId) {
        super(account, DAFQ, "UT", token, controlId);
    }

    /**
     * Abbreviated version of sub transaction with default values
     *
     * @param dataAccessType Data access type
     * @param viewName       View name
     * @param viewType       View type
     * @param columns        Columns (or keys for a SELECT)
     * @param criteria       Criteria
     */
    void addSubRequest(DataAccessType dataAccessType, String viewName, ViewType viewType, String columns, String criteria) {
        addSubRequest(F, STANDARD, (dataAccessType != null ? dataAccessType.toString() : null), L, VIEW, viewName,
                "0", columns, criteria, (viewType != null ? viewType.toString() : null), null);
    }

    /**
     * Full version of sub transaction
     *
     * @param submitFlag      Submit Flag
     * @param mode            Mode
     * @param dataAccessType  Data access type
     * @param viewOptions     View options
     * @param view            View
     * @param viewName        View name
     * @param viewRequestSize Request size
     * @param columns         Columns
     * @param criteria        Criteria
     * @param viewType        View type
     * @param requesterName   Requester Name
     */
    protected void addSubRequest(String submitFlag, String mode, String dataAccessType, String viewOptions,
                               String view, String viewName, String viewRequestSize, String columns,
                               String criteria, String viewType, String requesterName) {

        String[] commands = new String[]{
                submitFlag,
                mode,
                dataAccessType,
                viewOptions,
                view,
                viewName,
                viewRequestSize,
                columns,
                criteria,
                viewType,
                requesterName,
                (viewName != null) ? viewName + ".END" : null};

        DmiSubTransaction sub = new DmiSubTransaction(SDAFQ, 0, commands);
        super.addSubTransaction(sub);
    }
}