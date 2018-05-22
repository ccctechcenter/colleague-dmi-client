package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Metadata about variables in a Colleague Transaction (CTX). This data comes from the Colleague Transaction
 * GET.CTX.DETAILS.
 * <p>
 * These values correspond to the list of input/output variables on the "Elements" tab in Colleague Studio.
 */
@Getter
@Builder
public class CTXVariable {

    /**
     * Variable name
     */
    private final String varName;

    /**
     * Alias of variable
     */
    private final String varAliasName;

    /**
     * Required
     */
    private final String varRequired;

    /**
     * Direction (IN, OUT, INOUT)
     */
    private final String varDirection;

    /**
     * Data type
     */
    private final String varDataType;

    /**
     * Conversion
     */
    private final String varConv;

    /**
     * Group (for association types)
     */
    private final String varGroup;

    /**
     * Is boolean
     */
    private final String varIsBool;

    /**
     * Size
     */
    private final String varSize;

    /**
     * Is URI
     */
    private final String varIsUri;

}