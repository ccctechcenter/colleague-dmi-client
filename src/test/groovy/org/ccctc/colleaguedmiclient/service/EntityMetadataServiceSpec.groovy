package org.ccctc.colleaguedmiclient.service

import org.ccctc.colleaguedmiclient.exception.DmiMetadataException
import spock.lang.Specification

import static org.ccctc.colleaguedmiclient.util.StringUtils.VM

class EntityMetadataServiceSpec extends Specification {

    def "extra coverage"() {
        // nulls
        when: new EntityMetadataService(null)
        then: thrown NullPointerException
    }

    def "get - exception"() {
        setup:
        def d = Mock(DmiCTXService)
        def m = new EntityMetadataService(d)

        // null CTX response
        when:
        m.get("CORE", "PERSON")

        then:
        1 * d.executeRaw(*_) >> null
        0 * _
        def e = thrown DmiMetadataException
        e.getMessage().contains("unexpected response from DMI")

        // number format error
        when:
        m.get("CORE", "PERSON")

        then:
        1 * d.executeRaw(*_) >> [
                "TV.CDD.NAME" : "LAST.NAME",
                "TV.FIELD.PLACEMENT" : "invalid"]
        e = thrown DmiMetadataException
        e.getMessage().contains("NumberFormatException")
    }

    def "get"() {
        setup:
        def d = Mock(DmiCTXService)
        def m = new EntityMetadataService(d)
        def response = [
                "TV.CDD.NAME": "FIRST.NAME" + VM + "LAST.NAME" + VM + "" + VM + "CC.FIELD",
                "TV.SOURCE": "PERSON" + VM + "PERSON" + VM + "PERSON" + VM + "PERSON",
                "TV.MAXIMUM.STORAGE.SIZE": "30" + VM + "50" + VM + "" + VM + "50",
                "TV.FIELD.PLACEMENT": "1" + VM + "5" + VM + "" + VM + "",
                "TV.ENTITY.TYPE" : "TYPE",
                "TV.GUID.ENABLED" : "N"
        ]

        when:
        def r = m.get("CORE", "PERSON")
        def e  =r.getEntries()
        def o = r.getOrderedEntries()

        then:
        1 * d.executeRaw(*_) >> response
        0 * _
        r.getEntityType() == "TYPE"
        r.getGuidEnabled() == "N"
        e.size() == 3
        o.size() == 5
        o[0].name == "FIRST.NAME"
        o[1] == null
        o[2] == null
        o[3] == null
        o[4].name == "LAST.NAME"
        e["FIRST.NAME"].source == "PERSON"
        e["LAST.NAME"].source == "PERSON"
        e["FIRST.NAME"].maximumStorageSize == 30
        e["LAST.NAME"].maximumStorageSize == 50
        e["FIRST.NAME"].fieldPlacement == 1
        e["LAST.NAME"].fieldPlacement == 5
    }

    def "get - caching"() {
        setup:
        def d = Mock(DmiCTXService)
        def m = new EntityMetadataService(d)
        def response = [
                "TV.CDD.NAME": "FIRST.NAME" + VM + "LAST.NAME",
                "TV.FIELD.PLACEMENT": "1" + VM + "2",
        ]

        // first attempt caches value
        when:
        m.get("CORE", "PERSON")

        then:
        1 * d.executeRaw(*_) >> response
        0 * _

        // second attempt uses cache
        m.get("CORE", "PERSON")

        then:
        0 * _

        // clear cache, set to expire immediately, both requests should call DMI
        when:
        m.clearCache()
        m.setCacheExpirationSeconds(0)
        m.get("CORE", "PERSON")
        m.get("CORE", "PERSON")

        then:
        m.getCacheExpirationSeconds() == 0
        2 * d.executeRaw(*_) >> response

        // force doesn't use cache
        when:
        m.setCacheExpirationSeconds(100)
        m.get("CORE", "PERSON", true)
        m.get("CORE", "PERSON", true)

        then:
        2 * d.executeRaw(*_) >> response

    }
}
