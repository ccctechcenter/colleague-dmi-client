package org.ccctc.colleaguedmiclient.transaction.ctx;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.HashMap;
import java.util.Map;

import static org.ccctc.colleaguedmiclient.util.StringUtils.parseIntOrNull;

/**
 * Response from a DMI transaction that requested to run a Colleague Transaction. This should be instantiated by
 * calling @{code fromDmiTransaction()} with the DMI response from a @{code CTXRequest}.
 *
 * @see org.ccctc.colleaguedmiclient.service.DmiCTXService
 * @see CTXRequest
 */
@Getter
@ToString
public class CTXResponse {

    private static final String SCTRS = "SCTRS";
    private static final String SCTVAL = "SCTVAL";

    /**
     * Variables returned by the CTX transaction
     */
    private final Map<String, String> variables = new HashMap<>();

    /**
     * Create a CTX response from a DMI Transaction
     *
     * @param transaction DMI Transaction
     * @return CTX Response
     */
    public static CTXResponse fromDmiTransaction(DmiTransaction transaction) {
        if (transaction.getSubTransactions().size() > 0) {
            DmiSubTransaction sctrs = null;
            DmiSubTransaction sctval = null;

            for (DmiSubTransaction sub : transaction.getSubTransactions()) {
                if (SCTRS.equals(sub.getTransactionType()))
                    sctrs = sub;
                if (SCTVAL.equals(sub.getTransactionType()))
                    sctval = sub;
            }

            if (sctrs != null && sctval != null)
                return new CTXResponse(sctrs, sctval);
        }

        throw new DmiTransactionException("DMI Transaction does not contain a response to a Colleague Transaction");
    }

    /**
     * Create a data response from a sub transactions of types SCTRS and SCTVAL
     *
     * @param sctrs SCTRS sub transaction
     * @param sctval SCTVAL sub transaction
     */
    private CTXResponse(@NonNull DmiSubTransaction sctrs, @NonNull DmiSubTransaction sctval) {

        // @TODO - what do we need sctrs for ?

        // SCTVAL contains the values of the output variables
        String[] commands = sctval.getCommands();

        if (commands.length < 2)
            throw new DmiTransactionException("Malformed response: sub transaction not long enough");

        Integer variablesCount = parseIntOrNull(commands[1]);
        if (variablesCount == null)
            throw new DmiTransactionException("Malformed response: variable count is missing");

        for (int x = 0; x < variablesCount; x++) {
            int pos = 2 + x * 4;

            if (commands.length <= pos + 1)
                throw new DmiTransactionException("Malformed response: end of transaction before all variables read");

            String key = commands[pos];
            String value = commands[pos+1];

            if (key != null) {
                // it has been found that spaces can exist erroneously in transactions, notably in GET.CTX.DETAILS
                key = key.replace(" ", "");
                variables.put(key, value);
            }
        }
    }
}
