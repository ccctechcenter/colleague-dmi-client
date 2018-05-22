package org.ccctc.colleaguedmiclient.transaction.data

import spock.lang.Specification

class SingleKeyRequestSpec extends Specification {

    def "nulls"() {
        when: new SingleKeyRequest(null, null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new SingleKeyRequest("account", null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new SingleKeyRequest("account", "token", null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new SingleKeyRequest("account", "token", "controlId", null, null, null, null, null)
        then: thrown NullPointerException
        when: new SingleKeyRequest("account", "token", "controlId", "secret", null, null, null, null)
        then: thrown NullPointerException
        when: new SingleKeyRequest("account", "token", "controlId", "secret", "TABLE", null, null, null)
        then: thrown NullPointerException
    }

    def "create"() {
        when:
        def a = new SingleKeyRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, ["FIELD1"], "KEY")
        def b = new SingleKeyRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, null, "KEY")

        then:
        a.subTransactions[0].commands[2] == "SINGLEKEY"
        b.subTransactions[0].commands[2] == "SINGLEKEY"
    }
}
