package org.ccctc.colleaguedmiclient.service

import org.ccctc.colleaguedmiclient.model.KeyValuePair
import org.ccctc.colleaguedmiclient.model.SessionCredentials
import spock.lang.Specification

import java.time.LocalDateTime

class DmiCTXServiceSpec extends Specification{

    def "execute"() {
        setup:
        def d = Mock(DmiService)
        def c = new DmiCTXService(d)

        when:
        def e1 = c.execute("CORE", "TRANSACTION.NAME",
                [new KeyValuePair<String, String>("FIRST", "VALUE1"),
                 new KeyValuePair<String, String>("SECOND", "VALUE1")
                ])
        def e2 = c.execute("CORE", "TRANSACTION.NAME", null)
        def e3 = c.execute("CORE", "TRANSACTION.NAME", [])

        then:
        3 * d.getSessionCredentials() >> new SessionCredentials("token", "controlId", LocalDateTime.now().plusMinutes(60))


    }
}
