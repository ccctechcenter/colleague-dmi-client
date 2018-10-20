package org.ccctc.colleaguedmiclient.service

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.AssociationEntity
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Field
import org.ccctc.colleaguedmiclient.annotation.Ignore
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.exception.DmiServiceException
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import spock.lang.Specification

class DmiEntityServiceSpec extends Specification {

    def dmiDataService = Mock(DmiDataService)
    def dmiEntityService = new DmiEntityService(dmiDataService)


    def "clearCache"() {
        setup:
        dmiEntityService.entityCache.put("test", null)
        assert dmiEntityService.entityCache.size() == 1

        when:
        dmiEntityService.clearCache()

        then:
        dmiEntityService.entityCache.size() == 0
    }

    def "setCacheExpirationSeconds / getCacheExpirationSeconds"() {
        when: dmiEntityService.setCacheExpirationSeconds(100)
        then: dmiEntityService.getCacheExpirationSeconds() == 100
    }

    def "isConcurrentQueries / enableConcurrentQueries / disableConcurrentQueries"() {
        when: dmiEntityService.enableConcurrentQueries()
        then: dmiEntityService.isConcurrentQueries()
        when: dmiEntityService.disableConcurrentQueries()
        then: !dmiEntityService.isConcurrentQueries()
    }

    def "readForEntity"() {
        when:
        def result = dmiEntityService.readForEntity("key", TestRecord.class)
        then:
        1 * dmiDataService.singleKey(*_) >> null
        result == null

        when:
        result = dmiEntityService.readForEntity("key", TestRecord.class)

        then:
        1 * dmiDataService.singleKey("ST", "TEST", *_) >> testData
        1 * dmiDataService.singleKey("ST", "JOIN", *_) >> testJoin1
        1 * dmiDataService.singleKey("ST", "JOIN.MV", *_) >> mvJoin
        1 * dmiDataService.batchKeys(*_) >> [testJoin1, testJoin2, testJoin3]
        0 * _
        result.class == TestRecord.class
        result.testValue == "test"
        result.testValue2 == "test2"
        result.ignoreMe == null
        result.aJoin.value == "value"
        result.aJoin.boolValue == true
        result.join2[0].boolValue == true
        result.join2[1].boolValue == false
        result.join2[2].boolValue == null
        result.join3.value == "value"
        result.join3.key5 == "SUF"
        result.assoc[0].assocValue == "A"
        result.assoc[1].assocValue == "B"
    }

    def "readForEntity - missing entity annotation"() {
        when:
        dmiEntityService.readForEntity("k", ColleagueRecord.class)
        then:
        thrown DmiServiceException
    }

    def testData = new ColleagueData("KEY", [
            "TEST.VALUE": "test",
            "TEST.VALUE2": "test2",
            "IGNORE.ME": "ignored",
            "A.JOIN": "1",
            "JOIN.FIELD2": ["1", "2", "3"] as String[],
            "ASSOC.VALUE": ["A", "B"] as String[],
            // values for multi-valued join
            "SOME.KEY": "PRE",
            "OTHER.KEY": "SUF",
            "MV.JOIN": "1"
    ])

    def testJoin1 = new ColleagueData("1", ["VALUE": "value", "BOOL.VALUE": "Y"])
    def testJoin2 = new ColleagueData("2", ["VALUE": "value", "BOOL.VALUE": "N"])
    def testJoin3 = new ColleagueData("3", ["VALUE": "value", "BOOL.VALUE": null])

    def mvJoin = new ColleagueData("KEY*PRE*1*KEY*SUF", ["VALUE": "value"])

    @CompileStatic
    @Entity(appl = "ST", name = "TEST")
    static class TestRecord extends ColleagueRecord {

        String testValue

        @Field(value = "TEST.VALUE2")
        String testValue2

        @Ignore
        String ignoreMe

        @Join
        JoinRecord aJoin

        @Join(value = "JOIN.FIELD2")
        List<JoinRecord> join2

        @Association
        List<TestAssoc> assoc

        @Join(value = "MV.JOIN", prefixKeys = ["@ID", "SOME.KEY"], suffixKeys = ["@ID", "OTHER.KEY"])
        JoinMvRecord join3

    }

    @CompileStatic
    @Entity(appl = "ST", name = "JOIN")
    static class JoinRecord extends ColleagueRecord {

        String value
        Boolean boolValue

    }

    @CompileStatic
    @Entity(appl = "ST", name = "JOIN.MV")
    static class JoinMvRecord extends ColleagueRecord {

        String value

        @Field(value="@ID[5]")
        String key5

    }


    @CompileStatic
    @AssociationEntity
    static class TestAssoc {

        String assocValue

    }
}
