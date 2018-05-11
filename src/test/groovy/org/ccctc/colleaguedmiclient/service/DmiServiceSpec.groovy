package org.ccctc.colleaguedmiclient.service

import groovyx.gpars.GParsPool
import org.ccctc.colleaguedmiclient.exception.DmiServiceException
import org.ccctc.colleaguedmiclient.socket.PooledSocket
import org.ccctc.colleaguedmiclient.socket.PoolingSocketFactory
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import org.ccctc.colleaguedmiclient.transaction.data.SingleKeyRequest
import org.ccctc.colleaguedmiclient.transaction.data.ViewType
import spock.lang.Specification

import java.time.LocalDateTime

class DmiServiceSpec extends Specification{

    PoolingSocketFactory socketFactory
    DmiService dmiService

    // good / bad responses
    static token = "123456789012345"
    static controlId = "1234567890"
    static token2 = "987654321012345"
    static controlId2 = "0987654321"

    // login DMI responses
    static String goodResponse
    static String goodResponse2
    static String badResponse

    // data DMI responses
    static String dataResponseGood
    static String dataResponseFatal
    static String dataResponseSecurity
    static String dataResponseNotFatal

    // session state response
    static String sessionStateGood

    def setupSpec() {
        // good response based on actual DMI response with credentials modified
        goodResponse = 'DMIþ1.4þLGRSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj0þ18394þ1628þHOSTþLGRQþþDMI_PROCESS_LGRQþ18394þ1628þSLGRSþ12þ0þ1þþþþþþþþSLGRS.ENDþSSTATEþ14þ0þ0þþþþþþþþþþSSTATE.END'
        goodResponse = "#" + (goodResponse.size() + 5) + "#" + goodResponse + "#END#"

        // good response based on actual DMI response with credentials modified
        goodResponse2 = 'DMIþ1.4þLGRSþdev0_rtþUTþ' + token2 + 'þþ' + controlId2 + 'ýj0þ18394þ1628þHOSTþLGRQþþDMI_PROCESS_LGRQþ18394þ1628þSLGRSþ12þ0þ1þþþþþþþþSLGRS.ENDþSSTATEþ14þ0þ0þþþþþþþþþþSSTATE.END'
        goodResponse2 = "#" + (goodResponse2.size() + 5) + "#" + goodResponse2 + "#END#"

        // bad response with username/password error. based on actual DMI response.
        badResponse = 'DMIþ1.4þLGRSþdev0_rtþþþþ174846166ýj0þ18394þ1557þþþþþþþSLGRSþ12þ0þ0þþþþþUser errorþ10001þYou entered an invalid username or password. Please try again.þSLGRS.ENDþSERRSþ7þ0þUser errorþ10001þYou entered an invalid username or password. Please try again.þSERRS.END'
        badResponse = "#" + (badResponse.size() + 5) + "#" + badResponse + "#END#"

        // good data response
        dataResponseGood = 'DMIþ1.4þDAFSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj1þ18394þ44158þHOSTþDAFQþþþ18394þ44158þSDAFSþ19þ0þFþSTANDARDþSINGLEKEYþLþSINGLEþPERSONþ3þþ1234321þþþMalmsteenþþYngwieþPERSON.ENDþSDAFS.END'
        dataResponseGood = "#" + (dataResponseGood.size() + 5) + "#" + dataResponseGood + "#END#"

        // fatal data response - SET error indicates something wrong in the query
        dataResponseFatal = 'DMIþ1.4þDAFSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj1þ18394þ45636þHOSTþDAFQþþþ18394þ45636þSERRSþ7þþSETþ00019þInvalid field.   Field="BAD.FIELD". File ="PERSON"þSERRS.END'
        dataResponseFatal = "#" + (dataResponseFatal.size() + 5) + "#" + dataResponseFatal + "#END#"

        // not fatal data response - made up error - will force a retry
        dataResponseNotFatal = 'DMIþ1.4þDAFSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj1þ18394þ45636þHOSTþDAFQþþþ18394þ45636þSERRSþ7þþCODEþ00123þSome sort of not fatal errorþSERRS.END'
        dataResponseNotFatal = "#" + (dataResponseNotFatal.size() + 5) + "#" + dataResponseNotFatal + "#END#"

        // security error - previous session is no longer valid
        dataResponseSecurity = 'DMIþ1.4þDAFSþdev0_rtþCOREþ' + token + 'þþ' + controlId + 'ýj0þ18394þ46802þHOSTþDAFQþþERRORSþ18394þ46802þSERRSþ7þ0þSECURITYþ00002þYour previous session is no longer valid.  Please log in again.þSERRS.END'
        dataResponseSecurity = "#" + (dataResponseSecurity.size() + 5) + "#" + dataResponseSecurity + "#END#"

        // good response from a session state request (from keepAlive)
        sessionStateGood = 'DMIþ1.4þGSTRSþdev0_rtþUTþ' + token + 'þþ' + controlId + 'ýj0þ18394þ49438þHOSTþGSTRQþþDMI_PROCESS_GSTRQþ18394þ49438þSSTATEþ14þ0þ0þþþþ0þþþþþþSSTATE.END'
        sessionStateGood = "#" + (sessionStateGood.size() + 5) + "#" + sessionStateGood + "#END#"
    }

