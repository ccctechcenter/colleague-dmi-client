package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.Arrays;

import static org.ccctc.colleaguedmiclient.util.StringUtils.parseIntOrNull;

/**
 * Response from a DMI transaction that requested a key select. This should be instantiated by calling
 * @{code fromDmiTransaction()} with the DMI response from a @{code SelectRequest}.
 *
 * @see org.ccctc.colleaguedmiclient.service.DmiDataService
 * @see SelectRequest
 */
@ToString
public class SelectResponse {

    private final static String SDAFS = "SDAFS";

    private final static String F = "F";
    private final static String STANDARD = "STANDARD";
    private final static String SELECT = "SELECT";

    /**
     * Table name
     */
    @Getter private final String table;

    /**
     * List of keys selected
     */
    @Getter private final String[] keys;

    /**
     * Create a select response from a DMI transaction.
     *
     * @param transaction DMI Transaction
     * @return Select response
     */
    public static SelectResponse fromDmiTransaction(@NonNull DmiTransaction transaction) {
        for (DmiSubTransaction sub : transaction.getSubTransactions()) {
            if (SDAFS.equals(sub.getTransactionType()))
                return new SelectResponse(sub);
        }

        throw new DmiTransactionException("DMI Transaction does not contain a response to a select request");
    }

    /**
     * Create a select response from a sub transaction of type SDAFS
     * <p>
     * The format is as follows:
     * 0  = F
     * 1  = STANDARD
     * 2  = SELECT or SUBSELECT
     * 3  = L
     * 4  = SELECT
     * 5  = (table name) - start of table block
     * 6  = size of entire SELECT block starting with line 4 (SELECT) and ending with the end of the response - (table name).END
     * 7  = first key
     * 8  = second key
     * 9  = etc
     * ...
     * xx = (table name).END - end of table block
     *
     * @param subTransaction Sub transaction
     */
    private SelectResponse(DmiSubTransaction subTransaction) {
        String[] commands = subTransaction.getCommands();

        if (commands.length < 7)
            throw new DmiTransactionException("Malformed response: sub transaction not long enough");

        this.table = commands[5];

        if (table == null)
            throw new DmiTransactionException("Malformed response: no table/view specified");

        assert F.equals(commands[0]);
        assert STANDARD.equals(commands[1]);
        assert SELECT.equals(commands[4]);

        // verify transaction size
        Integer subsetSize = parseIntOrNull(commands[6]);
        if (subsetSize == null)
            throw new DmiTransactionException("Malformed response: subset size is missing");
        else if (subsetSize != commands.length - 4)
            throw new DmiTransactionException("Malformed response: subset size does match response size");

        // verify end of table block
        if (!(table + ".END").equals(commands[subsetSize + 3]))
            throw new DmiTransactionException("Malformed response: " + table + ".END not found where expected");

        this.keys = Arrays.copyOfRange(commands, 7, commands.length - 1);
    }
}
