package org.ccctc.colleaguedmiclient.transaction

import org.ccctc.colleaguedmiclient.exception.DmiTransactionException
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction
import org.ccctc.colleaguedmiclient.util.StringUtils
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime

class DmiTransactionSpec extends Specification {

    def "to string"() {
    }

    def "nulls"() {
        when: new DmiTransaction(null, null, null, null, null)
        then: thrown NullPointerException
        when: new DmiTransaction("account", null, null, null, null)
        then: thrown NullPointerException
        when: new DmiTransaction("account", "type", null, null, null)
        then: thrown NullPointerException
    }

    def "setter / toString"() {
        setup:
        def t = new DmiTransaction()

        when:
        t.setDmi("DMI")
        t.setVersion("1.4")
        t.setTransactionType("TYPE")
        t.setAccount(["account"] as String[])
        t.setApplication("application")
        t.setToken(["token"] as String[])
        t.setListenerId("listener")
        t.setControlId(["controlId"] as String[])
        t.setCreatedDate(LocalDate.now())
        t.setCreatedTime(LocalTime.now())
        t.setCreatedBy("user")
        t.setInResponseTo(null)
        t.setDebugLevel("0")
        t.setLastProcessedBy("me")
        t.setLastProcessedDate(null)
        t.setLastProcessedTime(null)

        then:
        t.toString() != null
    }

    def "setCredentials"() {
        setup:
        def t = new DmiTransaction()

        // nulls
        when: t.setCredentials(null, null, null)
        then: thrown NullPointerException
        when: t.setCredentials("token", null, null)
        then: thrown NullPointerException

        // set token for first time
        when:
        t.setCredentials("token", "controlId", null)

        then:
        t.getToken() == ["token"]
        t.getControlId() == ["controlId"]

        // set token on top of empty string arrays
        when:
        t.setToken(new String[0])
        t.setControlId(new String[0])
        t.setCredentials("token2", "controlId2", null)

        then:
        t.getToken() == ["token2"]
        t.getControlId() == ["controlId2"]

        // replace token, add hash
        when:
        t.setCredentials("token3", "controlId3", "secret")

        then:
        t.getToken() == ["token3"]
        t.getControlId() == ["controlId3"]
        t.getSubTransactions().size() == 1
    }

    def "add sub after hash"() {
        setup:
        def t = new DmiTransaction("account", "type", "appl", null, null)
        t.addHashSubRequest("secret")

        when:
        t.addSubTransaction(new DmiSubTransaction("type", 0, null))

        then:
        thrown DmiTransactionException
        t.subTransactions.size() == 1

        // second sub add replaces first
        when:
        t.addHashSubRequest("secret2")

        then:
        t.subTransactions.size() == 1
    }


    def "readFromStream - bad headers"() {
        setup:
        def emptyHeader = "##DMI ... #END#"
        def nonNumericHeader = "#123alpha123# ... #END#"
        def eof = "#1"
        def headerTooLong = "#10##END#"
        def noEnd = "#5#acdef"

        Exception e

        when:
        DmiTransaction.fromResponse(new DataInputStream(new ByteArrayInputStream(emptyHeader.getBytes("windows-1252"))))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("Empty header")

        when:
        DmiTransaction.fromResponse(new DataInputStream(new ByteArrayInputStream(nonNumericHeader.getBytes("windows-1252"))))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("non-numeric")

        when:
        DmiTransaction.fromResponse(new DataInputStream(new ByteArrayInputStream(eof.getBytes("windows-1252"))))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("EOFException")

        when:
        DmiTransaction.fromResponse(new DataInputStream(new ByteArrayInputStream(headerTooLong.getBytes("windows-1252"))))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("Encountered EOF")

        when:
        DmiTransaction.fromResponse(new DataInputStream(new ByteArrayInputStream(noEnd.getBytes("windows-1252"))))

        then:
        e = thrown DmiTransactionException
        e.getMessage().contains("end not found")
    }

    def "readFromStream - response exactly the size of one chunk"() {
        setup:
        def body = ("a" * 998) + (StringUtils.FM.toString() * 15)
        def header = "#" + (body.size() + 5) + "#"
        def footer = "#END#"

        assert (header + body + footer).size() == 1024

        def str = new DataInputStream(new ByteArrayInputStream((header + body + footer).getBytes("windows-1252")))

        when:
        def t = DmiTransaction.fromResponse(str)

        then:
        t.toDmiString() == body


    }

    def "readFromStream - long response is pieced together properly"() {
        // for this test, we'll piece together a large and random response
        // with 25 sub transactions of variable size. we will then verify
        // the results by serializing back to a string and comparing to
        // our source

        setup:
        def r = new Random(31242)
        def b = new StringBuilder()

        // nulls in body
        (1 .. 15).each { b.append(StringUtils.FM) }

        // 20 random sub transaction blocks
        for (int x = 0; x < 20; x++) {
            int blockSize = r.nextInt(20).abs() + 10

            b.append(StringUtils.FM)
            b.append("SUB" + x); b.append(StringUtils.FM)
            b.append(blockSize + 4); b.append(StringUtils.FM)
            b.append(0); b.append(StringUtils.FM)
            (1 .. blockSize).each { b.append(r.nextLong().toString()); b.append(StringUtils.FM) }
            b.append("SUB" + x + ".END")
        }

        // 1 block with a very long field (longer than the block size - the word "LONG" repeat 1000 times
        b.append(StringUtils.FM)
        b.append("SUBLONG"); b.append(StringUtils.FM)
        b.append(5); b.append(StringUtils.FM)
        b.append(0); b.append(StringUtils.FM)
        b.append("LONG" * 1000); b.append(StringUtils.FM)
        b.append("SUBLONG.END")

        def body = b.toString()
        def header = "#" + (body.size() + 5) + "#"
        def footer = "#END#"

        def str = new DataInputStream(new ByteArrayInputStream((header + body + footer).getBytes("windows-1252")))

        when:
        def t = DmiTransaction.fromResponse(str)

        then:
        t.toDmiString() == body
        t.subTransactions.size() == 21

        // add a claims sub just for fun (and coverage)
        when:
        t.addClaimsSubRequest()

        then:
        t.subTransactions.size() == 22
        t.subTransactions[21].transactionType == "SCLMQ"
    }
}
