package org.ccctc.colleaguedmiclient.transaction.ctx

import org.ccctc.colleaguedmiclient.exception.DmiTransactionException
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import spock.lang.Specification

class CTXResponseSpec extends Specification {

    def "exceptions"() {
        setup:
        def badLen = new DmiTransaction(subTransactions: [
                new DmiSubTransaction("SCTRS", 0, new String[0]),
                new DmiSubTransaction("SCTVAL", 0, new String[0])
        ])

        def badCount = new DmiTransaction(subTransactions: [
                new DmiSubTransaction("SCTRS", 0, new String[0]),
                new DmiSubTransaction("SCTVAL", 0, new String[2])
        ])

        def eot = new DmiTransaction(subTransactions: [
                new DmiSubTransaction("SCTRS", 0, new String[0]),
                new DmiSubTransaction("SCTVAL", 0, [
                        "1", "2", "A.VAR1", "VALUE1"
                ] as String[])
        ])
        Exception e

        // bad length
        when:
        CTXResponse.fromDmiTransaction(badLen)

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("sub transaction not long enough")

        // missing variable count
        when:
        CTXResponse.fromDmiTransaction(badCount)

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("variable count is missing")

        // end of transaction
        when:
        CTXResponse.fromDmiTransaction(eot)

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("end of transaction")

    }
}
