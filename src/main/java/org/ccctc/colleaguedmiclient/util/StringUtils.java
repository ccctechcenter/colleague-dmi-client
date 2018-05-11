package org.ccctc.colleaguedmiclient.util;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public final static char FM = (char) 254;
    public final static char VM = (char) 253;
    public final static char SM = (char) 252;
    public final static char TM = (char) 251;
    public final static LocalDate BASE_DATE = LocalDate.of(1967, 12, 31);

    /**
     * Convert a UniData date string to a LocalDate.
     * <p>
     * If conversion fails, null is returned.
     *
     * @param ds UniData date string
     * @return LocalDate
     */
    public static LocalDate dateFromString(String ds) {
        LocalDate result = null;
        if (ds != null) {
            try {
                long x = Long.parseLong(ds);
                result = BASE_DATE.plusDays(x);
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    /**
     * Convert a LocalDate to a UniData date string
     *
     * @param date LocalDate
     * @return UniData date string
     */
    public static String dateToString(LocalDate date) {
        if (date != null) {
            return String.valueOf(date.toEpochDay() - BASE_DATE.toEpochDay());
        }

        return null;
    }

    /**
     * Convert a UniData time string to a LocalTime.
     * <p>
     * If conversion fails, null is returned.
     *
     * @param ts UniData time string
     * @return LocalDate
     */
    public static LocalTime timeFromString(String ts) {
        LocalTime result = null;
        if (ts != null) {
            try {
                long x = Long.parseLong(ts);
                if (x < 0) x = Math.abs(x);
                if (x > 86400) x = x % 86400;
                result = LocalTime.ofSecondOfDay(x);
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    /**
     * Convert a LocalTime to a UniData time string
     *
     * @param time LocalTime
     * @return UniData time string
     */
    public static String timeToString(LocalTime time) {
        if (time != null) {
            return String.valueOf(time.toSecondOfDay());
        }

        return null;
    }


    /**
     * Split a string at a character delimiter. The normal version delivered with Java uses regex,
     * which is more complicated than necessary, so this version is tailored to the needs of this
     * package.
     *
     * @param str String
     * @param delimiter Delimiter
     * @return Split strings
     */
    public static String[] split(String str, char delimiter) {
        if (str == null) return null;

        List<String> results = new ArrayList<>();
        int start = 0;
        int next;
        do {
            next = str.indexOf(delimiter, start);
            results.add(str.substring(start, (next == -1) ? str.length() : next));
            start = next + 1;
        } while(next != -1 && start < str.length());

        return results.toArray(new String[0]);
    }


    /**
     * Join a string with a delimiter. This differs from the regular Java implementation where null values show up
     * as "null" which is not desired here (we want them to be empty string instead)
     *
     * @param delimiter Delimiter
     * @param array     Array
     * @return String
     */
    public static String join(Character delimiter, Iterable<String> array) {
        StringBuilder output = new StringBuilder();

        boolean first = true;
        for (String a : array) {
            if (first) first = false;
            else output.append(delimiter);

            if (a != null) output.append(a);
        }

        return output.toString();
    }


    /**
     * Compute an SHA1 hash on a string with the shared secret appended (if supplied). FM, VM, SM and TM characters
     * are converted to "," characters.
     * <p>
     * This represents the hashing scheme that the DMI uses to verify a DMI Transaction (other than a login request).
     *
     * @param value        Value
     * @param sharedSecret Shared secret
     * @return Hashed value
     * @throws UnsupportedEncodingException if windows-1252 encoding is not supported
     * @throws NoSuchAlgorithmException if SHA-1 is not supported
     */
    public static String computeHash(String value, String sharedSecret) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        value = value
                .replace(FM, ',')
                .replace(VM, ',')
                .replace(SM, ',')
                .replace(TM, ',');

        if (sharedSecret != null) value += sharedSecret;

        MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = value.getBytes("windows-1252");
        msdDigest.update(bytes, 0, bytes.length);
        return DatatypeConverter.printHexBinary(msdDigest.digest());
    }

    /**
     * Parse a string to an integer and return the value or null if conversion is not possible (rather than throwing
     * an exception).
     *
     * @param value String
     * @return Integer
     */
    public static Integer parseIntOrNull(String value) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) { }
        }

        return null;
    }

    /**
     * Parse a string to an integer and return the value or a default if conversion is not possible (rather than throwing
     * an exception).
     *
     * @param value        String
     * @param defaultValue Default Value
     * @return int
     */
    public static int parseIntOrDefault(String value, int defaultValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) { }
        }

        return defaultValue;
    }
}