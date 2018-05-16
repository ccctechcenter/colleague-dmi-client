package org.ccctc.colleaguedmiclient.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiServiceException;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
import org.ccctc.colleaguedmiclient.socket.PooledSocket;
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;
import org.ccctc.colleaguedmiclient.socket.PoolingSocketFactory;
import org.ccctc.colleaguedmiclient.transaction.LoginRequest;
import org.ccctc.colleaguedmiclient.transaction.SessionStateRequest;
import org.ccctc.colleaguedmiclient.util.StringUtils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Service to handle communication with the DMI. Makes use of a {@code PoolingSocketFactory} to pool connections.
 * Authentication (via a login request) is handled automatically.
 */
public class DmiService implements Closeable {

    private final Log log = LogFactory.getLog(DmiService.class);

    private final static String SERRS = "SERRS";


    /**
     * Colleague account (aka environment)
     */
    @Getter private final String account;

    /**
     * DMI Username
     */
    @Getter private final String username;

    /**
     * DMI Password
     */
    private final String password;

    /**
     * Shared Secret
     */
    @Getter(AccessLevel.PROTECTED) private final String sharedSecret;

    /**
     * Authorization expiration in seconds. Defaults to 4 hours.
     */
    @Getter @Setter private long authorizationExpirationSeconds = 4 * 60 * 60;

    /**
     * Maximum retries sending / receiving a DMI Transaction. Default is 1. This is in addition to the original
     * attempt, ie a retry of 1 means it will be tried initially then retried once if there is an error.
     */
    @Getter @Setter private int maxDmiTransactionRetry = 1;

    /**
     * Pooling socket factory used by this service to send and receive data from the DMI.
     */
    @Getter protected final PoolingSocketFactory socketFactory;

    // current active credentials
    private SessionCredentials sessionCredentials;

    // object used to synchronize on when login() is called
    private final Object loginLock = new Object();

    /**
     * Create and configure the DMI Service. Will use default PoolingSocketFactory.
     *
     * @param account          Account (aka environment)
     * @param username         DMI username
     * @param password         DMI password
     * @param host             Host / IP address
     * @param port             Port
     * @param secure           Secure connection ?
     * @param hostnameOverride Host name override
     * @param sharedSecret     Shared secret
     * @param poolSize         Pool size of the pooling socket factory
     */
    public DmiService(String account, String username, String password, String host, int port,
                      boolean secure, String hostnameOverride, String sharedSecret, int poolSize) {

        this(account, username, password, sharedSecret,
                new PoolingSocketFactory(host, port, poolSize, secure, hostnameOverride));
    }

    /**
     * Create and configure the DMI Service with an already created {@code PoolingSocketFactor}
     *
     * @param account              Account (aka environment)
     * @param username             DMI username
     * @param password             DMI password
     * @param sharedSecret         Shared secret
     * @param poolingSocketFactory Pooling Socket Factory
     */
    public DmiService(String account, String username, String password, String sharedSecret,
                      PoolingSocketFactory poolingSocketFactory) {
        this.account = account;
        this.username = username;
        this.password = password;
        this.sharedSecret = sharedSecret;
        this.socketFactory = poolingSocketFactory;
    }

    /**
     * Get session credentials from the last successful login request. If there are no credentials or if the current
     * credentials are expired, a new login attempt will be performed.
     *
     * @return Session credentials
     * @throws DmiServiceException if a new login attempt fails
     */
    public SessionCredentials getSessionCredentials() throws DmiServiceException {
        if (!isActive())
            return login(false);

        return sessionCredentials;
    }

    /**
     * Determine whether the DMI credentials are active. It does not check whether a connection with the DMI with
     * these credentials is possible, only that they have not expired.
     *
     * @return true or false
     */
    public boolean isActive() {
        return sessionCredentials != null
                && LocalDateTime.now().isBefore(sessionCredentials.getExpirationDateTime());
    }

    /**
     * Perform a keep alive against the DMI, ensuring the credentials are still active.  This attempts a "get session
     * state request" which should validate the credentials and be a low impact transaction.
     * <p>
     * This method may be scheduled (using your favorite scheduler) to fire every so often to keep a persistent connection
     * the DMI going by keeping the token active and re-connecting as necessary.
     *
     * @throws DmiServiceException if the keep alive attempt fails
     */
    public void keepAlive() throws DmiServiceException {
        SessionCredentials creds = login(false);
        DmiTransaction transaction = new SessionStateRequest(account, creds.getToken(), creds.getControlId());
        DmiTransaction response = send(transaction);

        if (log.isDebugEnabled()) {
            String state = response.getSubTransactions().size() > 0 ?
                    StringUtils.join(',',
                            Arrays.asList(response.getSubTransactions().get(0).getCommands()))
                    : "unknown";

            log.debug("Keep Alive success. Session State = " + state);
        }
    }

