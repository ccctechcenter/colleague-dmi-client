package org.ccctc.colleaguedmiclient.transaction.data;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ccctc.colleaguedmiclient.util.StringUtils.parseIntOrNull;

/**
 * Response from a DMI transaction that requested data. This should be instantiated by calling {@code fromDmiTransaction()}
 * with the DMI response from a {@code DataRequest}.
 *
 * @see org.ccctc.colleaguedmiclient.service.DmiDataService
 * @see DataRequest
 */
@ToString
public class DataResponse {

    private final static String DAFS = "DAFS";
    private final static String SDAFS = "SDAFS";
    private final static String BATCH = "BATCH";
    private final static String SINGLE = "SINGLE";
    private final static String TUPLE = "TUPLE";

    private final static String F = "F";
    private final static String STANDARD = "STANDARD";

    private final static String ERROR_00011 = "00011";
    private final static String ERROR_00012 = "00011";

    /**
     * Table name
     */
    @Getter private final String table;

    /**
     * Selection mode (SINGLE or BATCH)
     */
    @Getter private final String mode;

    /**
     * Field data, indexed by field name
     */
    @Getter private final Map<String, String[]> data = new HashMap<>();

    /**
     * Field data, indexed by field order
     */
    @Getter private final List<String> order = new ArrayList<>();

    /**
     * Create a data response from a DMI transaction.
     *
     * @param transaction DMI Transaction
     * @return Data response
     */
    public static DataResponse fromDmiTransaction(@NonNull DmiTransaction transaction) {
        for (DmiSubTransaction sub : transaction.getSubTransactions()) {
            if (SDAFS.equals(sub.getTransactionType()))
                return new DataResponse(sub);
        }

        throw new DmiTransactionException("DMI Transaction does not contain a response to a data request");
    }

    /**
     * Create a data response from a sub transaction of type SDAFS
     *
     * @param subTransaction Sub transaction
     */
    private DataResponse(DmiSubTransaction subTransaction) {

        String[] commands = subTransaction.getCommands();

        if (commands.length < 11)
            throw new DmiTransactionException("Malformed response: sub transaction not long enough");

        mode = commands[4];
        table = commands[5];

        if (table == null)
            throw new DmiTransactionException("Malformed response: no table/view specified");

        assert F.equals(commands[0]);
        assert STANDARD.equals(commands[1]);

        /*
        Not sure what would trigger an error code here rather than a SERRS sub-transaction instead. Will leave this in
        here if it's needed.

        String errorCode = commands[10];
        if (errorCode != null)
            throw new DmiTransactionException("Error code in sub transaction : " + errorCode);
        */

        if (SINGLE.equals(mode)) {
            Single(subTransaction);
        } else if (BATCH.equals(mode)) {
            Batch(subTransaction);
        } else {
            throw new DmiTransactionException("Malformed response: unexpected mode: " + mode);
        }
    }

    /**
     * Process an SDAFS sub transaction of type BATCH
     * <p>
     * The format is as follows:
     * 0  = F
     * 1  = STANDARD
     * 2  = BATCHKEYS or BATCHSELECT
     * 3  = L
     * 4  = BATCH
     * 5  = (table name) - start of table block
     * 6  = size of entire BATCH block starting with line 4 (BATCH) and ending with the end of the response - (table name).END
     * ...
     * 12 = number of records returned
     * 13 = also number of records returned (not sure the difference between 12 and 13 ..)
     * 14 = TUPLE
     * 15 = (record key) - start of record block
     * 16 = number of fields in the response
     * 17 = error code
     * 18 = value for field #1. will be included if columns was requested, or blank if column was not requested.
     * 19 = value for field #2. will be included if columns was requested, or blank if column was not requested.
     * 20 = etc
     * ...
     * xx = (record key).END - end of record block
     * ...
     * ... each subsequent record follows the same format above starting with "TUPLE" and ending with (record key).END
     * ...
     * xx = (table name).END - end of table block
     *
     * @param subTransaction Sub transaction
     */
    private void Batch(DmiSubTransaction subTransaction) {

        String[] commands = subTransaction.getCommands();

        // verify transaction size
        Integer subsetSize = parseIntOrNull(commands[6]);
        if (subsetSize == null)
            throw new DmiTransactionException("Malformed response: subset size is missing");
        else if (subsetSize != commands.length - 4)
            throw new DmiTransactionException("Malformed response: subset size does match response size");

        // verify end of table block
        if (!(table + ".END").equals(commands[subsetSize + 3]))
            throw new DmiTransactionException("Malformed response: " + table + ".END not found where expected");

        Integer records = parseIntOrNull(commands[12]);

        if (records == null)
            throw new DmiTransactionException("Malformed response: record count is missing");

        int startPos = 14;
        for(int x = 0; x < records; x++) {
            if (startPos + 2 > commands.length)
                throw new DmiTransactionException("Malformed response: end of transaction before all records read");

            // get header of the record, determine length and start and end read positions
            String tuple = commands[startPos];
            String key = commands[startPos + 1];
            String errorCode = commands[startPos + 1];

            // not found
            if (ERROR_00011.equals(errorCode)) continue;
            if (ERROR_00012.equals(errorCode)) throw new DmiTransactionException("Error reading file - 00012");

            int recordLen = parseIntOrNull(commands[startPos + 2]);

            // no data to return for this record
            if (recordLen == 0) continue;

            int fieldsStart = startPos + 4;
            int fieldsEnd = fieldsStart + recordLen;

            // this is probably an error, but just in case an empty string qualifies as a valid key, use it ...
            if (key == null) key = "";

            // validate position of TUPLE and (key).END
            if (!TUPLE.equals(tuple))
                throw new DmiTransactionException("Malformed response: missing TUPLE statement");
            if (fieldsEnd >= commands.length || !(key + ".END").equals(commands[fieldsEnd]))
                throw new DmiTransactionException("Malformed response: end of record not found for key " + key);

            String[] d = Arrays.copyOfRange(commands, fieldsStart, fieldsEnd);

            data.put(key, d);
            order.add(key);

            startPos = fieldsEnd + 1;
        }
    }


    /**
     * Process an SDAFS sub transaction of type SINGLE
     * <p>
     * The format is as follows:
     * 0  = F
     * 1  = STANDARD
     * 2  = SINGLEKEY
     * 3  = L
     * 4  = SINGLE
     * 5  = (table name) - start of table block
     * 6  = number of fields returned
     * 7  =
     * 8  = record key
     * ...
     * 11 = value for field #1. will be included if columns was requested, or blank if column was not requested.
     * 12 = value for field #2. will be included if columns was requested, or blank if column was not requested.
     * 13 = etc
     * ...
     * xx = (table name).END - end of table block
     *
     * @param subTransaction Sub transaction
     */
    private void Single(DmiSubTransaction subTransaction) {

        String[] commands = subTransaction.getCommands();

        // verify transaction size
        Integer subsetSize = parseIntOrNull(commands[6]);
        if (subsetSize == null)
            throw new DmiTransactionException("Malformed response: subset size is missing");
        else if (subsetSize != commands.length - 12)
            throw new DmiTransactionException("Malformed response: subset size does match response size");

        // verify end of table block
        if (!(table + ".END").equals(commands[subsetSize + 11]))
            throw new DmiTransactionException("Malformed response: " + table + ".END not found where expected");

        String key = commands[8];
        if (key == null) key = "";

        if (subsetSize > 0) {
            String[] d = Arrays.copyOfRange(commands, 11, subsetSize + 11);

            this.data.put(key, d);
            this.order.add(key);
        }
    }
}
