package org.ccctc.colleaguedmiclient.socket;


import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This class extends {@code Socket} to add an expiration time and support for pooling. When generated from a
 * {@code PoolingSocketFactory}, the {@code close} method will instead release the connection back to the
 * {@code PoolingSocketFactory}.
 *
 * @see PoolingSocketFactory
 */
public class PooledSocket extends Socket {

    /**
     * Expiration date and time of this socket
     */
    @Getter private final LocalDateTime expiration;

    private final PoolingSocketFactory factory;


    /**
     * Create a socket for the given host and port and connect.
     *
     * @param host             Host
     * @param port             Port
     * @param factory          Factory that created this socket
     * @param connectTimeoutMs Timeout waiting for socket to connect
     * @param expirationMs     Number of milliseconds before this socket expires. If set to zero this socket will only be
     *                         used once, although its connection will stay open until a new socket request comes in and
     *                         recycles it or it is forced closed.
     * @throws UnknownHostException if the IP address of the host could not be determined.
     * @throws IOException          if an I/O error occurs when creating the socket.
     */
    PooledSocket(String host, int port, PoolingSocketFactory factory, int connectTimeoutMs, int expirationMs)
            throws UnknownHostException, IOException {
        super();
        this.factory = factory;
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
     * Release the socket back to the pool without closing it. To actually close the socket, {@code forClose}
     * should be called instead.
     *
     * @throws IOException if an I/O error occurs when releasing the socket back to the pool
     */
    @Override
    public synchronized void close() throws IOException {
        // don't close the pooled socket, instead release it back to the pool
        release();
    }


    /**
     * Recycle the socket - closing it before releasing it so it won't be used again.
     *
     * @throws IOException if an I/O error occurs when releasing the socket back to the pool
     */
    public synchronized void recycle() throws IOException {
        super.close();
        release();
    }


    /**
     * Release the socket back to the pool.
     *
     * @throws IOException if an I/O error occurs when releasing the socket back to the pool
     */
    private synchronized void release() throws IOException {
        if (factory == null) super.close();
        else factory.release(this);
    }
}