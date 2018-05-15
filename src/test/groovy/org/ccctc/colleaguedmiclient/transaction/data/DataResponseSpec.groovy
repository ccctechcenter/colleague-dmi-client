package org.ccctc.colleaguedmiclient.transaction.data

import org.ccctc.colleaguedmiclient.exception.DmiTransactionException
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import spock.lang.Specification

class DataResponseSpec extends Specification {

    def "fromDmiTransaction - exception - batch"() {
        setup:
        def commands

        // null dmi transaction
        when:
        DataResponse.fromDmiTransaction(null)

        then:
        thrown NullPointerException

        // no sub transactions
        when:
        DataResponse.fromDmiTransaction(new DmiTransaction())

        then:
        thrown DmiTransactionException

        // sub transactions of wrong type
        when:
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("BAD", 0, [] as String[])]))

        then:
        thrown DmiTransactionException

        // sub transaction not long enough
        when:
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, [] as String[])]))

        then:
        thrown DmiTransactionException

        // sub transaction null table
        when:
        commands = new String[15]
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // sub transaction assertion error - no F
        when:
        commands[5] = "TABLE"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown AssertionError

        // sub transaction assertion error - no STANDARD
        when:
        commands[0] = "F"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown AssertionError

        // sub transaction invalid mode
        when:
        commands[1] = "STANDARD"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // batch null subset size
        when:
        commands[4] = "BATCH"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // batch subset size mismatch
        when:
        commands[6] = "5"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // batch missing end tag
        when:
        commands[6] = commands.length - 4
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // batch missing record count
        when:
        commands[commands.length - 1] = "TABLE.END"
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        thrown DmiTransactionException

        // zero records
        when:
        commands[12] = "0"
        def r = DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, commands)]))

        then:
        r.toString() != null
        r.data.size() == 0
    }

    def "fromDmiTransaction - batch - 2"() {
        setup:
        def commands = new String[29]
        commands[0] = "F"
        commands[1] = "STANDARD"
        commands[2] = "BATCHKEYS"
        commands[3] = "L"
        commands[4] = "BATCH"
        commands[5] = "TABLE"
        commands[6] = null // set later
        commands[12] = null // set later
        commands[13] = null
        commands[14] = "TUPLE"
        commands[15] = "KEY1"
        commands[16] = "2"
        commands[17] = null
        commands[18] = "VALUE1"
        commands[19] = "VALUE2"
        commands[20] = "KEY1.END"
        commands[21] = "TUPLE"
        commands[22] = "KEY2"
        commands[23] = "2"
        commands[24] = null
        commands[25] = "VALUE3"
        commands[26] = "VALUE4"
        commands[27] = "KEY2.END"
        commands[28] = "TABLE.END"

        String[] copy
        Exception e

        // end of transaction before all records read
        when:
        copy = commands.toList() as String[]
        copy[6] = copy.length - 4
        copy[12] = 3
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("end of transaction before all records read")

        // missing tuple
        when:
        copy = commands.toList() as String[]
        copy[6] = copy.length - 4
        copy[12] = 2
        copy[14] = null
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("missing TUPLE")

        // wrong number of fields
        when:
        copy = commands.toList() as String[]
        copy[6] = copy.length - 4
        copy[12] = 2
        copy[23] = 20
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("end of record not found for key")

        // wrong number of fields 2
        when:
        copy = commands.toList() as String[]
        copy[6] = copy.length - 4
        copy[12] = 2
        copy[23] = 1
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("end of record not found for key")

    }

    def "fromDmiTransaction - single"() {
        setup:
        def commands = new String[15]
        commands[0] = "F"
        commands[1] = "STANDARD"
        commands[2] = "SINGKELEY"
        commands[3] = "L"
        commands[4] = "SINGLE"
        commands[5] = "TABLE"
        commands[6] = null // set later
        commands[8] = "KEY"
        commands[11] = "VALUE1"
        commands[12] = "VALUE2"
        commands[13] = "VALUE3"
        commands[14] = "TABLE.END"

        String[] copy
        Exception e

        // subset size missing
        when:
        copy = commands.toList() as String[]
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("subset size is missing")

        // subset size wrong
        when:
        copy = commands.toList() as String[]
        copy[6] = 999
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("subset size does match response size")

        // end not found where expected
        when:
        copy = commands.toList() as String[]
        copy[6] = "3"
        copy[14] = null
        DataResponse.fromDmiTransaction(new DmiTransaction(subTransactions: [new DmiSubTransaction("SDAFS", 0, copy)]))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("TABLE.END not found where expected")
    }
}
