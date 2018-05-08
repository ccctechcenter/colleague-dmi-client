package org.ccctc.colleaguedmiclient.transaction;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.ByteSplitRemainder;
import org.ccctc.colleaguedmiclient.util.ByteUtils;
import org.ccctc.colleaguedmiclient.util.StringUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.ccctc.colleaguedmiclient.util.ByteUtils.byteArrayToString;
import static org.ccctc.colleaguedmiclient.util.ByteUtils.byteArrayToStringArray;
import static org.ccctc.colleaguedmiclient.util.StringUtils.*;

@Getter
@Setter
@ToString(exclude = "rawResponse")
public class DmiTransaction {

    private final static String SCLMQ = "SCLMQ";
    private final static String SDHSQ = "SDHSQ";
    private final static int CHUNK_SIZE = 1024;

    /**
     * Line 1 of a DMI transaction
     *
     * Literal String "DMI"
     */
    private String dmi;

    /**
     * Line 2 of a DMI transaction
     *
     * Version - "1.4" in this implementation
     */
    private String version;

    /**
     * Line 3 of a DMI transaction
     *
     * Transaction type
     */
    private String transactionType;

    /**
     * Line 4 of a DMI transaction
     *
     * Account information. The first element is the Colleague Environment / account (for example, production_rt).
     * The second element is the DASH connect string.
     */
    private String[] account;

    /**
     * Line 5 of a DMI transaction
     *
     * Application for this request, such as UT, CORE, ST, etc.
     */
    private String application;

    /**
     * Line 6 of a DMI transaction
     *
     * Token information returned by a login request or passed for a DMI request that need authorization. The combination
     * of token and the first value of ControlID authenticates a request.
     */
    private String[] token;

    /**
     * Line 7 of a DMI transaction
     *
     * Listener ID
     */
    private String listenerId;

    /**
     * Line 8 of a DMI transaction
     *
     * Control ID information. The first element is the Control ID (used to authenticate the request). The second
     * element is a unique value for the request (this application generates a random number).
     */
    private String[] controlId;

    /**
     * Line 9 of a DMI transaction
     *
     * Date request was created
     */
    private LocalDate createdDate;

    /**
     * Line 10 of a DMI transaction
     *
     * Time request was created
     */
    private LocalTime createdTime;

    /**
     * Line 11 of a DMI transaction
     *
     * Whom request was created by - HOST for the DMI or "CoreWSý2.0" for this service
     */
    private String createdBy;

    /**
     * Line 12 of a DMI transaction
     *
     * What transaction the request is in response to
     */
    private String inResponseTo;

    /**
     * Line 13 of a DMI transaction
     *
     * Debug level
     */
    private String debugLevel;

    /**
     * Line 14 of a DMI transaction
     *
     * Last processed by
     */
    private String lastProcessedBy;

    /**
     * Line 15 of a DMI transaction
     *
     * Last processed date
     */
    private LocalDate lastProcessedDate;

    /**
     * Line 16 of a DMI transaction
     *
     * Last processed time
     */
    private LocalTime lastProcessedTime;

    /**
     * Variable length sub transactions after line 16 of the DMI transaction
     */
    private List<DmiSubTransaction> subTransactions;

    /**
     * Has a hash been added to this request? If so subrequests are closed.
     */
    private boolean hashAdded = false;

    /**
     * Raw DMI response - chunks of bytes returned from the DMI
     */
    private List<byte[]> rawResponse;

    /**
     * Set on a fromResponse or toDmiBytes (used in logging)
     */
    private long transactionBytes = -1;


    private DmiTransaction() {
    }

    /**
     * Create a DMI Transaction
     *
     * @param account Account
     * @param transactionType Transaction type
     * @param application Application
     * @param token Token
     * @param controlId Control ID
     */
    protected DmiTransaction(@NonNull String account, @NonNull String transactionType, @NonNull String application,
                             String token, String controlId) {
        Random random = ThreadLocalRandom.current();
        String r = String.valueOf(Math.abs(random.nextInt()));

        this.dmi = "DMI";
        this.version = "1.4";
        this.transactionType = transactionType;
        this.account = new String[] { account };
        this.application = application;
        if (token != null) this.token = new String[] { token };

        this.controlId = new String[] { r, r };
        if (controlId != null) this.controlId[0] = controlId;

        this.createdBy = "CoreWSý2.0";
        this.createdDate = LocalDate.now();
        this.createdTime = LocalTime.now();
    }

