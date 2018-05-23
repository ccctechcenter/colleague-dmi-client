package org.ccctc.colleaguedmiclient.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.FieldOverflowException;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.CddEntryType;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class CddUtils {

    private static final Log log = LogFactory.getLog(CddUtils.class);

    // maximum and minimum from UniData documentation
    private static final long MIN_DATE = -46385L;  // December 31, 1840
    private static final long MAX_DATE = 2933628L; // December 31, 9999

    // maximum and minimum times as seconds in a day
    private static final long MIN_TIME = 0;     // 00:00:00
    private static final long MAX_TIME = 86399; // 23:59:59

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
        boolean isNumeric = false;
        boolean isDate = false;
        boolean isTime = false;
        Integer scale = null;

        //
        // types L (list), A (association) and Q (multi-valued pointer) are array types
        //
        if (dataType != null && (dataType.equals("L") || dataType.equals("A") || dataType.equals("Q")))
            isArray = true;

        if (conversion != null) {

            // date
            if (conversion.charAt(0) == 'D') {
                isDate = true;
                type = LocalDate.class;
            }

            // time
            else if (conversion.length() >= 2 && conversion.substring(0, 2).equals("MT")) {
                isTime = true;
                type = LocalTime.class;
            }

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

                isNumeric = true;

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

        return new CddEntryType(cddEntry, type, isArray, isNumeric, isDate, isTime, scale);
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
     * Convert a Java type to a String value for serialization to the DMI in the appropriate format for the data type
     * of the CDD Entry.
     * <p>
     * Numeric type handling:
     * BigDecimal, BigInteger, Double, Float, Integer and Long are supported numeric types. Standard rounding applies on conversion.
     * All other types are converted to a BigDecimal from their {@code toString()} value. {@code NumberFormatException} is
     * thrown if conversion fails.
     * The scale from the destination type is then applied, for example if the scale is 4 and the value is 1.23, the
     * result will be 12300 (Colleague stores these data types as a numeric string and scale internally).
     * <p>
     * Date / time handling:
     * LocalDate, LocalDateTime, Date - converted to UniData date
     * LocalTime, LocalDateTime, Date - converted to UniData time
     * All other types are converted to a Long from their {@code toString()} value. {@code NumberFormatException} is
     * thrown if conversion fails.
     * <p>
     * Other types:
     * Boolean - converted to Y or N
     * Any else is converted to a string using {@code toString()}
     * <p>
     * Notes:
     * 1. Null values for any data type are converted to an empty string
     * 2. Field sizes are optionally checked based on the value of {@code checkFieldSize}
     *
     * @param value          Java type to convert
     * @param cddEntry       CDD Entry
     * @param checkFieldSize Validate field size, throwing {@code FieldOverflowException} if overflow occurs?
     * @return Converted value
     * @throws NumberFormatException  if conversion to a numeric value fails (including date and time, which are numeric)
     * @throws FieldOverflowException if conversion would result in exceeding the max size of the field
     */
    public static String convertFromValue(Object value, CddEntry cddEntry, boolean checkFieldSize)
            throws NumberFormatException, FieldOverflowException {
        if (value == null) return "";

        CddEntryType cddEntryType = cddEntryType(cddEntry);

        Integer maxLength = null;
        if ("T".equals(cddEntry.getDatabaseUsageType())) {
            maxLength = 1996; // "text" field - in SQL Server is up to 1996 characters
        } else {
            maxLength = cddEntry.getMaximumStorageSize();
            if (maxLength == null) {
                try {
                    maxLength = Integer.parseInt(cddEntry.getDefaultDisplaySize());
                } catch (NumberFormatException ignored) { }
            }
        }

        // numeric type handling
        if (cddEntryType.isNumeric()) {
            BigDecimal dVal;

            Integer scale = cddEntryType.getScale();
            if (scale == null) scale = 0;

            if (value instanceof BigDecimal)
                dVal = ((BigDecimal) value);
            else if (value instanceof Double)
                dVal = new BigDecimal((double) value);
            else if (value instanceof Float)
                dVal = new BigDecimal((double) (float)value);
            else
                dVal = new BigDecimal(value.toString());

            // convert to string, check size
            if (scale > 0) dVal = dVal.movePointRight(scale);
            String sVal = dVal.setScale(0, RoundingMode.HALF_UP).toString();
            if (checkFieldSize && maxLength != null && sVal.length() > maxLength)
                throw new FieldOverflowException("Maximum size exceeded for numeric field " + cddEntry.getName());

            return sVal;
        }

        // date / time handling
        if (cddEntryType.isDate() || cddEntryType.isTime()) {
            // convert java.util.Date to LocalDate or LocalTime
            if (value instanceof Date) {
                if (cddEntryType.isDate())
                    value = ((Date)value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                else
                    value = ((Date)value).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            }

            if (cddEntryType.isDate()) {
                Long dateValue;

                if (value instanceof LocalDate)
                    dateValue = Long.parseLong(StringUtils.dateToString((LocalDate)value));
                else if (value instanceof LocalDateTime)
                    dateValue = Long.parseLong(StringUtils.dateToString(((LocalDateTime)value).toLocalDate()));
                else {
                    dateValue = Long.parseLong(value.toString());
                }

                if (checkFieldSize && (dateValue < MIN_DATE || dateValue > MAX_DATE))
                    throw new FieldOverflowException("Date value invalid - must be a number between " + MIN_DATE + " and " + MAX_DATE);

                return Long.toString(dateValue);
            } else {
                if (value instanceof LocalTime)
                    return StringUtils.timeToString((LocalTime)value);

                if (value instanceof LocalDateTime)
                    return StringUtils.timeToString(((LocalDateTime)value).toLocalTime());

                // time handling for non-LocalTime. must be a number between 0 and 86400 (number of seconds in a day).
                Long v = Long.parseLong(value.toString());
                if (checkFieldSize && (v < MIN_TIME || v > MAX_TIME))
                    throw new FieldOverflowException("Time value invalid - must be a number between " + MIN_TIME + " and  " + MAX_TIME);
                return v.toString();
            }
        }

        if (value instanceof Boolean)
            return ((Boolean)value) ? "Y" : "N";

        // anything else can be converted to a string
        String sVal = value.toString();
        if (checkFieldSize && maxLength != null && sVal.length() > maxLength)
            throw new FieldOverflowException("Maximum size exceeded for text field " + cddEntry.getName());

        return sVal;
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
