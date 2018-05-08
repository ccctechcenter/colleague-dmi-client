package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DMI Session Credentials
 */
@Data
public class SessionCredentials {
    /**
     * Token
     */
    private final String token;

    /**
     * Control ID
     */
    private final String controlId;

    /**
     * Expiration date/time
     */
    private final LocalDateTime expirationDateTime;
}
