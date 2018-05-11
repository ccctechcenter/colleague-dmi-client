package org.ccctc.colleaguedmiclient.transaction;

public class SessionStateRequest extends DmiTransaction {

    private final static String UT = "UT";
    private final static String GSTRQ = "GSTRQ";

    public SessionStateRequest(String account, String token, String controlId) {
        super(account, GSTRQ, UT, token, controlId);
    }
}
