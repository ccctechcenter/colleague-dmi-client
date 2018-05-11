package org.ccctc.colleaguedmiclient.socket;

import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class represents a factory for maintaining a pool of sockets of a specific size to a specific host and port.
 * <p>
 * When a new socket is requested and the pool is full, the operation will wait until a socket becomes available.
 * Fairness is guaranteed and sockets requests are filled on a first come, first served basis.
 * <p>
 * Sockets in the pool are not closed immediately and will be re-used by subsequent requests until the socket expires
 * after a configurable number of seconds.
 *
 * @see PooledSocket
 */
public class PoolingSocketFactory implements Closeable {

    /**
     * Host name / IP Address
     */
    @Getter
    private final String host;

    /**
     * Port
     */
    @Getter
    private final int port;

    /**
     * Pool size
     */
    @Getter
    private final int poolSize;

    /**
     * Timeout waiting for an available socket. Default value is 5 minutes.
     */
    @Getter
    @Setter
    private int poolTimeoutMs = 5 * 60 * 1000;

    /**
     * Timeout waiting for a socket to connect. Default value is 5 minutes.
     */
    @Getter
    @Setter
    private int socketConnectTimeoutMs = 5 * 60 * 1000;

    /**
     * Maximum amount of time a socket will wait for a response after a request has been sent. Default value is 15 minutes.
     */
    @Getter
    @Setter
    private int socketReadTimeoutMs = 15 * 60 * 1000;

    /**
     * Socket expiration time in milliseconds. Default is 30 minutes. This is the longest amount of time a socket will
     * be used for DMI transactions before it is destroyed.
     */
    @Getter
    @Setter
    private int socketExpirationMs = 30 * 60 * 1000;


    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition isFull = lock.newCondition();

    private final Queue<PooledSocket> available;
    private final List<PooledSocket> used;


    /**
     * Create a PoolingSocketFactory
     *
     * @param host                    Host name or IP address
     * @param port                    Host port
     * @param poolSize                Pool size
     */
    public PoolingSocketFactory(String host, int port, int poolSize) {
        this.host = host;
        this.port = port;
        this.poolSize = poolSize;

        available = new ConcurrentLinkedQueue<>();
        used = new ArrayList<>();
    }


    /**
     * Get the number of sockets in use.
     *
     * @return Sockets in use
     */
    public int getUsed() {
        return used.size();
    }


    /**
     * Get the number of sockets in the pool that are available and (hopefully) already connected and ready for use.
     * These are sockets that have previously been used, then left open and made available. Since socket validation
     * does not occur until a socket is requested, the state of these sockets is unknown and it is possible that they
     * may have expired or been closed.
     * <p>
     * Expired and closed sockets are purged from the available list when a socket request is received.
     *
     * @return Sockets available
     */
    public int getAvailable() {
        return available.size();
    }


    /**
     * Get a socket from the pool by either finding an available socket or creating a new one. If the pool is full,
     * the request will wait until a socket becomes available, or the timeout expires.
     *
     * @param forceNewSocket Force creation of a new socket (rather than using an already open socket)
     * @return Socket
     * @throws SocketException if the socket connection fails,if the timeout expires attempting to get an available
     *                         socket from the pool, or if the operation is interrupted waiting for an available socket
     *                         from the pool.
     */
    public PooledSocket getSocket(boolean forceNewSocket) throws SocketException {
        try {
            lock.lock();
            while (used.size() >= poolSize) {
                if (!isFull.await(poolTimeoutMs, TimeUnit.MILLISECONDS)) {
                    throw new SocketException("Timeout exceeded waiting for available connection");
                }
            }

            PooledSocket socket = available.poll();
            while (socket != null) {
                if (!socket.isClosed() && !socket.isExpired())
                    break;
                socket = available.poll();
            }

            // if we're forcing a new socket, recycle the one we got from the available pool
            if (forceNewSocket && socket != null) {
                try { socket.recycle(); } catch (IOException ignored) { }
                socket = null;
            }

            if (socket == null)
                socket = newSocket();

            used.add(socket);

            return socket;
        } catch (InterruptedException e) {
            throw new SocketException("Attempt to acquire available connection interrupted - " + e.getClass().getName() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new SocketException("Unable to connect to socket: " + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }


    /**
     * Close the connection factory, closing all sockets in the pool.
     */
    public synchronized void close() {
        lock.lock();
        try {
            // close all available sockets
            for (PooledSocket p = available.poll(); p != null; p = available.poll()) {
                try {
                    if (!p.isClosed()) {
                        p.recycle();
                    }
                } catch (IOException ignored) {
                }
            }

            // close all "used" connections
            // (a new list must be used to avoid ConcurrentModificationException)
            for (PooledSocket p : new ArrayList<>(used)) {
                try {
                    if (!p.isClosed()) p.recycle();
                } catch (IOException ignored) {
                }
            }

            used.clear();
        } finally {
            lock.unlock();
        }
    }


    /**
     * Create a new pooled socket
     *
     * @return Socket
     * @throws IOException if there is an error
     */
    private PooledSocket newSocket() throws IOException {
        PooledSocket s = new PooledSocket(host, port, this, this.socketConnectTimeoutMs, this.socketExpirationMs);
        s.setKeepAlive(true);
        s.setSoTimeout(socketReadTimeoutMs);
        return s;
    }


    /**
     * Release a socket from use. This moves the socket from "used" to "available", unless the socket has been closed
     * in which case it is discarded.
     *
     * @param socket Socket to release
     */
    void release(PooledSocket socket) {
        lock.lock();
        try {
            if (used.remove(socket)) {
                if (!socket.isClosed())
                    available.add(socket);
            }
            isFull.signal();
        } finally {
            lock.unlock();
        }
    }
}