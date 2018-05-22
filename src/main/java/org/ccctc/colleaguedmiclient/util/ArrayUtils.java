package org.ccctc.colleaguedmiclient.util;

/**
 * Utilities for arrays
 */
public class ArrayUtils {

    private final static String EMPTY_STRING = "";

    /**
     * Utility that gets a value from a string array, but only if the array is large enough and only
     * if the value is not an empty string. Otherwise null is returned.
     *
     * @param array String array
     * @param index Index in array
     * @return String
     */
    public static String getAt(String[] array, int index) {
        if (array == null || array.length <= index) return null;
        String q = array[index];
        if (q == null || EMPTY_STRING.equals(q)) return null;
        return q;
    }


    /**
     * Utility with the same functionality as {@code getAt} but that additionally parses the value
     * and returns an integer value.
     * <p>
     * Leading and trailing spaces are removed from the String prior to parsing.
     * <p>
     * If the array is null or the index is beyond the size of the array, null is returned.
     * <p>
     * If the value at the index is null, empty or only spaces, null is returned.
     * <p>
     * If the value cannot be parsed into an integer, {@code NumberFormatException} is thrown.
     *
     * @param array String array
     * @param index Index in array
     * @return Integer
     * @throws NumberFormatException if the string can't be parsed into an integer
     */
    public static Integer getAtInt(String[] array, int index) throws NumberFormatException {
        if (array == null || array.length <= index) return null;
        String q = array[index];
        if (q == null) return null;

        q = q.trim();
        if (EMPTY_STRING.equals(q)) return null;

        return Integer.parseInt(q);
    }

    /**
     * Utility with the same functionality as {@code getAt} but that additionally parses the value
     * and returns an integer value.
     * <p>
     * Leading and trailing spaces are removed from the String prior to parsing.
     * <p>
     * If the array is null or the index is beyond the size of the array, null is returned.
     * <p>
     * If the value at the index is null, empty or only spaces, null is returned.
     * <p>
     * If the value cannot be parsed into an integer, null is returned.
     *
     * @param array String array
     * @param index Index in array
     * @return Integer
     */
    public static Integer getAtIntOrNull(String[] array, int index) {
        try {
            return getAtInt(array, index);
        } catch (NumberFormatException ignored) { }

        return null;
    }
}
