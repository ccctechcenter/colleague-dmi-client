package org.ccctc.colleaguedmiclient.transaction;

import lombok.NonNull;
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;

/**
 * Login request transaction to authenticate with the DMI
 */
public class LoginRequest extends DmiTransaction {

    private static final String LGRQ = "LGRQ";
    private static final String SLGRQ = "SLGRQ";

    /**
     * Create a login request against a particular account (ie test_rt). Authentication is done via a DMI account
     * (typically setup on DRUS).
     *
     * @param account  Colleague account (ie test_rt)
     * @param username DMI username
     * @param password DMI password
     */
    public LoginRequest(@NonNull String account, @NonNull String username, @NonNull String password) {
        this(account, username, password, null, null, null);
    }

    /**
     * Create a login request against a particular account (ie test_rt). Authentication is done via a DMI account
     * (typically setup on DRUS).
     *
     * @param account         Colleague account (ie test_rt)
     * @param username        DMI username
     * @param password        DMI password
     * @param initialMnemonic Initial mnemonic
     * @param personId        Person ID
     * @param loginAttributes Login attributes
     */
    public LoginRequest(@NonNull String account, @NonNull String username, @NonNull String password,
                        String initialMnemonic, String personId, String loginAttributes) {
        super(account, LGRQ, "UT", null, null);

        /*

        Not needed at this time.

        super.addClaimsSubRequest();
        */

        DmiSubTransaction sub = new DmiSubTransaction(SLGRQ, 0,
                new String[]{username, password, initialMnemonic, personId, loginAttributes});

        super.addSubTransaction(sub);
    }
}