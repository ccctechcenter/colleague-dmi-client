package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

/**
 * CDD Entry with associated Java type, array, and numeric scale.
 */
@Data
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
     * Scale (for numeric types)
     */
    private final Integer scale;
}