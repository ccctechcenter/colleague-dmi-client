package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata about associations in a Colleague Transaction (CTX). This data comes from the Colleague Transaction
 * GET.CTX.DETAILS.
 * <p>
 * These associations refer to any input/output variables of a Colleague Transaction that have been grouped together on
 * the "Groups" tab in Colleague Studio.
 * <p>
 * Example:
 * A Colleague Transaction returns a list of first and last names for matching persons. Two multi-valued variables are
 * returned: TV.FIRST.NAMES and TV.LAST.NAMES. Each of the these variables on its own is an array of strings. By associating
 * them, we are saying that first name #1 should be grouped with last name #1, first name #2 should be grouped with last
 * name #2, etc.
 */
@Data
@Builder
public class CTXAssociation {

    private final String assocName;
    private final String assocAliasName;
    private final String assocRange;
    private final String assocType;
    private final String[] assocMembers;

}