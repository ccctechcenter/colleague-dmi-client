package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DMI Session Credentials
 */
@Getter
@AllArgsConstructor
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
