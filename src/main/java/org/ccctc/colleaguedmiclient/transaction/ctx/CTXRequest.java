package org.ccctc.colleaguedmiclient.transaction.ctx;

import org.ccctc.colleaguedmiclient.model.KeyValuePair;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.ArrayList;
import java.util.List;

public class CTXRequest extends DmiTransaction {

    private final static String CTRQ = "CTRQ";
    private final static String SCTRQ = "SCTRQ";
    private final static String SCTVAL = "SCTVAL";

    /**
     * Create a DMI Transaction to run a Colleague Transaction.
     *
     * @param account         Account
     * @param token           Token
     * @param controlId       Control ID
     * @param sharedSecret    Shared Secret
     * @param appl            Application
     * @param transactionName Transaction name
     * @param parameters      Parameters
     */
    public CTXRequest(String account, String token, String controlId, String sharedSecret,
                      String appl, String transactionName, List<KeyValuePair<String, String>> parameters) {
        super(account, CTRQ, appl, token, controlId);
        super.addSubTransaction(subTransaction(appl, transactionName, parameters));
        super.addHashSubRequest(sharedSecret);
    }

    /**
     * Create the SCTRQ sub transaction for a request
     *
     * @param appl            Application
     * @param transactionName Transaction Name
     * @param parameters      Parameters
     * @return Sub transaction
     */
    private DmiSubTransaction subTransaction(String appl, String transactionName,
                                             List<KeyValuePair<String, String>> parameters) {

        List<String> request = new ArrayList<>();
        request.add("1");
        request.add(appl);
        request.add(transactionName);

        if (parameters != null && parameters.size() > 0) {
            request.add(SCTVAL);

            // header:
            // total size of SCTVAL block
            // mio level (0 is the default)
            // version (1 is the default)
            // parameters (each takes up 4 values):
            // 1. name
            // 2. value
            // 3. blank
            // 4. blank

            request.add(Integer.toString(parameters.size() * 4 + 6));
            request.add("0");
            request.add("1");
            request.add(Integer.toString(parameters.size()));

            for (KeyValuePair<String, String> p : parameters) {
                request.add(p.getKey());
                request.add(p.getValue());
                request.add(null);
                request.add(null);
            }

            request.add(SCTVAL + ".END");
        }

        String[] array = new String[request.size()];
        request.toArray(array);

        return new DmiSubTransaction(SCTRQ, 0, array);
    }
}