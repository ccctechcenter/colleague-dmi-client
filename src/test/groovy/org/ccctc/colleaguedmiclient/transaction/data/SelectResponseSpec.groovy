package org.ccctc.colleaguedmiclient.transaction.data

import org.ccctc.colleaguedmiclient.exception.DmiTransactionException
import org.ccctc.colleaguedmiclient.transaction.DmiSubTransaction
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import spock.lang.Specification

class SelectResponseSpec extends Specification {

    def "fromDmiTransaction - exception or missing sub transaction"() {
        when:
        SelectResponse.fromDmiTransaction(null)

        then:
        thrown NullPointerException

        when:
        SelectResponse.fromDmiTransaction(new DmiTransaction())

        then:
        thrown DmiTransactionException

        when:
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SUB", 0, null)]))

        then:
        thrown DmiTransactionException
    }

    def "fromDmiTransaction - more exceptions"() {
        setup:
        String [] commands
        Exception e

        // not long enough
        when:
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, [] as String[])]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("sub transaction not long enough")

        // no table
        when:
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, new String[7])]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("no table/view specified")

        // assertion 1
        when:
        commands = new String[10]
        commands[5] = "TABLE"
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown AssertionError

        // assertion 2
        when:
        commands[0] = "F"
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown AssertionError

        // assertion 3
        when:
        commands[1] = "STANDARD"
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown AssertionError

        // missing subset size
        when:
        commands[4] = "SELECT"
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("subset size is missing")

        // subset size wrong
        when:
        commands[6] = 999
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("subset size does match")

        // end block not found
        when:
        commands[6] = commands.length - 4
        SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("TABLE.END not found")
    }

    def "fromDmiTransaction - success"() {
        setup:
        def commands = [
                "F",
                "STANDARD",
                "SELECT",
                "L",
                "SELECT",
                "TABLE",
                "7",
                "KEY1",
                "KEY2",
                "KEY3",
                "TABLE.END"
        ] as String[]

        when:
        def r = SelectResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        r.toString() != null
        r.table == "TABLE"
        r.keys == ["KEY1", "KEY2", "KEY3"]
    }
}
