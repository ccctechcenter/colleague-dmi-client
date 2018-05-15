package org.ccctc.colleaguedmiclient.socket

import groovyx.gpars.GParsPool
import spock.lang.Specification

class SocketSpec extends Specification {

    static int testPort = 8488
    static int testPort2 = 8489
    static ServerSocket

    def setupSpec() {
        serverSocket = new ServerSocket(testPort)
    }

    def cleanupSpec() {
        serverSocket.close()
    }

    def "getters / setters"() {
        when:
        def f = new PoolingSocketFactory("localhost", testPort, 1, true, "hostnameoverride")
        f.setSocketReadTimeoutMs(10000)
        f.setSocketExpirationMs(9999)
        f.setPoolTimeoutMs(9998)
        f.setSocketConnectTimeoutMs(9997)

        then:
        f.getHost() == "localhost"
        f.getPort() == testPort
        f.isSecure() == true
        f.getHostnameOverride() == "hostnameoverride"
        f.getPoolSize() == 1
        f.getSocketReadTimeoutMs() == 10000
        f.getSocketExpirationMs() == 9999
        f.getPoolTimeoutMs() == 9998
        f.getSocketConnectTimeoutMs() == 9997
    }

    def "pool timeout exceeded"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1, false, null)
        f.setPoolTimeoutMs(5)

        when:
        f.getSocket(false)
        f.getSocket(false)

        then:
        def i = thrown SocketException
        i.getMessage().contains("Timeout")

        cleanup:
        f.close()
    }

    def "socket timeout exceeded"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort2, 1, false, null)
        f.setSocketConnectTimeoutMs(5)

        when:
        f.getSocket(false)

        then:
        def i = thrown SocketException
        i.getMessage().contains("Timeout")

        cleanup:
        f.close()
    }

    def "interrupted"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1, false, null)

        when:
        f.getSocket(false)

        Exception ex

        Thread th = new Thread({
            try { f.getSocket(false) }
            catch (SocketException e) { ex = e }
            } as Runnable)
        th.start()
        th.interrupt()

        // it might not instantly interrupt, so give it a second
        sleep(1000)

        then:
        ex != null
        ex.getMessage().contains("interrupted")

        cleanup:
        f.close()
    }


    def "socket re-used"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 5, false, null)

        // one socket is closed, then re-used
        when:
        def s = f.getSocket(false)
        s.close()
        def s2 = f.getSocket(false)

        then:
        s == s2

        cleanup:
        f.close()
    }

    def "force new socket, available socket is recycled"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 5, false, null)

        // one socket is closed, then re-used
        when:
        def s = f.getSocket(false)
        s.close()
        def s2 = f.getSocket(true)

        then:
        s.isClosed()
        s != s2
        f.available == 0
        f.used == 1

        cleanup:
        f.close()
    }

    def "one socket expired"() {
        // in this test, we open two sockets - s3, s4 then close s3 after it has expired and s4 when it has not expired.
        // s5 should pick up s4 and s3 should be recycled since it has expired.

        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 5, false, null)

        when:
        f.setSocketExpirationMs(500)
        def s3 = f.getSocket(false)
        sleep(500)
        def s4 = f.getSocket(false)
        s3.close()
        s4.close()
        def s5 = f.getSocket(false)

        then:
        s3.isClosed()
        s4 == s5
        f.getAvailable() == 0
        f.getUsed() == 1

        cleanup:
        f.close()
    }

    def "closed socket not re-used"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1, false, null)

        when:
        def s = f.getSocket(false)
        s.recycle()
        def s2 = f.getSocket(false)

        then:
        s != s2
        f.getUsed() == 1
        f.getAvailable() == 0

        cleanup:
        f.close()
    }

    def "expired socket not re-used"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 10, false, null)
        f.setSocketExpirationMs(100)

        when:
        def s = f.getSocket(false)
        f.close()
        Thread.sleep(1200)
        def s2 = f.getSocket(false)

        then:
        s != s2
        f.getUsed() == 1
        f.getAvailable() == 0

        cleanup:
        f.close()
    }

    def "open and close test"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 10, false, null)
        def sockets, sockets2, sockets3

        when:
        // open 5 connections
        GParsPool.withPool {
            sockets = (0..4).collectParallel { return f.getSocket(false) }
        }

        then:
        f.used == 5
        f.available == 0

        when:
        // open 5 more connections
        GParsPool.withPool {
            sockets2 = (0..4).collectParallel { return f.getSocket(false) }
        }

        then:
        f.used == 10
        f.available == 0

        when:
        // release 5 connections
        GParsPool.withPool {
            sockets.eachParallel { it.close() }
        }

        then:
        f.used == 5
        f.available == 5

        when:
        // open 5 more connections
        GParsPool.withPool {
            sockets3 = (0..4).collectParallel { return f.getSocket(false) }
        }

        then:
        f.used == 10
        f.available == 0
        // check to see that all socket released were from "socket"
        // taken by "socket3" (but perhaps not in the same array order)
        !sockets3.find { !sockets.contains(it) }

        when:
        // release 5 connections
        sockets3.each { it.close() }

        then:
        f.used == 5
        f.available == 5

        cleanup:
        f.close()
    }

    def "pooled socket close without factory"() {
        setup:
        def s = new PooledSocket("localhost", testPort, null, 5000, 3600)

        when:
        s.close()

        then:
        // without a factory the connection is closed instead of released back to factory
        s.isClosed()
    }

    def "close factory - used and available sockets should be emptied"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 10, false, null)
        def s1 = f.getSocket(false)
        def s2 = f.getSocket(false)

        s2.close()

        when:
        f.close()

        then:
        s1.isClosed()
        s2.isClosed()
        f.available == 0
        f.used == 0
    }
}