package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CDD Entry with associated Java type, array, and numeric scale.
 */
@Getter
@AllArgsConstructor
public class CddEntryType {

    /**
     * CDD Entry
     */
    private final CddEntry cddEntry;

    /**
     * Type
     */
    private final Class type;

    /**
     * Is multi-valued
     */
    private final boolean isArray;

    /**
     * Is numeric
     */
    private final boolean isNumeric;

    /**
     * Is date
     */
    private final boolean isDate;

    /**
     * Is time
     */
    private final boolean isTime;

    /**
     * Scale (for numeric types)
     */
    private final Integer scale;
}