package org.ccctc.colleaguedmiclient.socket

import groovyx.gpars.GParsPool
import spock.lang.Specification

class SocketSpec extends Specification {

    static String goodResponse
    static token = "123456789012345"
    static controlId = "1234567890"
    String testHost
    int testPort
    ServerSocket serverSocket


    def setupSpec() {
        // good response based on actual DMI response with credentials modified
        goodResponse = 'DMIþ1.4þLGRSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj0þ18394þ1628þHOSTþLGRQþþDMI_PROCESS_LGRQþ18394þ1628þSLGRSþ12þ0þ1þþþþþþþþSLGRS.ENDþSSTATEþ14þ0þ0þþþþþþþþþþSSTATE.END'
        goodResponse = "#" + (goodResponse.size() + 5) + "#" + goodResponse + "#END#"
    }

    def setup() {
        serverSocket = new ServerSocket(0)
        testPort = serverSocket.getLocalPort()
        testHost = serverSocket.getInetAddress().getHostAddress()
    }

    def cleanup() {
        serverSocket.close()
    }

    def "getters / setters"() {
        when:
        def f = new PoolingSocketFactory(testHost, testPort, 1, true, "hostnameoverride")
        f.setSocketReadTimeoutMs(10000)
        f.setSocketExpirationMs(9999)
        f.setPoolTimeoutMs(9998)
        f.setSocketConnectTimeoutMs(9997)

        then:
        f.getHost() == testHost
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
        def f = new PoolingSocketFactory(testHost, testPort, 1, false, null)
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

    def "socket"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 1, false, null)

        when:
        def s = f.getSocket(false)

        then:
        f.poolSize == 1
        s.getOutputStream() instanceof OutputStream
        s.getInputStream() instanceof InputStream

        cleanup:
        f.close()
    }

    def "secure socket"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 1, true, null)

        when:
        def s = f.getSocket(false)

        then:
        f.poolSize == 1
        s.getOutputStream() instanceof OutputStream
        s.getInputStream() instanceof InputStream

        cleanup:
        f.close()
    }

    def "socket connection refused"() {
        setup:
        // create a server socket to find an open port - then close it so the port will no longer accept connections
        // (should trigger a connection refused as that port is not listening)
        def ss = new ServerSocket(0)
        def h = ss.getInetAddress().getHostAddress()
        def p = ss.getLocalPort()
        ss.close()

        def f = new PoolingSocketFactory(h, p, 1, false, null)

        when:
        f.getSocket(false)

        then:
        def i = thrown SocketException
        i.getMessage().contains("refused")

        cleanup:
        f.close()
    }

    def "interrupted"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 1, false, null)

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
        def f = new PoolingSocketFactory(testHost, testPort, 5, false, null)

        // one socket is closed, then re-used
        when:
        def s = f.getSocket(false)
        f.release(s)
        def s2 = f.getSocket(false)

        then:
        s == s2

        cleanup:
        f.close()
    }

    def "force new socket, available socket is recycled"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 5, false, null)

        // one socket is closed, then re-used
        when:
        def s = f.getSocket(false)
        f.release(s)
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
        def s3, s4, s5

        // in this test, we open two sockets - s3, s4 then close s3 after it has expired and s4 when it has not expired.
        // s5 should pick up s4 and s3 should be recycled since it has expired.

        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 5, false, null)

        when:
        f.setSocketExpirationMs(100)
        s3 = f.getSocket(false)
        f.setSocketExpirationMs(100000)
        s4 = f.getSocket(false)

        assert f.used == 2
        assert f.available == 0

        // close s3 and s4 - sleep between closes to ensure s3 is released first
        f.release(s3)
        sleep(100)
        f.release(s4)

        assert f.used == 0
        assert f.available == 2

        while (!s3.isExpired())
            sleep(100)

        // this should recycle s3 (since its expired) and pick up the available socket originally assigned to s4
        s5 = f.getSocket(false)

        then:
        s3.isClosed()
        s4 == s5
        f.available == 0
        f.used == 1

        cleanup:
        f.close()
    }

    def "closed socket not re-used"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 1, false, null)

        when:
        def s = f.getSocket(false)
        f.recycle(s)
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
        def f = new PoolingSocketFactory(testHost, testPort, 10, false, null)
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
        def f = new PoolingSocketFactory(testHost, testPort, 10, false, null)
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
            sockets.eachParallel { f.release(it) }
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
        sockets3.each { f.release(it) }

        then:
        f.used == 5
        f.available == 5

        cleanup:
        f.close()
    }

    def "close factory - used and available sockets should be emptied"() {
        setup:
        def f = new PoolingSocketFactory(testHost, testPort, 10, false, null)
        def s1 = f.getSocket(false)
        def s2 = f.getSocket(false)

        f.release(s2)

        when:
        f.close()

        then:
        s1.isClosed()
        s2.isClosed()
        f.available == 0
        f.used == 0
    }

    def "socket close exceptions"() {
        setup:
        def m1 = Mock(PooledSocket)
        def m2 = Mock(PooledSocket)
        def m3 = Mock(PooledSocket)
        def f = new PoolingSocketFactory(testHost, testPort, 10, false, null)
        f.@available.add(m1)
        f.@available.add(m2)
        f.@available.add(m3)

        when:
        def s1 = f.getSocket(false)
        def s2 = f.getSocket(false)

        assert s1 == m2
        assert s2 == m3

        f.@used.remove(m2)

        f.release(s1)
        f.recycle(s2)

        then:

        // m1 - already expired, closing throws an exception (does not go back to pool)
        1 * m1.isExpired() >> true
        1 * m1.close() >> { throw new IOException("whoopsies") }

        // m2 - available and released back into pool
        1 * m2.isExpired() >> false
        1 * m2.isClosed() >> false

        // m3 - recycled
        1 * m3.isExpired() >> false
        1 * m3.isClosed() >> false
        1 * m3.close() >> { throw new IOException("whoopsies") }

        _ * _.equals(*_)

        0 * _
        f.available == 1
        f.used == 0


    }
}