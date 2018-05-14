package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata about variables in a Colleague Transaction (CTX). This data comes from the Colleague Transaction
 * GET.CTX.DETAILS.
 * <p>
 * These values correspond to the list of input/output variables on the "Elements" tab in Colleague Studio.
 */
@Data
@Builder
public class CTXVariable {

    private final String varName;
    private final String varAliasName;
    private final String varRequired;
    private final String varDirection;
    private final String varDataType;
    private final String varConv;
    private final String varGroup;
    private final String varIsBool;
    private final String varSize;
    private final String varIsUri;

}