    /**
     * Login to the DMI, but only if the connection is not already active or if force is set to true.
     *
     * @param force Force a new login request
     * @throws DmiServiceException if the login cannot request cannot be performed or if it fails
     */
    public SessionCredentials login(boolean force) throws DmiServiceException{
        if (isActive() && !force) return sessionCredentials;

        synchronized (loginLock) {
            // do this check a second time in case a login was completed while waiting for the login lock
            if (isActive() && !force) return sessionCredentials;

            LoginRequest loginRequest = new LoginRequest(account, username, password);

            log.info("Sending login request to Colleague DMI");

            DmiTransaction result;
            try {
                result = doSend(loginRequest, true);
            } catch (Exception e) {
                throw new DmiServiceException("Login request failed", e);
            }

            //
            // a successful login request should result in a token and control ID
            //

            if (result.getToken() != null
                    && result.getControlId() != null
                    && result.getToken().length > 0
                    && result.getControlId().length > 0) {
                sessionCredentials = new SessionCredentials(result.getToken()[0], result.getControlId()[0],
                        LocalDateTime.now().plus(authorizationExpirationSeconds, ChronoUnit.SECONDS));

                log.info("Received credentials from DMI. Expiration: " + sessionCredentials.getExpirationDateTime().toString());
            } else {
                sessionCredentials = null;

                // determine what went wrong - message should be in SERRS block
                DmiSubTransaction errSub = null;
                for (DmiSubTransaction sub : result.getSubTransactions()) {
                    if (SERRS.equals(sub.getTransactionType())) {
                        throw new DmiServiceException("Login request failed: " + String.join(",", sub.getCommands()));
                    }
                }

                throw new DmiServiceException("Login request failed and no credentials or error message returned");
            }

            return sessionCredentials;
        }
    }

    /**
     * Send a transaction to the DMI and return the response.
     * <p>
     * Error handling is done as part of the request and depending on the error a re-login
     * or retry of the transaction may be performed to see if it can complete the request.
     * If it cannot complete the request, it will throw a {@code DmiServiceException}.
     *
     * @param transaction DMI Transaction
     * @return Response
     * @throws DmiServiceException if the request cannot be completed
     */
    public DmiTransaction send(@NonNull DmiTransaction transaction) throws DmiServiceException {
        int attempt = 0;
        while (true) {
            Exception ex = null;
            DmiTransaction response = null;
            String errorMessage = null;
            boolean logBackIn = false;
            boolean fatal = false;

            try {
                boolean forceNewSocket = (attempt > 0);
                response = doSend(transaction, forceNewSocket);

                // check to see if the response is an error
                DmiSubTransaction errSub = null;
                for (DmiSubTransaction sub : response.getSubTransactions()) {
                    if (SERRS.equals(sub.getTransactionType())) {
                        errSub = sub;
                        break;
                    }
                }

                // on error, determine whether we need new login credentials, whether this is a known "fatal" error,
                // or whether we should retry the transaction, hoping for a better result!
                if (errSub != null) {
                    String errType = (errSub.getCommands().length) > 0 ? errSub.getCommands()[0] : null;

                    if ("SECURITY".equals(errType)) logBackIn = true;
                    if ("SET".equals(errType)) fatal = true;

                    errorMessage = String.join(", ", errSub.getCommands());
                } else {
                    return response;
                }
            } catch (Exception e) {
                ex = e;
            }

            // fatal or max retry exceeded
            if (fatal || ++attempt > maxDmiTransactionRetry) {
                if (ex != null) throw new DmiServiceException("Error sending/receiving transaction to/from DMI", ex);
                throw new DmiServiceException("DMI Transaction resulted in an error: " + errorMessage);
            }

            // credentials error - retry login
            if (logBackIn) {
                log.info("Invalid/expired credentials, attempting to log back in");
                SessionCredentials creds = login(true);

                // replace credentials and try again (if the request requires credentials, which we can test by presence of a token)
                if (transaction.getToken() != null && transaction.getToken().length > 0) {
                    transaction.setCredentials(creds.getToken(), creds.getControlId(), sharedSecret);
                }
            } else {

                // sleep 250 ms before attempting a retry
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) { }

                // on retry leave a message in the logger, but continue
                log.error("DMI Transaction Error, attempting to retry: "
                        + (ex != null ? ex.getClass().getSimpleName() + " - " + ex.getMessage() : errorMessage));

            }
        }
    }

    /**
     * Send data to the DMI and return the result
     *
     * @param transaction    DMI Transaction to send
     * @param forceNewSocket Force new socket for transaction?
     * @return DMI Transaction response
     */
    private DmiTransaction doSend(DmiTransaction transaction, boolean forceNewSocket) {
        Exception ex = null;
        PooledSocket socket = null;
        DataOutputStream os = null;
        DataInputStream is = null;
        DmiTransaction response;

        try {
            socket = socketFactory.getSocket(forceNewSocket);
            os = new DataOutputStream(socket.getOutputStream());
            is = new DataInputStream(socket.getInputStream());
            byte[] bytes = transaction.toDmiBytes();

            if (log.isTraceEnabled())
                log.trace("DMI send: " + transaction.toDmiString());

            os.write(bytes);
            response =  DmiTransaction.fromResponse(is);

            if (log.isTraceEnabled() && response.getRawResponse() != null)
                log.trace("DMI recv: " + StringUtils.join(StringUtils.FM, response.getRawResponse()));

        } catch (Exception e) {
            ex = e;
            throw new RuntimeException(e);
        } finally {
            try {
                if (socket != null) {
                    // recycle the socket on exception
                    if (ex != null) socket.recycle();
                    else socket.close();
                }
            } catch (IOException ignored) { }

            try { if (os != null) os.close(); } catch (IOException ignored) { }
            try { if (is != null) is.close(); } catch (IOException ignored) { }
        }

        return response;
    }

    /**
     * Empty the connection pool associated with the DMI Service. Any open sockets will be closed and recycled,
     * ensuring no connections remain open.
     */
    @Override
    public void close() {
        this.socketFactory.close();
    }
}