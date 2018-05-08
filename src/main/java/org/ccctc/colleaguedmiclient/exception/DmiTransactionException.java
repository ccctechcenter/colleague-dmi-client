package org.ccctc.colleaguedmiclient.exception;

import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;

public class DmiTransactionException extends RuntimeException {
    private final DmiTransaction dmiTransaction;

    public DmiTransactionException(String message) {
        super(message);
        this.dmiTransaction = null;
    }

    public DmiTransactionException(String message, DmiTransaction dmiTransaction) {
        super(message);
        this.dmiTransaction = dmiTransaction;
    }
}
