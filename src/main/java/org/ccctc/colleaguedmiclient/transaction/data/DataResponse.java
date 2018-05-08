package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.Getter;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DataResponse {

    /**
     * Table name
     */
    private final String table;

    /**
     * Selection mode (SINGLE or BATCH)
     */
    private final String mode;

    /**
     * Field data, indexed by field name
     */
    private final Map<String, String[]> data = new HashMap<>();

    /**
     * Field data, indexed by field order
     */
    private final List<String> order = new ArrayList<>();

    /**
     * Create a data response from a DMI transaction. The first sub-transaction of the DMI Transaction is assumed to
     * contain the data requested (or an error if one occurred).
     *
     * @param transaction DMI Transaction
     * @return Data response
     */
    public static DataResponse fromDmiTransaction(DmiTransaction transaction) {
        assert "DAFS".equals(transaction.getTransactionType());

        if (transaction.getSubTransactions().size() == 0)
            throw new DmiTransactionException("DMI Transaction passed to DataResponse did not contain any sub transactions");

        return new DataResponse(transaction.getSubTransactions().get(0));
    }

    /**
     * Create a data response from a sub transaction
     *
     * @param subTransaction Sub transaction
     */
    private DataResponse(DmiSubTransaction subTransaction) {

        if (subTransaction.getTransactionType().equals("SERRS")) {
            if (subTransaction.getCommands() == null || subTransaction.getCommands().length == 0)
                throw new DmiTransactionException("Unknown error returned from DMI Transaction");

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(subTransaction.getCommands()[0]);
            for(int x = 1; x < subTransaction.getCommands().length; x++) {
                stringBuilder.append(" ");
                stringBuilder.append(subTransaction.getCommands()[x]);
            }

            throw new DmiTransactionException(stringBuilder.toString());
        }

        assert "SDAFS".equals(subTransaction.getTransactionType());
        assert subTransaction.getCommands().length >= 7;
        assert "F".equals(subTransaction.getCommands()[0]);
        assert "STANDARD".equals(subTransaction.getCommands()[1]);
        assert subTransaction.getCommands()[2] != null;
        assert subTransaction.getCommands()[4] != null;
        assert subTransaction.getCommands()[5] != null;

        this.mode = subTransaction.getCommands()[4];
        this.table = subTransaction.getCommands()[5];

        if ("SINGLE".equals(this.mode)) {
            Single(subTransaction);
        } else if ("BATCH".equals(this.mode)) {
            Batch(subTransaction);
        }
    }

    /**
     * Process a BATCH sub transaction
     *
     * @param subTransaction Sub transaction
     */
    private void Batch(DmiSubTransaction subTransaction) {

        String[] commands = subTransaction.getCommands();

        if (commands.length < 11)
            throw new DmiTransactionException("Subset not long enough");

        String table = commands[5];
        String errorCode = commands[10];

        // @TODO - error on 00012 in errorCode

        /*
        int subsetSize;
        try {
            subsetSize = Integer.parseInt(commands[6]);
        } catch (NumberFormatException ignored) {
        throw new DmiTransactionException("Invalid subset length");
        }
        */


        int records;
        try {
            records = Integer.parseInt(commands[12]);
        } catch (NumberFormatException ignored) {
            throw new DmiTransactionException("Record count not specified");
        }

        int startPos = 14;
        for(int x = 0; x < records; x++) {
            assert "TUPLE".equals(commands[startPos]);

            // @TODO - length checking

            String key = commands[startPos+1];

            if (key == null || key.equals(""))
                throw new DmiTransactionException("Missing key");

            int recordLen;
            try {
                recordLen = Integer.parseInt(commands[startPos+2]);
            } catch (NumberFormatException ignored) {
                throw new DmiTransactionException("Missing record length");
            }

            data.put(key, Arrays.copyOfRange(commands, startPos + 4, startPos + 4 + recordLen));
            order.add(key);

            assert (key + ".END").equals(commands[startPos + 4 + recordLen]);

            if (x == records - 1) {
                assert (table + ".END").equals(commands[startPos + 5 + recordLen]);
            } else {
                startPos += recordLen + 5;

            }
        }
    }


    /**
     * Process a SINGLE sub transaction
     *
     * @param subTransaction Sub transaction
     */
    private void Single(DmiSubTransaction subTransaction) {

        String[] commands = subTransaction.getCommands();

        if (commands.length < 11)
            throw new DmiTransactionException("Subset not long enough");

        String table = commands[5];
        String key = commands[8];
        String errorCode = commands[10];

        // @TODO - error on 00011 or 00012 in errorCode

        int subsetSize;
        try {
            subsetSize = Integer.parseInt(commands[6]);
        } catch (NumberFormatException ignored) {
            throw new DmiTransactionException("Missing subset length");
        }

        if (commands.length < subsetSize + 12)
            throw new DmiTransactionException("Invalid subset length - too few records");

        assert (table + ".END").equals(commands[subsetSize + 11]);

        this.data.put(key, Arrays.copyOfRange(commands, 11, subsetSize + 11));
        this.order.add(key);
    }
}
