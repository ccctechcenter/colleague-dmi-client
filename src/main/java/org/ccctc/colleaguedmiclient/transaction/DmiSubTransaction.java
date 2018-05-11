package org.ccctc.colleaguedmiclient.transaction;

import lombok.Data;

/**
 * Sub transaction of a DMI transaction
 */
@Data
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