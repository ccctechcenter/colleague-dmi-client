package org.ccctc.colleaguedmiclient.transaction

import spock.lang.Specification

class LoginRequestSpec extends Specification {

    def "nulls"() {
        when: new LoginRequest(null, null, null)
        then: thrown NullPointerException
        when: new LoginRequest("account", null, null)
        then: thrown NullPointerException
        when: new LoginRequest("account", "username", null)
        then: thrown NullPointerException

        when: new LoginRequest(null, null, null, null, null, null)
        then: thrown NullPointerException
        when: new LoginRequest("account", null, null, null, null, null)
        then: thrown NullPointerException
        when: new LoginRequest("account", "username", null, null, null, null)
        then: thrown NullPointerException
    }
}
