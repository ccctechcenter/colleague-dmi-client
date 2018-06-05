package org.ccctc.colleaguedmiclient.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This class extends {@code Socket} to add an expiration time for use by the {@code PoolingSocketFactory}
 *
 * @see PoolingSocketFactory
 */
public class PooledSocket extends Socket {

    /**
     * Expiration date and time of this socket
     */
    private final LocalDateTime expiration;

    /**
     * Create a socket for the given host and port and connect.
     *
     * @param host             Host
     * @param port             Port
     * @param connectTimeoutMs Timeout waiting for socket to connect
     * @param expirationMs     Number of milliseconds before this socket expires. If set to zero this socket will only be
     *                         used once, although its connection will stay open until a new socket request comes in and
     *                         recycles it or it is forced closed.
     * @throws UnknownHostException if the IP address of the host could not be determined.
     * @throws IOException          if an I/O error occurs when creating the socket.
     */
    PooledSocket(String host, int port, int connectTimeoutMs, int expirationMs)
            throws UnknownHostException, IOException {
        super();
        expiration = LocalDateTime.now().plus(expirationMs, ChronoUnit.MILLIS);

        this.connect(new InetSocketAddress(host, port), connectTimeoutMs);
    }

    /**
     * Check whether the socket is expired.
     *
     * @return true is the socket is expired
     */
    public boolean isExpired() {
        return expiration.isBefore(LocalDateTime.now());
    }

    /**
     * General information about the status of the socket
     *
     * @return Socket info
     */
    @Override
    public String toString() {
        return "PooledSocket{" +
                "hashCode=" + hashCode() +
                ", isConnected=" + isConnected() +
                ", isClosed=" + isClosed() +
                ", isBound=" + isBound() +
                ", isExpired=" + isExpired() +
                '}';
    }
}