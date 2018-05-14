package org.ccctc.colleaguedmiclient.util;

/**
 * Utilities for arrays
 */
public class ArrayUtils {

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
        if ("".equals(q)) return null;
        return q;
    }


    /**
     * Utility with the same functionality as {@code getAt} but that additionally parses the value
     * and returns an integer value. If the value is null, empty or is beyond the size of the array null is returned.
     * If the value cannot be parsed into an integer, {@code NumberFormatException} is thrown.
     *
     * @param array String array
     * @param index Index in array
     * @return Integer
     * @throws NumberFormatException if the string can't be parsed into an integer
     */
    public static Integer getAtInt(String[] array, int index) throws NumberFormatException {
        if (array.length <= index) return null;
        String q = array[index];
        if ("".equals(q)) return null;

        return Integer.parseInt(q);
    }
}
