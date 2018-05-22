package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Sub transaction of a DMI transaction
 */
@Getter
@AllArgsConstructor
public class DmiSubTransaction {

    /**
     * Transaction type
     */
    private final String transactionType;

    /**
     * MIO level
     */
    private final int mioLevel;

    /**
     * Commands
     */
    private final String[] commands;
}