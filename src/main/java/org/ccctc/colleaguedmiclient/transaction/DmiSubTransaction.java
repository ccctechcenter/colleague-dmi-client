package org.ccctc.colleaguedmiclient.transaction;

import lombok.Data;

@Data
public class DmiSubTransaction {
    private final String transactionType;
    private final int mioLevel;
    private final String[] commands;
}