    /**
     * Add a sub transaction
     *
     * Note: Once a hash has been added, no more sub transactions can be added
     *
     * @param subTransaction Sub transaction to add
     */
    protected void addSubTransaction(DmiSubTransaction subTransaction) {
        if (hashAdded)
            throw new DmiTransactionException("Attempted to add sub transaction to a DMI transaction after hash subrequest");

        if (subTransactions == null) subTransactions = new ArrayList<>();
        subTransactions.add(subTransaction);
    }

    /**
     * Append a delimiter followed by a string value to the string builder
     *
     * @param stringBuilder String builder
     * @param value Value
     * @param delimiter Delimeter
     */
    private void append(StringBuilder stringBuilder, String value, char delimiter) {
        stringBuilder.append(delimiter);
        if (value != null) stringBuilder.append(value);
    }

    /**
     * Append a delimiter followed by each value of a string array delimiteed by a sub delimiter
     *
     * @param StringBuilder String builder
     * @param value Value
     * @param delimiter Delimiter
     * @param subDelimiter Sub delimiter
     */
    private void append(StringBuilder StringBuilder, String[] value, char delimiter, char subDelimiter) {
        String val = null;
        if (value != null && value.length > 0) {
            StringBuilder b = new StringBuilder();
            b.append(value[0] != null ? value[0] : "");
            for (int x = 1; x < value.length; x++) {
                b.append(subDelimiter);
                b.append(value[x] != null ? value[x] : "");
            }
            val = b.toString();
        }

        append(StringBuilder, val, delimiter);
    }

    public String debugInfo() {
        StringBuilder b = new StringBuilder();

        if (this.account != null && this.account.length > 0) {
            b.append(this.account[0]);
            b.append(" ");
        }

        b.append(this.transactionType);
        b.append(" ");
        if (this.inResponseTo != null) {
            b.append(" responding to: ");
            b.append(this.inResponseTo);
            b.append(" ");
        }

        if (this.subTransactions.size() > 0) {
            b.append(" - ");

            for (DmiSubTransaction s : this.subTransactions) {
                b.append(s.getTransactionType());
                b.append(" ");
            }
        }

        b.append(" ... ");

        if (this.transactionBytes > 0)
            b.append("Size: " + transactionBytes);

        return b.toString();
    }

    /**
     * Convert this DMI transaction to a DMI String
     *
     * @return String
     */
    public String toDmiString() {
        StringBuilder b = new StringBuilder();

        // the first first 16 values are common to all DMI transactions
        b.append(this.dmi);
        append(b, this.version, FM);
        append(b, this.transactionType, FM);
        append(b, this.account, FM, VM);
        append(b, this.application, FM);
        append(b, this.token, FM, VM);
        append(b, this.listenerId, FM);
        append(b, this.controlId, FM, VM);
        append(b, dateToString(this.createdDate), FM);
        append(b, timeToString(this.createdTime), FM);
        append(b, this.createdBy, FM);
        append(b, this.inResponseTo, FM);
        append(b, this.debugLevel, FM);
        append(b, this.lastProcessedBy, FM);
        append(b, dateToString(this.lastProcessedDate), FM);
        append(b, timeToString(this.lastProcessedTime), FM);

        //
        // sub transactions follow and are variable in length. Each sub transactions has three header lines and one
        // footer line. The three header lines are: transaction type, total line of the sub transaction block, MIO level.
        // the footer line is the transaction type followed by ".END".
        //
        // Example:
        // SLGRQ
        // 9
        // 0
        // ... 5 lines of detail here ...
        // SLGRQ.END
        //
        if (this.subTransactions != null) {
            for (DmiSubTransaction r : this.subTransactions) {
                append(b, r.getTransactionType(), FM);
                append(b, String.valueOf(r.getCommands().length + 4), FM);
                append(b, String.valueOf(r.getMioLevel()), FM);
                if (r.getCommands() != null) {
                    for (String c : r.getCommands()) {
                        append(b, c, FM);
                    }
                }
                append(b, r.getTransactionType() + ".END", FM);
            }
        }

        return b.toString();
    }

