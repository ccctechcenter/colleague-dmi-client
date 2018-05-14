package org.ccctc.colleaguedmiclient.exception;

public class DmiMetadataException extends RuntimeException {
    public DmiMetadataException(String message) {
        super(message);
    }

    public DmiMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
