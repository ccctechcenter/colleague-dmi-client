package org.ccctc.colleaguedmiclient.service

import groovyx.gpars.GParsPool
import org.ccctc.colleaguedmiclient.exception.DmiServiceException
import org.ccctc.colleaguedmiclient.socket.PooledSocket
import org.ccctc.colleaguedmiclient.socket.PoolingSocketFactory
import spock.lang.Specification

import java.time.LocalDateTime

class DmiServiceSpec extends Specification{

    PoolingSocketFactory socketFactory
    DmiService dmiService

    // good / bad responses
    static token = "123456789012345"
    static controlId = "1234567890"
    static String goodResponse
    static String badResponse

    def setupSpec() {
        // good response based on actual DMI response with credentials modified
        goodResponse = 'DMIþ1.4þLGRSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj0þ18394þ1628þHOSTþLGRQþþDMI_PROCESS_LGRQþ18394þ1628þSLGRSþ12þ0þ1þþþþþþþþSLGRS.ENDþSSTATEþ14þ0þ0þþþþþþþþþþSSTATE.END'
        goodResponse = "#" + (goodResponse.size() + 5) + "#" + goodResponse + "#END#"

        // bad response with username/password error. based on actual DMI response.
        badResponse = 'DMIþ1.4þLGRSþdev0_rtþþþþ174846166ýj0þ18394þ1557þþþþþþþSLGRSþ12þ0þ0þþþþþUser errorþ10001þYou entered an invalid username or password. Please try again.þSLGRS.ENDþSERRSþ7þ0þUser errorþ10001þYou entered an invalid username or password. Please try again.þSERRS.END'
        badResponse = "#" + (badResponse.size() + 5) + "#" + badResponse + "#END#"
    }

    def setup() {
        socketFactory = Mock(PoolingSocketFactory)
        dmiService = new DmiService("account", "username", "password", "secret", socketFactory)
    }


    def "getters coverage"() {
        // coverage for getters
        assert dmiService.getAccount() == "account"
        assert dmiService.getUsername() == "username"
        assert dmiService.getAuthorizationExpirationSeconds() > 0
        assert dmiService.getMaxDmiTransactionRetry() > 0
        assert dmiService.getSocketFactory() == socketFactory
    }

    def "login / getSessionCredentials"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))

        // connection is not active
        when:
        def isActive = dmiService.isActive()

        then:
        0 * _
        isActive == false

        // get credentials triggers a login attempt. token and controlId are extracted from response.
        when:
        def creds = dmiService.getSessionCredentials()

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is
        1 * os.write(*_)
        1 * socket.close()
        1 * os.close()
        1 * os.flush()
        0 * _

        creds.token == token
        creds.controlId == controlId
        creds.expirationDateTime > LocalDateTime.now()

        // get credentials used existing values
        when:
        creds = dmiService.getSessionCredentials()

        then:
        0 * _

        creds.token == token
        creds.controlId == controlId
        creds.expirationDateTime > LocalDateTime.now()

        // we are now active
        when:
        isActive = dmiService.isActive()

        then:
        0 * _
        isActive == true

        // non-forced login does nothing
        when:
        dmiService.login(false)

        then:
        0 * _
    }

    def "login concurrency"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))

        // without force, login is performed once
        when:
        GParsPool.withPool {
            def cl = { dmiService.login(false) }
            def r1 = cl.callAsync()
            def r2 = cl.callAsync()

            r2.get()
            r1.get()
        }

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is
        1 * os.write(*_) >> { Thread.sleep(100) }
        1 * socket.close()
        1 * os.close()
        1 * os.flush()
        0 * _

        // with force, login is performed twice
        when:
        GParsPool.withPool {
            def cl = { dmiService.login(true) }
            def r1 = cl.callAsync()
            def r2 = cl.callAsync()

            r2.get()
            r1.get()
        }

        then:
        2 * socketFactory.getSocket(true) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >> { is.reset(); return is }
        2 * os.write(*_)
        2 * socket.close()
        2 * os.close()
        2 * os.flush()
        0 * _
    }

    def "login errors"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is = new ByteArrayInputStream(badResponse.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream("#27#THIS IS A BAD RESPONSE#END#".getBytes("windows-1252"))

        // no mocks so something will fail ...
        when:
        dmiService.login(false)

        then:
        def e = thrown DmiServiceException
        e.getMessage() == "Login request failed"

        // invalid username
        when:
        dmiService.login(false)

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is
        e = thrown DmiServiceException
        e.getMessage().contains("You entered an invalid username")

        // bad response from DMI, not able to determine error message
        when:
        dmiService.login(false)

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is2
        e = thrown DmiServiceException
        e.getMessage() == "Login request failed and no credentials or error message returned"

    }

}