    def setup() {
        socketFactory = Mock(PoolingSocketFactory)
        dmiService = new DmiService("account", "username", "password", "secret", socketFactory)
    }

    def "getters setters coverage"() {
        when:
        dmiService.setMaxDmiTransactionRetry(1)
        dmiService.setAuthorizationExpirationSeconds(100)

        then:
        dmiService.getAccount() == "account"
        dmiService.getUsername() == "username"
        dmiService.getSocketFactory() == socketFactory
        dmiService.getMaxDmiTransactionRetry() == 1
        dmiService.getAuthorizationExpirationSeconds() == 100
        // for extra coverage .. close not really necessary as socket pool is a mock and not actual sockets
        dmiService.close()
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

    def "login - expired credentials"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is1 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))

        dmiService.setAuthorizationExpirationSeconds(1)

        // get credentials triggers a login attempt. token and controlId are extracted from response.
        when:
        def creds1 = dmiService.login(false)
        Thread.sleep(1100) // sleep for over 1 seconds to let credentials expire
        def creds2 = dmiService.login(false)

        then:
        2 * socketFactory.getSocket(true) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >>> [is1, is2]
        2 * os.write(*_)
        2 * socket.close()
        2 * os.close()
        2 * os.flush()
        0 * _
        creds1 != creds2
    }

    def "login concurrency"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def creds1, creds2

        // note: for testing, we can't use the same stream for concurrency, otherwise two threads try to read from the
        // same stream, which will fail
        def is1 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))
        def is3 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))


        // two simultaneous calls but login is only performed once and both get the same credentials
        when:
        GParsPool.withPool {
            def r1 = { dmiService.login(false) }.callAsync()
            def r2 = { dmiService.login(false) }.callAsync()

            creds2 = r2.get()
            creds1 = r1.get()
        }

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is1
        1 * os.write(*_)
        1 * socket.close()
        1 * os.close()
        1 * os.flush()
        0 * _
        assert creds1 == creds2

        // with force, login is performed twice
        when:
        GParsPool.withPool {
            def r1 = { dmiService.login(true) }.callAsync()
            def r2 = { dmiService.login(true) }.callAsync()

            creds2 = r2.get()
            creds1 = r1.get()
        }

        then:
        2 * socketFactory.getSocket(true) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >>> [is2, is3]
        2 * os.write(*_)
        2 * socket.close()
        2 * os.close()
        2 * os.flush()
        0 * _
        assert creds1 != creds2
    }

    def "login errors"() {
        setup:
        def ex = new Exception("test exception")
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is = new ByteArrayInputStream(badResponse.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream("#27#THIS IS A BAD RESPONSE#END#".getBytes("windows-1252"))

        // exception in doSend
        when:
        dmiService.login(false)

        then:
        1 * socketFactory.getSocket(true) >> { throw ex }
        def e = thrown DmiServiceException
        e.getMessage() == "Login request failed"
        e.getCause().getCause() == ex

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

    def "send -- success"() {
        setup:
        def transaction = new SingleKeyRequest("account", token, controlId, "secret", "PERSON", ViewType.PHYS, ["FIRST.NAME", "LAST.NAME"], "1234321")
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is = new ByteArrayInputStream(dataResponseGood.getBytes("windows-1252"))

        when:
        def result = dmiService.send(transaction)

        then:
        1 * socketFactory.getSocket(false) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is
        1 * os.write(*_)
        1 * os.close()
        1 * os.flush()
        1 * socket.close()
        0 * _
        result != null
    }

    def "send -- exception"() {
        setup:
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        dmiService.setMaxDmiTransactionRetry(1)

        // exception getting a socket
        when:
        dmiService.send(Mock(DmiTransaction))

        then:
        1 * socketFactory.getSocket(false) >> { throw new Exception() }
        1 * socketFactory.getSocket(true) >> { throw new Exception() }
        0 * _
        thrown DmiServiceException

        // exception getting output stream
        when:
        dmiService.send(Mock(DmiTransaction))

        then:
        2 * socketFactory.getSocket(_) >> socket
        2 * socket.getOutputStream() >> { throw new Exception() }
        2 * socket.recycle()
        0 * _
        thrown DmiServiceException

        // exception getting input stream
        when:
        dmiService.send(Mock(DmiTransaction))

        then:
        2 * socketFactory.getSocket(_) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >>  { throw new Exception() }
        2 * socket.recycle()
        2 * os.close()
        2 * os.flush()
        0 * _
        thrown DmiServiceException

    }

    def "send -- SERRS - log back in"() {
        setup:
        dmiService.setMaxDmiTransactionRetry(1)

        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)

        def securityResponse = new ByteArrayInputStream(dataResponseSecurity.getBytes("windows-1252"))
        def goodLoginResponse = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))
        def goodDataResponse = new ByteArrayInputStream(dataResponseGood.getBytes("windows-1252"))

        def transaction1 = new DmiTransaction("account", "DAFS", "appl", "badtoken", "badcontrolid")
        def transaction2 = new DmiTransaction("account", "DAFS", "appl", null, null)

        // log back in required - credentials are updated in original request
        when:
        dmiService.send(transaction1)

        then:
        1 * socketFactory.getSocket(false) >> socket
        2 * socketFactory.getSocket(true) >> socket
        3 * socket.getOutputStream() >> os
        3 * socket.getInputStream() >>> [securityResponse, goodLoginResponse, goodDataResponse]
        3 * os.write(*_)
        3 * socket.close()
        3 * os.close()
        3 * os.flush()
        0 * _
        transaction1.token[0] == token
        transaction1.controlId[0] == controlId

        // log back in required - no credentials update in original request
        when:
        securityResponse.reset()
        goodLoginResponse.reset()
        goodDataResponse.reset()
        dmiService.send(transaction2)

        then:
        1 * socketFactory.getSocket(false) >> socket
        2 * socketFactory.getSocket(true) >> socket
        3 * socket.getOutputStream() >> os
        3 * socket.getInputStream() >>> [securityResponse, goodLoginResponse, goodDataResponse]
        3 * os.write(*_)
        3 * socket.close()
        3 * os.close()
        3 * os.flush()
        0 * _
        transaction2.token == null
    }

    def "send -- SERRS - fatal"() {
        setup:
        dmiService.setMaxDmiTransactionRetry(1)

        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)

        def is = new ByteArrayInputStream(dataResponseFatal.getBytes("windows-1252"))

        def transaction = new DmiTransaction("account", "DAFS", "appl", token, controlId)

        when:
        dmiService.send(transaction)

        then:
        1 * socketFactory.getSocket(false) >> socket
        1 * socket.getOutputStream() >> os
        1 * socket.getInputStream() >> is
        1 * os.write(*_)
        1 * socket.close()
        1 * os.close()
        1 * os.flush()
        0 * _
        thrown DmiServiceException
    }

    def "send -- SERRS - retry"() {
        setup:
        dmiService.setMaxDmiTransactionRetry(1)

        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)

        def is1 = new ByteArrayInputStream(dataResponseNotFatal.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream(dataResponseNotFatal.getBytes("windows-1252"))

        def transaction = new DmiTransaction("account", "DAFS", "appl", token, controlId)

        when:
        dmiService.send(transaction)

        then:
        1 * socketFactory.getSocket(false) >> socket
        1 * socketFactory.getSocket(true) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >>> [is1, is2]
        2 * os.write(*_)
        2 * socket.close()
        2 * os.close()
        2 * os.flush()
        0 * _
        thrown DmiServiceException
    }

    def "keepAlive"() {
        def socket = Mock(PooledSocket)
        def os = Mock(OutputStream)
        def is1 = new ByteArrayInputStream(goodResponse.getBytes("windows-1252"))
        def is2 = new ByteArrayInputStream(sessionStateGood.getBytes("windows-1252"))

        // this will perform a login, then a session state request
        when:
        dmiService.keepAlive()

        then:
        1 * socketFactory.getSocket(true) >> socket
        1 * socketFactory.getSocket(false) >> socket
        2 * socket.getOutputStream() >> os
        2 * socket.getInputStream() >>> [is1, is2]
        2 * os.write(*_)
        2 * socket.close()
        2 * os.close()
        2 * os.flush()
        0 * _
    }
}
