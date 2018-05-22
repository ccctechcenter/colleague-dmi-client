package org.ccctc.colleaguedmiclient.service

import org.ccctc.colleaguedmiclient.exception.DmiMetadataException
import spock.lang.Specification

import static org.ccctc.colleaguedmiclient.util.StringUtils.VM

class CTXMetadataServiceSpec extends Specification {

    def "extra coverage"() {
        // nulls
        when: new CTXMetadataService(null)
        then: thrown NullPointerException
    }

    def "get - no variables / cached / cache refresh"() {
        setup:
        def c = Mock(DmiCTXService)
        def d = new CTXMetadataService(c)

        // initial get
        when:
        def m = d.get("appl", "TRANSACTION.NAME")

        then:
        1 * c.executeRaw(*_) >> ["TV.PRCS.ALIAS.NAME": "Alias"]
        0 * _
        m.variables == []
        m.elements == []
        m.associations == []

        // second get - from cache
        when:
        d.get("appl", "TRANSACTION.NAME")

        then:
        0 * _

        // refresh cache, set value to expire immediately
        when:
        d.setCacheExpirationSeconds(0)
        d.get("appl", "TRANSACTION.NAME", true)

        then:
        1 * c.executeRaw(*_) >> ["TV.PRCS.ALIAS.NAME": "Alias"]
        0 * _
        d.getCacheExpirationSeconds() == 0

        // cache value expired
        when:
        d.get("appl", "TRANSACTION.NAME")

        then:
        1 * c.executeRaw(*_) >> ["TV.PRCS.ALIAS.NAME": "Alias"]
        0 * _

        // clear cache works
        when:
        d.setCacheExpirationSeconds(100)
        d.get("appl", "TRANSACTION.NAME")
        d.clearCache()
        d.get("appl", "TRANSACTION.NAME")

        then:
        2 * c.executeRaw(*_) >> ["TV.PRCS.ALIAS.NAME": "Alias"]
        0 * _
    }

    def "get - exception"() {
        setup:
        def c = Mock(DmiCTXService)
        def d = new CTXMetadataService(c)

        // no response from DMI
        when:
        d.get("appl", "TRANSACTION.NAME")

        then:
        1 * c.executeRaw(*_) >> null
        thrown DmiMetadataException
    }

    def "get"() {
        setup:
        def c = Mock(DmiCTXService)
        def d = new CTXMetadataService(c)

        when:
        def m = d.get("appl", "TRANSACTION.NAME")

        then:
        1 * c.executeRaw(*_) >> [
                "TV.ELEMENT.NAME": "E1" + VM + "E2",
                "TV.ELEMENT.REQUIRED" : "Y" + VM + "N",
                "TV.ELEMENT.CONV" : "" + VM + "D4/",
                "TV.ELEMENT.SIZE" : "1" + VM + "2",
                "TV.ELEMENT.IS.BOOL" : "N" + VM + "N",
                "TV.ASSOC.NAME" : "A1" + VM + "A2",
                "TV.ASSOC.MEMBERS" : "V1,V2" + VM + "V3,V4",
                "TV.VAR.NAME": "D1" + VM  + "V1" + VM + "V2" + VM + "V3" + VM + "V4"]
        0 * _
        m.elements[0].elementName == "E1"
        m.elements[1].elementName == "E2"
        m.elements[0].elementRequired == "Y"
        m.elements[1].elementRequired == "N"
        m.elements[0].elementConv == null
        m.elements[1].elementConv == "D4/"
        m.elements[0].elementSize == "1"
        m.elements[1].elementSize == "2"
        m.elements[0].elementIsBool == "N"
        m.elements[1].elementIsBool == "N"
        m.associations[0].assocName == "A1"
        m.associations[0].assocMembers == ["V1", "V2"]
        m.associations[1].assocName == "A2"
        m.associations[1].assocMembers == ["V3", "V4"]
        m.variables[0].varName == "D1"
        m.variables[1].varName == "V1"
        m.variables[2].varName == "V2"
        m.variables[3].varName == "V3"
        m.variables[4].varName == "V4"
    }
}