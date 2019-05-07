package org.ccctc.colleaguedmiclient.socket;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
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

    private final Log log = LogFactory.getLog(PoolingSocketFactory.class);

    /**
     * Host name / IP Address
     */
    @Getter private final String host;

    /**
     * Port
     */
    @Getter private final int port;

    /**
     * Pool size
     */
    @Getter private final int poolSize;

    /**
     * Use a secure connection?
     */
    @Getter private final boolean secure;

    /**
     * Host name override
     */
    @Getter private final String hostnameOverride;

    /**
     * Timeout waiting for an available socket. Default value is 5 minutes.
     */
    @Getter @Setter private int poolTimeoutMs = 5 * 60 * 1000;

    /**
     * Timeout waiting for a socket to connect. Default value is 5 minutes.
     */
    @Getter @Setter private int socketConnectTimeoutMs = 5 * 60 * 1000;

    /**
     * Maximum amount of time a socket will wait for a response after a request has been sent. Default value is 15 minutes.
     */
    @Getter @Setter private int socketReadTimeoutMs = 15 * 60 * 1000;

    /**
     * Socket expiration time in milliseconds. Default is 30 minutes. This is the longest amount of time a socket will
     * be used for DMI transactions before it is destroyed.
     */
    @Getter @Setter private int socketExpirationMs = 30 * 60 * 1000;


    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition isFull = lock.newCondition();

    private final SocketFactory socketFactory;
    private final Queue<PooledSocket> available;
    private final List<PooledSocket> used;


    /**
     * Create a PoolingSocketFactory
     *
     * @param host             Host name or IP address
     * @param port             Host port
     * @param poolSize         Pool size
     * @param secure           Secure (SSL) connection?
     * @param hostnameOverride Host name override ?
     */
    public PoolingSocketFactory(String host, int port, int poolSize, boolean secure, String hostnameOverride) {

        // @TODO - implement secure and implement hostnameOverride or remove it

        this.host = host;
        this.port = port;
        this.poolSize = poolSize;
        this.secure = secure;
        this.hostnameOverride = hostnameOverride;
        socketFactory = (secure)
                    ? (SSLSocketFactory) SSLSocketFactory.getDefault()
                    : SocketFactory.getDefault();

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
        lock.lock();
        try {
            log.trace("New socket requested, used=" + this.getUsed() + ", available=" + this.getAvailable()
                    + ", waiting=" + lock.getWaitQueueLength(isFull));

            while (used.size() >= poolSize) {
                log.trace("Waiting for available socket");
                if (!isFull.await(poolTimeoutMs, TimeUnit.MILLISECONDS)) {
                    throw new SocketException("Timeout exceeded waiting for available socket");
                }
            }

            // loop through available sockets, closing any that are expired
            PooledSocket socket = available.poll();
            while (socket != null) {
                log.trace("Available socket polled: " + socket.toString());

                if (socket.isExpired()) {
                    try {
                        log.trace("Available socket expired, closing: " + socket.toString());
                        socket.close();
                    } catch (IOException e) {
                        log.error("Error closing expired socket", e);
                    }
                } else if (socket.isClosed()) {
                    log.trace("Available socket already closed, skipping: " + socket.toString());
                } else {
                    break;
                }

                socket = available.poll();
            }

            // if we're forcing a new socket, close and discard the one we got from the available pool to make room
            if (forceNewSocket && socket != null) {
                try {
                    log.trace("Closing available socket (force new): " + socket.toString());
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }

                socket = null;
            }

            // create a new socket
            if (socket == null) {
                socket = newSocket();
                log.trace("New socket created: " + socket.toString());
            } else {
                log.trace("Available socket re-used: " + socket.toString());
            }

            used.add(socket);

            if (used.size() < poolSize)
                isFull.signal();

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
     * Close the connection factory, closing all sockets in the pool (including any sockets in use!)
     */
    public synchronized void close() {
        lock.lock();
        try {
            // close all available sockets
            for (PooledSocket p = available.poll(); p != null; p = available.poll()) {
                try {
                    log.trace("Closing available socket: " + p.toString());
                    p.close();
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
            }

            // close all used sockets (a new list must be used to avoid ConcurrentModificationException)
            for (PooledSocket p : new ArrayList<>(used)) {
                try {
                    log.trace("Closing used socket: " + p.toString());
                    p.close();
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
            }

            used.clear();
            isFull.signalAll();
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
        log.trace("Creating new socket");
        PooledSocket s = new PooledSocket(host, port, socketConnectTimeoutMs, socketExpirationMs, socketFactory);
        s.setKeepAlive(true);
        s.setSoTimeout(socketReadTimeoutMs);
        return s;
    }


    /**
     * Release a socket from use. This moves the socket from "used" to "available".
     *
     * @param socket Socket to release
     */
    public void release(PooledSocket socket) {
        lock.lock();
        try {
            log.trace("Releasing socket back to pool: " + socket.toString());
            used.remove(socket);
            available.add(socket);
            isFull.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Recycle a socket that is in use. This wil remove it from the "used" pool, close it and discard it.
     *
     * @param socket Socket to recycle
     */
    public void recycle(PooledSocket socket) {
        lock.lock();
        try {
            log.trace("Removing socket from pool and closing: " + socket.toString());
            used.remove(socket);

            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }

            isFull.signal();
        } finally {
            lock.unlock();
        }
    }
}