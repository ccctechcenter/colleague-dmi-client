package org.ccctc.colleaguedmiclient.util

import spock.lang.Specification

import static org.ccctc.colleaguedmiclient.util.ByteUtils.*

class ByteUtilsSpec extends Specification{

    static constructorConverage = new ByteUtils()

    def "byteSplit - remainder"() {
        setup:
        def b = new byte[5]
        b[0] = 1
        b[1] = 2
        b[2] = 1
        b[3] = 2
        b[4] = 3

        when:
        def r = byteSplit(b, b.length, (byte)2)

        then:
        r.split.size() == 2
        r.split[0].size() == 1
        r.split[1].size() == 1
        r.split[0][0] == (byte)1
        r.split[1][0] == (byte)1
        r.remainder.size() == 1
        r.remainder[0] == (byte)3

    }

    def "byteSplit - no remainder"() {
        setup:
        def b = new byte[10]
        b[0] = 1
        b[1] = 2
        b[2] = 1
        b[3] = 2

        when:
        def r = byteSplit(b, 4, (byte)2)

        then:
        r.split.size() == 2
        r.split[0].size() == 1
        r.split[1].size() == 1
        r.split[0][0] == (byte)1
        r.split[1][0] == (byte)1
        r.remainder == null
    }

    def "byteArrayToString - null"() {
        when:
        def nil = byteArrayToString(new byte[0])

        then:
        nil == null
    }

    def "byteArrayToString"() {
        setup:
        def b = new byte[3]
        b[0] = (byte)('a' as char)
        b[1] = (byte)('-' as char)
        b[2] = (byte)('Z' as char)

        when:
        def aToZ = byteArrayToString(b)

        then:
        aToZ == "a-Z"
    }

    def "byteArrayToStringArray - empty"() {
        when:
        def empty = byteArrayToStringArray(new byte[0], (char)1)

        then:
        empty.size() == 0
    }

    def "byteArrayToStringArray"() {
        setup:
        def b = new byte[7]
        b[0] = (byte)('a' as char)
        b[1] = (byte)('-' as char)
        b[2] = (byte)('Z' as char)
        b[3] = (byte)('+' as char)
        b[4] = (byte)('z' as char)
        b[5] = (byte)('-' as char)
        b[6] = (byte)('A' as char)

        when:
        def aToZtoA = byteArrayToStringArray(b, '+' as char)

        then:
        aToZtoA.size() == 2
        aToZtoA[0] == "a-Z"
        aToZtoA[1] == "z-A"
    }


}
