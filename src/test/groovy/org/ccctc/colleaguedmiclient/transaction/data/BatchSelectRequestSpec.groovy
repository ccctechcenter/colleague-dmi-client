package org.ccctc.colleaguedmiclient.transaction.data

import spock.lang.Specification

class BatchSelectRequestSpec extends Specification {

    def "nulls"() {
        when: new BatchSelectRequest(null, null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchSelectRequest("account", null, null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchSelectRequest("account", "token", null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchSelectRequest("account", "token", "controlId", null, null, null, null, null)
        then: thrown NullPointerException
        when: new BatchSelectRequest("account", "token", "controlId", "secret", null, null, null, null)
        then: thrown NullPointerException
        when: new BatchSelectRequest("account", "token", "controlId", "secret", "TABLE", null, null, null)
        then: thrown NullPointerException
    }

    def "create"() {
        when:
        def a = new BatchSelectRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, ["FIELD1"], "A = 'B'")
        def b = new BatchSelectRequest("account", "token", "controlId", "secret", "TABLE", ViewType.PHYS, null, "A = 'B'")

        then:
        a.subTransactions[0].commands[2] == "BATCHSELECT"
        b.subTransactions[0].commands[2] == "BATCHSELECT"
    }

}
