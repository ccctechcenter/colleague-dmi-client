package org.ccctc.colleaguedmiclient.transaction.ctx;

import lombok.Getter;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.HashMap;
import java.util.Map;

public class CTXResponse {

    @Getter private final Map<String, String> response = new HashMap<>();

    public static CTXResponse fromDmiTransaction(DmiTransaction transaction) {
        assert "CTRS".equals(transaction.getTransactionType());
        assert "CTRQ".equals(transaction.getInResponseTo());

        if (transaction.getSubTransactions().size() > 0) {
            DmiSubTransaction sctrs = null;
            DmiSubTransaction sctval = null;

            for (DmiSubTransaction sub : transaction.getSubTransactions()) {
                if ("SCTRS".equals(sub.getTransactionType()))
                    sctrs = sub;
                if ("SCTVAL".equals(sub.getTransactionType()))
                    sctval = sub;
            }

            return new CTXResponse(sctrs, sctval);
        }


        return null;
    }

    public CTXResponse(DmiSubTransaction sctrs, DmiSubTransaction sctval) {

        // @TODO - length of 3 is probably an ERR block ? process accordingly.

        /*
        if (arguments.Count == 3)
        {
            StringBuilder stringBuilder = new StringBuilder();
            foreach (string argument in arguments)
            {
                stringBuilder.Append(argument).Append(" \n");
            }
            throw new DataReaderErrorResponseException(stringBuilder.ToString());
        } */

        // @TODO - what do we need sctrs for ?
        if (sctrs != null) {

        }

        if (sctval != null) {
            assert "SCTVAL".equals(sctval.getTransactionType());
            assert sctval.getCommands().length >= 2;

            String[] commands = sctval.getCommands();

            int paramCount;
            try {
                paramCount = Integer.parseInt(commands[1]);
            } catch (NumberFormatException e) {
                throw new DmiTransactionException("Invalid parameter count");
            }

            // @TODO - array size checks
            for (int x = 0; x < paramCount; x++) {
                int pos = 2 + x * 4;

                String key = commands[pos];
                String value = commands[pos+1];

                this.response.put(key, value);
            }
        }
    }
}
