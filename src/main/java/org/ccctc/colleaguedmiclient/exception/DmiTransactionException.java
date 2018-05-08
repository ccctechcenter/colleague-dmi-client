package org.ccctc.colleaguedmiclient.exception;

public class DmiTransactionException extends RuntimeException {
    public DmiTransactionException(String message) {
        super(message);
    }

    public DmiTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
