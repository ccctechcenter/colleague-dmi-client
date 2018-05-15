package org.ccctc.colleaguedmiclient.service

import spock.lang.Specification

class MetadataCacheSpec extends Specification {

    def cache = new MetadataCache<String>(100)

    def "overloaded methods"() {
        when:
        cache.put("key", "value")

        then:
        cache.get("key") == "value"
        cache.size() == 1
        cache.isEmpty() == false
        cache.containsKey("key")
        cache.containsValue("value")

        when:
        cache.remove("key")

        then:
        cache.isEmpty() == true

        when:
        cache.putAll([key: "value"])

        then:
        thrown UnsupportedOperationException

        when:
        cache.put("key", "value")
        def ks = cache.keySet()

        then:
        ks.size() == 1
        ks[0] == "key"

        when:
        def vs = cache.values()

        then:
        vs.size() == 1
        vs[0] == "value"

        when:
        def es = cache.entrySet()

        then:
        es.size() == 1
        es[0].key == "key"
        es[0].value == "value"
    }

    def "removeExpired"() {
        when:
        cache.setCacheExpirationSeconds(0)
        cache.put("key", "value")

        then:
        cache.entrySet().size() == 0
    }

}
