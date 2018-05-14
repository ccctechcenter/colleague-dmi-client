package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Metadata for a Colleague Transaction (CTX).
 * <p>
 * These values are retrieved by calling the Colleague Transaction GET.CTX.DETAILS.
 */
@Data
@Builder
public class CTXMetadata {

    private final String prcsAliasName;
    private final String isAnonymousCtx;
    private final String prcsInquiryOnly;
    private final Integer prcsVersion;

    private final List<CTXVariable> variables;
    private final List<CTXElement> elements;
    private final List<CTXAssociation> associations;

}