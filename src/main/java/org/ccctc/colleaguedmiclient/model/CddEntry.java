package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

/**
 * CDD (common data dictionary) entry from Colleague. This represents information about a single data element in Colleague.
 */
@Data
@Builder
public class CddEntry {

    /**
     * Name of the field
     */
    private final String name;

    /**
     * Physical name of the field (if applicable)
     */
    private final String physName;

    /**
     * Name of the table that this CDD entry is associated with
     */
    private final String source;

    /**
     * Maximum storage size. For string values, this typically is the maximum number of characters. This field is
     * optional - if not populated for a string value, defaultDisplaySize will reflect the maximum number of characters.
     */
    private final Integer maximumStorageSize;

    /**
     * Location in the table of the field. The first field is 1. Blank for computed columns.
     */
    private final Integer fieldPlacement;

    /**
     * Database usage type:
     * <p>
     * A = Association
     * C = Computed Column ? (old value - should be I, but these still persist it appears)
     * D = Data
     * I = Computed Column
     * K = Key
     * L = List
     * Q = Multi-valued pointer
     * X = Single-valued pointer
     */
    private final String databaseUsageType;

    /**
     * Default display size
     */
    private final String defaultDisplaySize;

    /**
     * Format string. Indicates default display size and justification.
     */
    private final String informFormatString;

    /**
     * Conversion string. Indicates data type:
     * <p>
     * The prefix is as follows:
     * MD = Decimal / numeric
     * D  = Date
     * MT = Time
     */
    private final String informConversionString;

    /**
     * Data type (not typically used)
     */
    private final String dataType;

    /**
     * Name of association (if this is part of one)
     */
    private final String elementAssocName;

    /**
     * Type of element in the association (K = key, D = data)
     */
    private final String elementAssocType;
}
