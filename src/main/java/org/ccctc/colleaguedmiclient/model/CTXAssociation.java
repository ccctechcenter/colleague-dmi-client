package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Getter;

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
@Getter
@Builder
public class CTXAssociation {

    /**
     * Association name
     */
    private final String assocName;

    /**
     * Alias of association
     */
    private final String assocAliasName;

    /**
     * Range
     */
    private final String assocRange;

    /**
     * Type
     */
    private final String assocType;

    /**
     * List of members
     */
    private final String[] assocMembers;

}