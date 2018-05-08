package org.ccctc.colleaguedmiclient.transaction.data;

import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

public class DataRequest extends DmiTransaction {

    private static final String DAFQ = "DAFQ";
    private static final String SDAFQ = "SDAFQ";

    public enum ViewType {PHYS, LOGI, PERM, BLOB}

    protected enum DataAccessType {BATCHSELECT, BATCHKEYS, SINGLEKEY, SELECT, SUBSELECT}

    protected enum View {VIEW}

    protected enum SubmitFlag {F}

    protected enum Mode {STANDARD}

    protected enum ViewOptions {L}

    protected DataRequest(String account, String token, String controlId) {
        super(account, DAFQ, "UT", token, controlId);
    }

    /**
     * Abbreviated version of sub transaction with default values
     *
     * @param dataAccessType Data access type
     * @param viewName       View name
     * @param viewType       View type
     * @param viewOptions    View options
     * @param columns        Columns (or keys for a SELECT)
     * @param criteria       Criteria
     */
    protected void addSubRequest(DataAccessType dataAccessType, String viewName, ViewType viewType,
                                 ViewOptions viewOptions, String columns, String criteria) {

        addSubRequest(SubmitFlag.F, Mode.STANDARD, dataAccessType, viewOptions, View.VIEW, viewName,
                "0", columns, criteria, viewType, null);
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
    private void addSubRequest(SubmitFlag submitFlag, Mode mode, DataAccessType dataAccessType, ViewOptions viewOptions,
                               View view, String viewName, String viewRequestSize, String columns,
                               String criteria, ViewType viewType, String requesterName) {

        String[] commands = new String[]{
                (submitFlag != null) ? submitFlag.toString() : null,
                (mode != null) ? mode.toString() : null,
                (dataAccessType != null) ? dataAccessType.toString() : null,
                (viewOptions != null) ? viewOptions.toString() : null,
                (view != null) ? view.toString() : null,
                viewName,
                viewRequestSize,
                (columns != null) ? String.join(",", columns) : null,
                criteria,
                (viewType != null) ? viewType.toString() : null,
                requesterName,
                (viewName != null) ? viewName + ".END" : null};

        DmiSubTransaction sub = new DmiSubTransaction(SDAFQ, 0, commands);
        super.addSubTransaction(sub);
    }
}