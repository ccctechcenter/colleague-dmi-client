package org.ccctc.colleaguedmiclient.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.CddEntryType;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;

public class CddUtils {

    private static final Log log = LogFactory.getLog(CddUtils.class);

    private static final String EMPTY_STRING = "";

    /**
     * Get data type information from a CDD Entry, including Java type, array (true/false) and numeric scale
     *
     * @param cddEntry CDD Entry
     * @return CDD Entry w/ type information
     */
    public static CddEntryType cddEntryType(CddEntry cddEntry) {

        String conversion = cddEntry.getInformConversionString();
        Integer maxStorage = cddEntry.getMaximumStorageSize();
        Integer displaySize = StringUtils.parseIntOrNull(cddEntry.getDefaultDisplaySize());
        String dataType = cddEntry.getDatabaseUsageType();

        Class type = String.class;
        boolean isArray = false;
        Integer scale = null;

        //
        // types L (list), A (association) and Q (multi-valued pointer) are array types
        //
        if (dataType != null && (dataType.equals("L") || dataType.equals("A") || dataType.equals("Q")))
            isArray = true;

        if (conversion != null) {

            // date
            if (conversion.charAt(0) == 'D')
                type = LocalDate.class;

                // time
            else if (conversion.length() >= 2 && conversion.substring(0, 2).equals("MT"))
                type =  LocalTime.class;

                // decimal
            else if (conversion.length() >= 2 && conversion.substring(0, 2).equals("MD")) {
                //
                // in colleague, decimal values are stored without the decimal point, the scale is
                // specified in the data type as the 3rd or 4th character.
                // Examples:
                // MD25 = scale of 5
                // MD2 = scale of 2
                // MD0 = scale of 0 (integer or long value depending on "length")
                // MD  = same as MD0
                //
                // Note: other format values could follow the number, for example MD0, MD25$, etc  which specify
                // commas, negatives, currency, etc. Therefore we need to ignore any non-numeric characters.
                //

                // attempt to parse the number at position 2 or 3, defaulting back to the already discovered value if conversion fails
                scale = 0;
                if (conversion.length() >= 3) scale = StringUtils.parseIntOrDefault(conversion.substring(2, 3), scale);
                if (conversion.length() >= 4) scale = StringUtils.parseIntOrDefault(conversion.substring(3, 4), scale);

                if (scale == 0) {
                    // if maximum storage is null, display size will max size of the field
                    Integer numericSize = maxStorage != null ? maxStorage : displaySize;

                    // Long or Integer depending on size
                    type = (numericSize != null && numericSize > 9) ? Long.class : Integer.class;
                } else {
                    // BigDecimal
                    type = BigDecimal.class;
                }
            }
        }

        return new CddEntryType(cddEntry, type, isArray, scale);
    }

    /**
     * Convert a string to a Java type based on a CDD Entry. The resultant type may be one of the following types:
     * {@code String}, {@code Integer}, {@code Long}, {@code BigDecimal}, {@code LocalDate}, {@code LocalTime}.
     * <p>
     * The result will either be single-value or multi-valued based on the data type. Multi-valued types are returned
     * as arrays, ie String[], Integer[], etc.
     * <p>
     * Note: For string values, an empty string is converted to null.
     *
     * @param value    Value to convert
     * @param cddEntry CDD Entry
     * @return Converted value
     */
    public static Object convertToValue(String value, CddEntry cddEntry) {
        if (value == null) return null;

        CddEntryType cddEntryType = cddEntryType(cddEntry);

        if (cddEntryType.isArray()) {
            String[] strSplit = StringUtils.split(value, StringUtils.VM);
            Object[] array = (Object[]) Array.newInstance(cddEntryType.getType(), strSplit.length);
            for (int x = 0; x < strSplit.length; x++) {
                array[x] = convertOneValue(strSplit[x], cddEntryType);
            }

            return array;
        }

        return convertOneValue(value, cddEntryType);
    }

    /**
     * Convert a Java type to a String value for serialization to the DMI in the appropriate String format.
     *
     * @param value    Java type to convert
     * @param cddEntry CDD Entry
     * @return Converted value
     */
    public static String convertFromValue(Object value, CddEntry cddEntry) {
        // @TODO
        return null;
    }

    private static Object convertOneValue(String value, CddEntryType cddEntryType) {

        Class c = cddEntryType.getType();
        Integer scale = cddEntryType.getScale();

        try {
            if (c == String.class)
                return EMPTY_STRING.equals(value) ? null : value;

            if (c == LocalDate.class)
                return StringUtils.dateFromString(value);

            if (c == LocalTime.class)
                return StringUtils.timeFromString(value);

            if (c == Long.class)
                return Long.valueOf(value);

            if (c == Integer.class)
                return Integer.valueOf(value);

            if (c == BigDecimal.class)
                return new BigDecimal(new BigInteger(value), scale);

        } catch (NumberFormatException e) {
            log.warn("Unable to convert value to numeric equivalent. Null will be returned instead. Value = "
                    + value + ", conversion  = " + cddEntryType.getCddEntry().getInformConversionString());
        }

        return null;
    }
}
