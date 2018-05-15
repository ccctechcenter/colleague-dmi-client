package org.ccctc.colleaguedmiclient.transaction.data

import spock.lang.Specification

class SelectRequestSpec extends Specification {

    def "nulls"() {
        when: new SelectRequest(null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new SelectRequest("account", null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new SelectRequest("account", "token", null, null, null, null, null)
        then: thrown NullPointerException
        when: new SelectRequest("account", "token", "controlId", null, null, null, null)
        then: thrown NullPointerException
        when: new SelectRequest("account", "token", "controlId", "secret", null, null, null)
        then: thrown NullPointerException
    }

    def "type - SELECT"() {
        when:
        def s = new SelectRequest("account", "token", "controlId", "secret", "TABLE", "A = 'B'", null)
        then:
        s.subTransactions[0].commands[2] == "SELECT"

        when:
        s = new SelectRequest("account", "token", "controlId", "secret", "TABLE", "A = 'B'", [])
        then:
        s.subTransactions[0].commands[2] == "SELECT"

        when:
        s = new SelectRequest("account", "token", "controlId", "secret", "TABLE", "A = 'B'", [""])
        then:
        s.subTransactions[0].commands[2] == "SELECT"
    }

    def "type - SUBSELECT"() {
        when:
        def s = new SelectRequest("account", "token", "controlId", "secret", "TABLE", "A = 'B'", ["KEY1", "KEY2"])
        then:
        s.subTransactions[0].commands[2] == "SUBSELECT"
    }

}
