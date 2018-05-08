package org.ccctc.colleaguedmiclient.util;

import org.ccctc.colleaguedmiclient.model.ByteSplitRemainder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for byte and string conversion. All operations use Windows-1252 encoding.
 */
public class ByteUtils {

    /**
     * Split a byte array at a delimiter and return a ByteSplitRemainder with the split values
     * plus the remainder after the last delimiter. This is a little different than a normal
     * split operation in that the last value is the "remainder" instead of part of the split.
     * <p>
     * This is useful for processing a chunk of bytes from a stream while the stream is not yet
     * complete - the last value in the chunk is the remainder as the next chunk will continue
     * that value.
     *
     * @param array     Byte array
     * @param length    Length of byte array
     * @param delimiter Delimiter
     * @return Split array and remainder
     */
    public static ByteSplitRemainder byteSplit(byte[] array, int length, byte delimiter) {
        List<byte[]> byteArrays = new ArrayList<>();
        byte[] remainder = null;
        int begin = 0;
        for (int i = 0; i < length; i++) {
            if (array[i] == delimiter) {
                byteArrays.add(Arrays.copyOfRange(array, begin, i));
                begin = i + 1;
            }
        }

        if (begin < length) {
            remainder = Arrays.copyOfRange(array, begin, length);
        }

        return new ByteSplitRemainder(byteArrays, remainder);
    }

    /**
     * Convert a byte array to a String in windows-1252 format.
     * <p>
     * If bytes is empty, null will be returned.
     *
     * @param bytes Byte array
     * @return String
     * @throws UnsupportedEncodingException if windows-1252 encoding is not supported
     */
    public static String byteArrayToString(byte[] bytes) throws UnsupportedEncodingException {
        if (bytes.length == 0) return null;
        return new String(bytes, "windows-1252");
    }

    /**
     * Convert a byte array to a String in windows-1252, then split into an array by using the supplied delimiter/
     * <p>
     * If bytes is empty, an empty String array will be returned.
     *
     * @param bytes     Byte array
     * @param delimiter Delimiter
     * @return String array
     * @throws UnsupportedEncodingException if windows-1252 encoding is not supported
     */
    public static String[] byteArrayToStringArray(byte[] bytes, char delimiter) throws UnsupportedEncodingException {
        String str = byteArrayToString(bytes);
        if (str == null) return new String[0];

        return StringUtils.split(str, delimiter);
    }
}