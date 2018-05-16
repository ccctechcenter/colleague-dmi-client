package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Metadata about elements in a Colleague Transaction (CTX). This data comes from the Colleague Transaction
 * GET.CTX.DETAILS.
 * <p>
 * These "elements" refer to the database elements that are used by the process to read/write data. In Colleague Studio
 * these are seen at the top of the "Elements" tab where it says "List of elements in the transaction".
 */
@Getter
@Builder
public class CTXElement {

    /**
     * Element name
     */
    private final String elementName;

    /**
     * Points to file
     */
    private final String pointsToFile;

    /**
     * Required
     */
    private final String elementRequired;

    /**
     * Direction
     */
    private final String elementDirection;

    /**
     * Element alias
     */
    private final String elementAliasName;

    /**
     * Display only
     */
    private final String elementDispOnly;

    /**
     * Conversion
     */
    private final String elementConv;

    /**
     * Group
     */
    private final String elementGroup;

    /**
     * Is boolean
     */
    private final String elementIsBool;

    /**
     * Size
     */
    private final String elementSize;

    /**
     * Data type
     */
    private final String elementDataType;

}