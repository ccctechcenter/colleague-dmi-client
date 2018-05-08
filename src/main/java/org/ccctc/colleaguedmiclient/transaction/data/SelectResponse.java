package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.Getter;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.Arrays;

@Getter
public class SelectResponse {

    /**
     * Table name
     */
    private final String table;

    /**
     * List of keys selected
     */
    private final String[] keys;

    public static SelectResponse fromDmiTransaction(DmiTransaction transaction) {
        assert "DAFS".equals(transaction.getTransactionType());
        assert "DAFQ".equals(transaction.getInResponseTo());

        if (transaction.getSubTransactions().size() == 0)
            throw new DmiTransactionException("DMI Transaction passed to SelectResponse did not contain any sub transactions");

        return new SelectResponse(transaction.getSubTransactions().get(0));
    }

    public SelectResponse(DmiSubTransaction subTransaction) {
        assert "SDAFS".equals(subTransaction.getTransactionType());

        String[] commands = subTransaction.getCommands();

        assert commands.length >= 7;
        assert "F".equals(commands[0]);
        assert "STANDARD".equals(commands[1]);
        assert ("SELECT".equals(commands[2]) || "SUBSELECT".equals(commands[2]));
        assert "L".equals(commands[3]);
        assert "SELECT".equals(commands[4]);
        assert commands[5] != null;

        this.table = commands[5];

        int sizeCheck;
        try {
            sizeCheck = Integer.valueOf(commands[6]);
        } catch (NumberFormatException e) {
            throw new DmiTransactionException("Missing record length");
        }

        if (commands.length != sizeCheck + 4)
            throw new DmiTransactionException("Sub transaction size does not match size check");

        assert (this.table + ".END").equals(commands[commands.length - 1]);

        this.keys = Arrays.copyOfRange(commands, 7, commands.length - 1);
    }
}
