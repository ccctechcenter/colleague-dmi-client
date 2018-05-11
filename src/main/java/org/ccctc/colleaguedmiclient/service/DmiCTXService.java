package org.ccctc.colleaguedmiclient.service;

import org.ccctc.colleaguedmiclient.model.KeyValuePair;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;
import org.ccctc.colleaguedmiclient.transaction.ctx.CTXRequest;
import org.ccctc.colleaguedmiclient.transaction.ctx.CTXResponse;

import java.util.List;

/**
 * Service for running a Colleague Transaction via the DMI Service.
 * <p>
 * To instantiate this object, you will need to first create a {@code DmiService}.
 * <p>
 * In a Spring environment, these services would typically be beans.
 *
 * @see DmiService
 */
public class DmiCTXService {

    private final DmiService dmiService;

    /**
     * Create a DMI CTX Service (for running Colleague Transactions). This requires a DMI Service (to send/receive
     * DMI transactions).
     *
     * @param dmiService DMI Service
     */
    public DmiCTXService(DmiService dmiService) {
        this.dmiService = dmiService;
    }

    /**
     * Execute a Colleague Transaction and return data from the response (the output parameters of the Colleague
     * Transaction)
     *
     * @param appl            Application
     * @param transactionName Transaction Name
     * @param params          Transaction Input parameters
     * @return Data from the output parameters of the Colleague Transaction
     */
    public CTXResponse execute(String appl, String transactionName, List<KeyValuePair<String, String>> params) {
        SessionCredentials creds = dmiService.getSessionCredentials();
        CTXRequest request = new CTXRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), appl, transactionName, params);

        DmiTransaction dmiResponse = dmiService.send(request);
        return CTXResponse.fromDmiTransaction(dmiResponse);
    }
}
