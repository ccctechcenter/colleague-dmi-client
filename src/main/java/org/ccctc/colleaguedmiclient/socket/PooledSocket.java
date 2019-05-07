package org.ccctc.colleaguedmiclient.socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This class extends {@code Socket} to add an expiration time for use by the {@code PoolingSocketFactory}
 *
 * @see PoolingSocketFactory
 */
public class PooledSocket implements Closeable {

    /**
     * Expiration date and time of this socket
     */
    private final LocalDateTime expiration;
    private Socket socket;

    /**
     * Create a socket for the given host and port and connect.
     *
     * @param host             Host
     * @param port             Port
     * @param connectTimeoutMs Timeout waiting for socket to connect
     * @param expirationMs     Number of milliseconds before this socket expires. If set to zero this socket will only be
     *                         used once, although its connection will stay open until a new socket request comes in and
     *                         recycles it or it is forced closed.
     * @param socketFactory    socket factory
     * @throws UnknownHostException if the IP address of the host could not be determined.
     * @throws IOException          if an I/O error occurs when creating the socket.
     */
    PooledSocket(String host, int port, int connectTimeoutMs, int expirationMs, SocketFactory socketFactory)
            throws UnknownHostException, IOException {
        socket = (socketFactory instanceof SSLSocketFactory)
                ? (SSLSocket) socketFactory.createSocket()
                : socketFactory.createSocket();
        socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        expiration = LocalDateTime.now().plus(expirationMs, ChronoUnit.MILLIS);
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    private Boolean isConnected() {
        return socket.isConnected();
    }

    public Boolean isClosed() {
        return socket.isClosed();
    }

    private Boolean isBound() {
        return socket.isBound();
    }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }

    synchronized void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
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
     * Closes the socket
     * @throws IOException if an I/O error occurs
     */
    @Override
    public synchronized void close() throws IOException {
        socket.close();
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