package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata about elements in a Colleague Transaction (CTX). This data comes from the Colleague Transaction
 * GET.CTX.DETAILS.
 * <p>
 * These "elements" refer to the database elements that are used by the process to read/write data. In Colleague Studio
 * these are seen at the top of the "Elements" tab where it says "List of elements in the transaction".
 */
@Data
@Builder
public class CTXElement {

    private final String elementName;
    private final String pointsToFile;
    private final String elementRequired;
    private final String elementDirection;
    private final String elementAliasName;
    private final String elementDispOnly;
    private final String elementConv;
    private final String elementGroup;
    private final String elementIsBool;
    private final String elementSize;
    private final String elementDataType;

}