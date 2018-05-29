package org.ccctc.colleaguedmiclient.transaction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.ByteSplitRemainder;
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;
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
import static org.ccctc.colleaguedmiclient.util.StringUtils.*;

@Getter
@Setter(value = AccessLevel.PROTECTED)
@ToString(exclude = {"log", "rawResponse"})
public class DmiTransaction {

    private final Log log = LogFactory.getLog(DmiTransaction.class);

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
     * The second element is the DAS connect string.
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
    private List<DmiSubTransaction> subTransactions = new ArrayList<>();

    /**
     * Has a hash been added to this request? If so subrequests are closed.
     */
    private boolean hashAdded = false;

    /**
     * DMI response, split at the @FM delimiter, so that each item in the list
     * is a line of the response.
     */
    private List<String> rawResponse;

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

        this.createdBy = "CoreWS" + Character.toString(VM) + "2.0";
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
            throw new DmiTransactionException("Attempted to add sub transaction to a DMI transaction after hash subrequest", this);

        subTransactions.add(subTransaction);
    }

    /**
     * Append a delimiter followed by a string value to the string builder
     *
     * @param stringBuilder String builder
     * @param value         Value
     * @param delimiter     Delimiter
     */
    private void append(StringBuilder stringBuilder, String value, char delimiter) {
        stringBuilder.append(delimiter);
        if (value != null) stringBuilder.append(value);
    }

    /**
     * Append a delimiter followed by each value of a string array delimiteed by a sub delimiter
     *
     * @param StringBuilder String builder
     * @param value         Value
     * @param delimiter     Delimiter
     * @param subDelimiter  Sub delimiter
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

    /**
     * Convert this DMI transaction to a DMI String
     *
     * @return String
     */
    public String toDmiString() {
        StringBuilder b = new StringBuilder();

        // the first first 16 values are common to all DMI transactions
        if (this.dmi != null) b.append(this.dmi);
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
     * <p>
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
            throw new DmiTransactionException("Encoding error - " + e.getClass().getName() + ": " + e.getMessage(), this);
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
            while (true) {
                headerBytes++;
                byte bite = is.readByte();
                if (bite == '#') {
                    if (inHeader) break;
                    inHeader = true;
                } else size += Character.toString((char) bite);
            }

            if (size.length() == 0)
                throw new DmiTransactionException("Empty header", this);

            try {
                responseSize = Long.valueOf(size);
            } catch (NumberFormatException e) {
                throw new DmiTransactionException("Invalid header size (non-numeric)", this);
            }
        } catch (IOException e) {
            throw new DmiTransactionException("Problem processing DMI response - " + e.getClass().getName() + ": " + e.getMessage(), this);
        }

        this.transactionBytes = headerBytes + responseSize;

        //
        // read the rest of the response in chunks
        //

        List<String> results = new ArrayList<>();
        long totalRead = 0L;
        byte[] data = new byte[CHUNK_SIZE];
        byte[] remainder = null;
        while (true) {
            try {
                int bytesRead = is.read(data);
                if (bytesRead == -1)
                    throw new DmiTransactionException("Encountered EOF before end of response", this);

                totalRead += bytesRead;

                // split the chunk into lists of byte arrays delimited by FM and a remainder (the last value)
                // the remainder is kept separate as the next chunk may contain more data for that response line
                ByteSplitRemainder byteSplitResult = ByteUtils.byteSplit(data, bytesRead, (byte) FM);

                // add split values to the result
                if (byteSplitResult.getSplit().size() > 0) {
                    List<byte[]> split = byteSplitResult.getSplit();

                    byte[] first = split.get(0);

                    if (remainder == null || remainder.length == 0) {
                        results.add(byteArrayToString(first));
                    } else {
                        // combine remainder in to first result
                        byte[] combined = new byte[remainder.length + first.length];
                        System.arraycopy(remainder, 0, combined, 0, remainder.length);
                        System.arraycopy(first, 0, combined, remainder.length, first.length);
                        results.add(byteArrayToString(combined));

                        remainder = null;
                    }

                    for (int x = 1; x < split.size(); x++) {
                        results.add(byteArrayToString(split.get(x)));
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

                            if (remainder.length > 5) {
                                byte[] l = Arrays.copyOfRange(remainder, 0, remainder.length - 5);
                                results.add(byteArrayToString(l));
                            } else {
                                results.add(null);
                            }

                            break;
                        }
                    }

                    throw new DmiTransactionException("Transaction end not found", this);
                }
            } catch (IOException e) {
                throw new DmiTransactionException("Problem processing response - " + e.getClass().getName() + ": " + e.getMessage(), this);
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
    private void readResponse(List<String> data) {

        // add null elements to get us up to 16
        if (data.size() < 16) {
            for (int x = data.size(); x < 16; x++) {
                data.add(null);
            }
        }

        this.rawResponse = data;

        // strings
        this.dmi = data.get(0);
        this.version = data.get(1);
        this.transactionType = data.get(2);
        this.application = data.get(4);
        this.listenerId = data.get(6);
        this.createdBy = data.get(10);
        this.inResponseTo = data.get(11);
        this.debugLevel = data.get(12);
        this.lastProcessedBy = data.get(13);

        // arrays
        this.account = (data.get(3) != null) ? split(data.get(3), VM) : null;
        this.token = (data.get(5) != null) ? split(data.get(5), VM) : null;
        this.controlId = (data.get(7) != null) ? split(data.get(7), VM) : null;

        // date / times
        this.createdDate = dateFromString(data.get(8));
        this.createdTime = timeFromString(data.get(9));
        this.lastProcessedDate = dateFromString(data.get(14));
        this.lastProcessedTime = timeFromString(data.get(15));

        // add in sub commands
        if (data.size() > 16)
            this.subTransactions = new ArrayList<>();

        for (int x = 16; x < data.size(); x++) {
            String type = data.get(x);
            String end = type + ".END";
            boolean finishedBlock = false;
            List<String> subCommands = new ArrayList<>();
            for (x++; x < data.size(); x++) {
                String val = data.get(x);
                if (val != null && val.equals(end)) {
                    finishedBlock = true;
                    break;
                }
                subCommands.add(val);
            }

            // do not save an incomplete block as some transactions appear to have lines at the end that don't follow
            // the normal format of sub commands (so this effectively ignores the extra lines)
            if (!finishedBlock) {
                log.trace("Incomplete block: of type " + type);
                break;
            }

            // check size
            if (subCommands.size() < 2)
                throw new DmiTransactionException("sub transaction of incorrect size", this);

            int sizeCheck;
            try {
                sizeCheck = Integer.valueOf(subCommands.get(0));

                if (sizeCheck != subCommands.size() + 2)
                    throw new DmiTransactionException("sub transaction size does not match content", this);

            } catch (NumberFormatException e) {
                throw new DmiTransactionException("sub transaction of size value non numeric", this);
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
    }

    /**
     * Add a claims subrequest to this DMI Transaction
     */
    protected void addClaimsSubRequest() {
        addSubTransaction(new DmiSubTransaction(SCLMQ, 0, new String[]{""}));
    }

    /**
     * Add or replace the hash subrequest in this DMI Transaction.
     * <p>
     * No more sub transactions may be added after the hash sub request.
     *
     * @param sharedSecret Shared Secret
     */
    protected void addHashSubRequest(String sharedSecret) {
        try {
            // remove existing hash request (must be last request)
            if (hashAdded && subTransactions.size() > 0) {
                DmiSubTransaction last = subTransactions.get(subTransactions.size() - 1);
                if (SDHSQ.equals(last.getTransactionType()))
                    subTransactions.remove(last);

                hashAdded = false;
            }

            // add new hash request
            String[] commands = new String[]{StringUtils.computeHash(this.toDmiString(), sharedSecret)};
            addSubTransaction(new DmiSubTransaction(SDHSQ, 0, commands));
            hashAdded = true;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new DmiTransactionException("Unable to compute hash value - " + e.getClass().getName() + ": " + e.getMessage(), this);
        }
    }


    /**
     * Set the Control ID and Token of this request and add/replace a hash subrequest w/ the shared secret
     *
     * @param token        Token
     * @param controlId    Control ID
     * @param sharedSecret Shared secret (if not specified, no hash subrequest will be added)
     */
    public void setCredentials(@NonNull String token, @NonNull String controlId, String sharedSecret) {
        if (this.token == null || this.token.length == 0)
            this.token = new String[] { token };
        else
            this.token[0] = token;

        if (this.controlId == null || this.controlId.length == 0)
            this.controlId = new String[] { controlId };
        else
            this.controlId[0] = controlId;

        if (sharedSecret != null)
            addHashSubRequest(sharedSecret);
    }
}