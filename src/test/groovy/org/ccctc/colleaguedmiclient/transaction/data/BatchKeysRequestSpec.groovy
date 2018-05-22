package org.ccctc.colleaguedmiclient.transaction.data

import spock.lang.Specification

class BatchKeysRequestSpec extends Specification {

    def "nulls"() {
        when: new BatchKeysRequest(null, null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchKeysRequest("account", null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchKeysRequest("account", "token", null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchKeysRequest("account", "token", "controlId", null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchKeysRequest("account", "token", "controlId", "secret", null, null, null, null)
        then: thrown NullPointerException
        when: new BatchKeysRequest("account", "token", "controlId", "secret", "TABLE", null, null, null)
        then: thrown NullPointerException
    }

    def "create"() {
        when:
        def a = new BatchKeysRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, ["FIELD1"], null)
        def b = new BatchKeysRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, null, ["KEY1"])

        then:
        a.subTransactions[0].commands[2] == "BATCHKEYS"
        b.subTransactions[0].commands[2] == "BATCHKEYS"
    }
}
