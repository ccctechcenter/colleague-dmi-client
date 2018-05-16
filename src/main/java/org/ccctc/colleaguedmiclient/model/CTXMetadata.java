package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Metadata for a Colleague Transaction (CTX).
 * <p>
 * These values are retrieved by calling the Colleague Transaction GET.CTX.DETAILS.
 */
@Getter
@Builder
public class CTXMetadata {

    /**
     * Alias for the transaction
     */
    private final String prcsAliasName;

    /**
     * Is anonymous
     */
    private final String isAnonymousCtx;

    /**
     * Is inquiry only
     */
    private final String prcsInquiryOnly;

    /**
     * Version
     */
    private final Integer prcsVersion;

    /**
     * Variables
     */
    private final List<CTXVariable> variables;

    /**
     * Elements
     */
    private final List<CTXElement> elements;

    /**
     * Associations
     */
    private final List<CTXAssociation> associations;
}