    /**
     * Convert this DMI transaction to a byte array, including the appropriate header and footer
     * indicating the beginning, size and end of the DMI transaction.
     *
     * The DMI transaction is encoded in windows-1252 format
     *
     * @return Byte array
     */
    public byte[] toDmiBytes() {
        try {
            String dmiString = toDmiString();

            //
            // the structure of a DMI request includes a header, which indicates the size of the request, plus
            // a footer. The size of the request includes the 5 bytes at the end of the request (#END).
            //
            // Example:
            //
            // Header = #100# - this indicates that after this header there are 100 bytes to be read (including the footer)
            // Body = .... 95 characters of data ...
            // Footer = #END# - this indicates the transaction has ended
            //
            byte[] body = dmiString.getBytes("windows-1252");
            byte[] header = ("#" + (body.length + 5) + "#").getBytes("windows-1252");
            byte[] footer = ("#END#").getBytes("windows-1252");

            byte[] result = new byte[header.length + body.length + footer.length];


            System.arraycopy(header, 0, result, 0, header.length);
            System.arraycopy(body, 0, result, header.length, body.length);
            System.arraycopy(footer, 0, result, header.length + body.length, footer.length);

            this.transactionBytes = result.length;

            return result;
        } catch (UnsupportedEncodingException e) {
            throw new DmiTransactionException("Encoding error - " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Create a DMI transaction from an input stream from the DMI
     *
     * @param is Input stream
     * @return DMI Transaction
     */
    public static DmiTransaction fromResponse(DataInputStream is) {
        DmiTransaction result = new DmiTransaction();
        result.readFromStream(is);
        return result;
    }

    /**
     * Process the input stream, reading the data in chunks and splitting into byte arrays delimited by FM so that it
     * can be easily processed. By splitting the values of the array up during the read process, less work needs to
     * be done later to separate each field.
     *
     * @param is Input stream
     */
    private void readFromStream(DataInputStream is) {

        //
        // determine the size of the response. the format of the header is #...# with the value between
        // the hash marks being the number of bytes of the stream after the header.
        //
        int headerBytes = 0;
        long responseSize;
        try {
            String size = "";
            boolean inHeader = false;
            while(true) {
                headerBytes++;
                byte bite = is.readByte();
                if (bite == '#') {
                    if (inHeader) break;
                    inHeader = true;
                }
                else size += Character.toString((char)bite);
            }

            if (size.length() == 0)
                throw new DmiTransactionException("Empty header");

            try {
                responseSize = Long.valueOf(size);
            } catch (NumberFormatException e ) {
                throw new DmiTransactionException("Invalid header size (non-numeric)");
            }
        } catch (IOException e) {
            throw new DmiTransactionException("Problem processing DMI response - " + e.getClass().getName() + ": " + e.getMessage());
        }

        this.transactionBytes = headerBytes + responseSize;

        //
        // read the rest of the response in chunks
        //
        List<byte[]> results = new ArrayList<>();
        long totalRead = 0L;
        byte[] data = new byte[CHUNK_SIZE];
        byte[] remainder = null;
        while (true) {
            try {
                int bytesRead = is.read(data);
                if (bytesRead == -1)
                    throw new DmiTransactionException("Encountered EOF before end of response");

                totalRead += bytesRead;

                // split the chunk into lists of byte arrays delimited by FM and a remainder (the last value)
                // the remainder is kept separate as the next chunk may contain more data for that response line
                ByteSplitRemainder byteSplitResult = ByteUtils.byteSplit(data, bytesRead, (byte) FM);

                // add split values to the result
                if (byteSplitResult.getSplit().size() > 0) {
                    List<byte[]> split = byteSplitResult.getSplit();

                    byte[] first = split.get(0);

                    if (remainder == null || remainder.length == 0) {
                        results.add(first);
                    } else {
                        // combine remainder in to first result
                        byte[] combined = new byte[remainder.length + first.length];
                        System.arraycopy(remainder, 0, combined, 0, remainder.length);
                        System.arraycopy(first, 0, combined, remainder.length, first.length);
                        results.add(combined);

                        remainder = null;
                    }

                    for (int x = 1; x < split.size(); x++) {
                        results.add(split.get(x));
                    }
                }

                // either set a new remainder or combine with existing remainder
                if (remainder == null || remainder.length == 0) {
                    remainder = byteSplitResult.getRemainder();
                } else {
                    byte[] r = byteSplitResult.getRemainder();
                    if (r != null && r.length > 0) {
                        // this chunk is still continuing, add the remainders together
                        byte[] combined = new byte[remainder.length + r.length];
                        System.arraycopy(remainder, 0, combined, 0, remainder.length);
                        System.arraycopy(r, 0, combined, remainder.length, r.length);
                        remainder = combined;
                    }
                }

                // if we've processed all expected bytes in the response, the remainder should now contain
                // the last line of the response, plus the footer: #END#
                if (totalRead == responseSize) {
                    if (remainder != null && remainder.length >= 5) {
                        int x = remainder.length - 5;

                        if (remainder[x] == (byte) '#'
                                && remainder[x + 1] == (byte) 'E'
                                && remainder[x + 2] == (byte) 'N'
                                && remainder[x + 3] == (byte) 'D'
                                && remainder[x + 4] == (byte) '#') {

                            if (remainder.length > 5)
                                results.add(Arrays.copyOfRange(remainder, 0, remainder.length - 5));
                            else
                                results.add(new byte[0]);

                            break;
                        }
                    }

                    throw new DmiTransactionException("Transaction end not found");
                }
            } catch (IOException e) {
                throw new DmiTransactionException("Problem processing response - " + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        // now that it's split out, send it to readResponse for processing
        readResponse(results);
    }

    /**
     * Read a DMI transaction from a List of byte arrays. Each item in the list is one line from the response.
     *
     * @param data DMI transaction data
     */
    private void readResponse(List<byte[]> data) {
        try {
            this.setRawResponse(data);

            if (data.size() < 16) {
                // add empty elements to get us up to 16
                for (int x = data.size(); x < 16; x++) {
                    data.add(new byte[0]);
                }
            }

            // strings
            this.dmi = byteArrayToString(data.get(0));
            this.version = byteArrayToString(data.get(1));
            this.transactionType = byteArrayToString(data.get(2));
            this.application = byteArrayToString(data.get(4));
            this.listenerId = byteArrayToString(data.get(6));
            this.createdBy = byteArrayToString(data.get(10));
            this.inResponseTo = byteArrayToString(data.get(11));
            this.debugLevel = byteArrayToString(data.get(12));
            this.lastProcessedBy = byteArrayToString(data.get(13));

            // arrays
            this.account = byteArrayToStringArray(data.get(3), VM);
            this.token = byteArrayToStringArray(data.get(5), VM);
            this.controlId = byteArrayToStringArray(data.get(7), VM);

            // date / times
            this.createdDate = dateFromString(byteArrayToString(data.get(8)));
            this.createdTime = timeFromString(byteArrayToString(data.get(9)));
            this.lastProcessedDate = dateFromString(byteArrayToString(data.get(14)));
            this.lastProcessedTime = timeFromString(byteArrayToString(data.get(15)));

            // add in sub commands
            if (data.size() > 16)
                this.subTransactions = new ArrayList<>();

            for (int x = 16; x < data.size(); x++) {
                String type = byteArrayToString(data.get(x));
                String end = type + ".END";
                boolean finishedBlock = false;
                List<String> subCommands = new ArrayList<>();
                for (x++; x < data.size(); x++) {
                    String val = byteArrayToString(data.get(x));
                    if (val != null && val.equals(end)) {
                        finishedBlock = true;
                        break;
                    }
                    subCommands.add(val);
                }

                if (!finishedBlock)
                    throw new DmiTransactionException("block not complete");

                // check size
                if (subCommands.size() < 2) throw new DmiTransactionException("sub transaction of incorrect size");
                int sizeCheck;
                try {
                    sizeCheck = Integer.valueOf(subCommands.get(0));

                    if (sizeCheck != subCommands.size() + 2)
                        throw new DmiTransactionException("sub transaction size does not match content");

                } catch (NumberFormatException e) {
                    throw new DmiTransactionException("sub transaction of size value non numeric");
                }

                int mioLevel = 0;
                // set mio level, ignore errors (it defaults to zero)
                try {
                    mioLevel = Integer.valueOf(subCommands.get(1));
                } catch (NumberFormatException ignored) {
                }

                String[] commands = new String[subCommands.size() - 2];
                for (int y = 2; y < subCommands.size(); y++)
                    commands[y - 2] = subCommands.get(y);

                this.subTransactions.add(new DmiSubTransaction(type, mioLevel, commands));
            }
        } catch (UnsupportedEncodingException e) {
            throw new DmiTransactionException("Encoding error - " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Add a claims subrequest to this DMI Transaction
     */
    protected void addClaimsSubRequest() {
        addSubTransaction(new DmiSubTransaction(SCLMQ, 0, new String[]{""}));
    }

    /**
     * Add a hash subrequest to this DMI Transaction. No more sub transactions may be added after the hash sub request.
     *
     * @param sharedSecret Shared Secret
     */
    protected void addHashSubRequest(String sharedSecret) {
        try {
            String[] commands = new String[]{StringUtils.computeHash(this.toDmiString(), sharedSecret)};
            addSubTransaction(new DmiSubTransaction(SDHSQ, 0, commands));
            hashAdded = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new DmiTransactionException("Unable to computed has value - " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}