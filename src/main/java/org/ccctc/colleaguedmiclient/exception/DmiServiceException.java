package org.ccctc.colleaguedmiclient.exception;

public class DmiServiceException extends RuntimeException {
    public DmiServiceException(String message) {
        super(message);
    }

    public DmiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
