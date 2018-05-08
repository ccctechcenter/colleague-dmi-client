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

    def "constructor and getters"() {
        when:
        def f = new PoolingSocketFactory("localhost", testPort, 1)

        then:
        f.getHost() == "localhost"
        f.getPort() == testPort
        f.getPoolSize() == 1
        f.getPoolTimeoutMs() != null
        f.getSocketConnectTimeoutMs() != null
        f.getSocketExpirationMs() != null
        f.getSocketReadTimeoutMs() != null
    }

    def "pool timeout exceeded"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1)
        f.setPoolTimeoutMs(5)

        when:
        f.getSocket()
        f.getSocket()

        then:
        def i = thrown SocketException
        i.getMessage().contains("Timeout")

        cleanup:
        f.close()
    }

    def "socket timeout exceeded"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort2, 1)
        f.setSocketConnectTimeoutMs(5)

        when:
        f.getSocket()

        then:
        def i = thrown SocketException
        i.getMessage().contains("Timeout")

        cleanup:
        f.close()
    }

    def "interrupted"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1)

        when:
        f.getSocket()

        Exception ex

        Thread th = new Thread({
            try { f.getSocket() }
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
        def f = new PoolingSocketFactory("localhost", testPort, 1)

        when:
        def s = f.getSocket()
        s.close()
        def s2 = f.getSocket()

        then:
        s == s2

        cleanup:
        f.close()
    }

    def "closed socket not re-used"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 1)

        when:
        def s = f.getSocket()
        s.recycle()
        def s2 = f.getSocket()

        then:
        s != s2
        f.getUsed() == 1
        f.getAvailable() == 0

        cleanup:
        f.close()
    }

    def "expired socket not re-used"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 10)
        f.setSocketExpirationMs(100)

        when:
        def s = f.getSocket()
        f.close()
        Thread.sleep(1200)
        def s2 = f.getSocket()

        then:
        s != s2
        f.getUsed() == 1
        f.getAvailable() == 0

        cleanup:
        f.close()
    }

    def "open and close test"() {
        setup:
        def f = new PoolingSocketFactory("localhost", testPort, 10)
        def sockets, sockets2, sockets3

        when:
        // open 5 connections
        GParsPool.withPool {
            sockets = (0..4).collectParallel { return f.getSocket() }
        }

        then:
        f.used == 5
        f.available == 0

        when:
        // open 5 more connections
        GParsPool.withPool {
            sockets2 = (0..4).collectParallel { return f.getSocket() }
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
            sockets3 = (0..4).collectParallel { return f.getSocket() }
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
        def f = new PoolingSocketFactory("localhost", testPort, 10)
        def s1 = f.getSocket()
        def s2 = f.getSocket()

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