package org.ccctc.colleaguedmiclient.transaction.data

import spock.lang.Specification

class DataRequestSpec extends Specification {

    def "npe"() {
        when:
        new DataRequestTest(null, null, null)

        then:
        thrown NullPointerException
    }

    def "nulls in sub request"() {
        when:
        def a = new DataRequestTest("account", null, null)
        a.addSubRequest(null, null, null, null, null)

        then:
        a.subTransactions[0].commands[2] == null // data access type
        a.subTransactions[0].commands[5] == null // view name
        a.subTransactions[0].commands[11] == null // view end

    }

    class DataRequestTest extends DataRequest {
        DataRequestTest(String account, String token, String controlId) {
            super(account, token, controlId)
        }

        @Override
        void addSubRequest(DataAccessType dataAccessType, String viewName, ViewType viewType, String columns, String criteria) {
            super.addSubRequest(dataAccessType, viewName, viewType, columns, criteria)
        }
    }
}
