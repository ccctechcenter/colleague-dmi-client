package org.ccctc.colleaguedmiclient.service;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.exception.DmiServiceException;
import org.ccctc.colleaguedmiclient.exception.DmiTransactionException;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
import org.ccctc.colleaguedmiclient.socket.PooledSocket;
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;
import org.ccctc.colleaguedmiclient.socket.PoolingSocketFactory;
import org.ccctc.colleaguedmiclient.transaction.LoginRequest;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service to handle communication with the DMI. Makes use of a {@code PoolingSocketFactory} to pool connections.
 * Authentication (via a login request) is handled automatically.
 */
public class DmiService implements Closeable {

    private final Log log = LogFactory.getLog(DmiService.class);

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
    @Getter private final String password;

    /**
     * Host / IP Address
     */
    @Getter private final String host;

    /**
     * Port
     */
    @Getter private final int port;

    /**
     * Use a secure connection?
     */
    @Getter private final boolean secure;

    /**
     * Host name override
     */
    @Getter private final String hostnameOverride;

    /**
     * Shared Secret
     */
    @Getter private final String sharedSecret;

    /**
     * Authorization expiration in milliseconds. Defaults to 4 hours.
     */
    @Getter @Setter private long authorizationExpirationMs = 4 * 60 * 60 * 1000;

    /**
     * Maximum retry on a DMI socket error.
     */
    @Getter @Setter private int maximumConnectionRetry = 5;

    /**
     * Pooling socket factory used by this service to send and receive data from the DMI.
     */
    @Getter private final PoolingSocketFactory socketFactory;

    private SessionCredentials sessionCredentials;

    // object used to synchronize on when login() is called
    private final Object loginLock = new Object();

    /**
     * Create and configure the DMI Service
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
        this.account = account;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.hostnameOverride = hostnameOverride;
        this.sharedSecret = sharedSecret;

        // @TODO - configuration for each below

        // @TODO - secure connection and hostname override

        socketFactory = new PoolingSocketFactory(host, port, poolSize);
    }

    /**
     * Get session credentials from the last successful login request
     *
     * @return Session credentials
     */
    public SessionCredentials getSessionCredentials() {
        if (!isActive())
            login(true);

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
     * Perform a keep alive against the DMI
     */
    private void keepAlive() {
        if (!isActive())
            login(true);

        // @TODO
    }

    /**
     * Login to the DMI, but only if the connection is not already active or if force is set to true
     *
     * @param force Force a new login request
     */
    public void login(boolean force) {
        if (isActive() && !force) return;

        synchronized (loginLock) {
            LoginRequest loginRequest = new LoginRequest(account, username, password);
            DmiTransaction result = send(loginRequest);

            //
            // a successful login request should result in a token and control ID
            //

            if (result != null
                    && result.getToken() != null
                    && result.getControlId() != null
                    && result.getToken().length > 0
                    && result.getControlId().length > 0) {
                sessionCredentials = new SessionCredentials(result.getToken()[0], result.getControlId()[0],
                        LocalDateTime.now().plus(authorizationExpirationMs, ChronoUnit.MILLIS));
            } else {
                sessionCredentials = null;
                throw new DmiServiceException("Login request did not return a proper result");
            }
        }
    }

    /**
     * Send a transaction to the DMI and return the response
     *
     * @param transaction DMI Transaction
     * @return Response
     */
    public DmiTransaction send(DmiTransaction transaction) {
        int x = 0;
        boolean fatal = false;

        while(true) {
            Exception ex = null;
            PooledSocket socket = null;
            DataOutputStream os = null;
            DataInputStream is = null;

            try {
                socket = socketFactory.getSocket();
                os = new DataOutputStream(socket.getOutputStream());
                is = new DataInputStream(socket.getInputStream());
                byte[] bytes = transaction.toDmiBytes();

                if (log.isDebugEnabled())
                    log.debug("DMI send: " + transaction.debugInfo());

                os.write(bytes);

                DmiTransaction response =  DmiTransaction.fromResponse(is);

                if (log.isDebugEnabled())
                    log.debug("DMI recv: " + transaction.debugInfo());

                // check to see if the response is an error
                DmiSubTransaction sub1 = (response.getSubTransactions().size() > 0)
                        ? response.getSubTransactions().get(0) : null;

                if (sub1 != null && sub1.getTransactionType().equals("SERRS")) {
                    String err = String.join(", ", sub1.getCommands());
                    throw new DmiTransactionException(err);
                }


                return response;
            } catch (Exception e) {

                log.error("DMI Service error (may retry) - " + e.getClass().getName() + ": " + e.getMessage());

                // @TODO - determine what might constitute a "fatal" error - for example username/password error

                // @TODO - determine when an error means our credentials have expired and we need to log back in

                ex = e;
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

            // sleep 250 ms before attempting again
            try {
                Thread.sleep(250);
            } catch (InterruptedException i) {
                throw new DmiServiceException("Error connecting to Colleague - " + ex.getClass().getName() + ": " + ex.getMessage());
            }

            if (++x >= maximumConnectionRetry || fatal) {
                throw new DmiServiceException("Error connecting to Colleague - " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Close the connection pool associated with the DMI Service
     *
     * @throws IOException if closing the connection pool causes an error
     */
    @Override
    public void close() throws IOException {
        this.socketFactory.close();
    }
}