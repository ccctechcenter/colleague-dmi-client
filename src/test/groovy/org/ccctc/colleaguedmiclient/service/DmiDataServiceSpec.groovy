package org.ccctc.colleaguedmiclient.service

import org.ccctc.colleaguedmiclient.exception.DmiServiceException
import org.ccctc.colleaguedmiclient.model.CddEntry
import org.ccctc.colleaguedmiclient.model.EntityMetadata
import org.ccctc.colleaguedmiclient.model.SessionCredentials
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import org.ccctc.colleaguedmiclient.transaction.data.ViewType
import org.ccctc.colleaguedmiclient.util.StringUtils
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DmiDataServiceSpec extends Specification {

    DmiService dmiService
    EntityMetadataService entityMetadataService
    DmiDataService dmiDataService

    def setup() {
        dmiService = Mock(DmiService)
        entityMetadataService = Mock(EntityMetadataService)
        dmiDataService = new DmiDataService(dmiService, entityMetadataService)

        def creds = new SessionCredentials("token", "controldid", LocalDateTime.now().plusDays(1))

        dmiService.getAccount() >> "account"
        dmiService.getSharedSecret() >> "secret"
        dmiService.getSessionCredentials() >> creds

    }

    def "data types and field mappings via singleKey"() {
        setup:
        // test out CDD entries of each all types
        def cdds = [new CddEntry("DATA.FIELD", null, "VIEW", 10, 1, "D", "10", null, null, "D", null, null),
                    new CddEntry("INT.FIELD", null, "VIEW", 5, 2, "D", "10", null, "MD0", "D", null, null),
                    new CddEntry("LONG.FIELD", null, "VIEW", 20, 3, "D", "10", null, "MD0\$", "D", null, null),
                    new CddEntry("DEC.FIELD", null, "VIEW", null, 4, "D", "10", null, "MD25", "D", null, null),
                    new CddEntry("DATE.FIELD", null, "VIEW", null, 5, "D", "10", null, "D4/", "D", null, null),
                    new CddEntry("TIME.FIELD", null, "VIEW", null, 6, "D", "10", null, "MTH", "D", null, null),
                    new CddEntry("ASSOC.FIELD1", null, "VIEW", 10, 7, "A", "10", null, null, "D", "ASSOC1", "K"),
                    new CddEntry("ASSOC.FIELD2", null, "VIEW", 10, 8, "A", "10", null, "MD2,", "D", "ASSOC1", "D"),
                    new CddEntry("LIST.FIELD", null, "VIEW", 10, 9, "L", "10", null, null, "D", null, null),
                    new CddEntry("Q.FIELD", null, "VIEW", 10, 10, "Q", "10", null, null, "D", null, null),
                    new CddEntry("X.FIELD", null, "VIEW", 10, 11, "X", "10", null, null, "D", null, null)
        ]

        def metadata = new EntityMetadata("VIEW", null, cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])
        def fieldList = cdds.collect {i -> i.name}

        // expected / seeded values
        def stringValue = "string"
        def intValue = 12345
        def longValue = -1234567890L
        def decValue = new BigDecimal("3.12345")
        def dateValue = LocalDate.of(2018, 1, 5)
        def timeValue = LocalTime.of(12, 20, 03)
        def assocValue1 = ["first", "second"] as String[]
        def assocValue2 = [new BigDecimal("123.14"), new BigDecimal("9.87")] as BigDecimal[]
        def listValue = ["list1", "list2"] as String[]
        def qValue = ["q1", "q2"] as String[]
        def xValue = null

        def response = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response.setInResponseTo("SDAFQ")
        response.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SINGLEKEY",
                null,
                "SINGLE",
                "VIEW",
                "11",
                null,
                "KEY",
                null,
                null,
                // values for each field
                stringValue,
                intValue.toString(),
                longValue.toString(),
                decValue.unscaledValue().toString(),
                StringUtils.dateToString(dateValue),
                StringUtils.timeToString(timeValue),
                String.join(StringUtils.VM as String, assocValue1),
                String.join(StringUtils.VM as String, assocValue2.collect { it.unscaledValue().toString() }),
                String.join(StringUtils.VM as String, listValue),
                String.join(StringUtils.VM as String, qValue),
                xValue,
                "VIEW.END"
        ] as String[]))

        when:
        def result = dmiDataService.singleKey("APPL", "VIEW", fieldList, "KEY")

        then:
        1 * dmiService.send(_) >> response
        1 * entityMetadataService.get("APPL", "VIEW") >> metadata
        result.key == "KEY"
        result.values.size() == fieldList.size()
        result.values["DATA.FIELD"] == stringValue
        result.values["INT.FIELD"] == intValue
        result.values["LONG.FIELD"] == longValue
        result.values["DEC.FIELD"] == decValue
        result.values["DATE.FIELD"] == dateValue
        result.values["TIME.FIELD"] == timeValue
        result.values["ASSOC.FIELD1"] == assocValue1
        result.values["ASSOC.FIELD2"] == assocValue2
        result.values["LIST.FIELD"] == listValue
        result.values["Q.FIELD"] == qValue
        result.values["X.FIELD"] == xValue
    }

    def "singleKey - not found"() {
        setup:
        def response = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response.setInResponseTo("SDAFQ")
        response.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SINGLEKEY",
                "L",
                "SINGLE",
                "VIEW",
                "0",
                null,
                "KEY",
                "00011",
                "Empty tuple",
                "VIEW.END"
        ] as String[]))

        when:
        def result = dmiDataService.singleKey("APPL", "VIEW", ["FIELD1", "FIELD2"], "KEY")

        then:
        1 * dmiService.send(_) >> response
        0 * entityMetadataService.getEntity(_, _)
        result == null
    }

    def "singleKey missing metadata, bad field name"() {
        setup:
        def response = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response.setInResponseTo("SDAFQ")
        response.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SINGLEKEY",
                null,
                "SINGLE",
                "VIEW",
                "2",
                null,
                "KEY",
                null,
                null,
                "VALUE1",
                "VALUE2",
                "VIEW.END"
        ] as String[]))

        def cdds = [new CddEntry("FIELD1", null, "VIEW", 10, 1, "D", "10", null, null, "D", null, null)]
        def metadata = new EntityMetadata("VIEW", null, cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])


        // no metadata
        when:
        dmiDataService.singleKey("APPL", "VIEW", ["FIELD1", "FIELD2"], "KEY")

        then:
        1 * dmiService.send(_) >> response
        1 * entityMetadataService.get("APPL", "VIEW") >> null
        def e = thrown DmiServiceException
        e.getMessage().contains("No entity information found")

        // missing field - FIELD2
        when:
        dmiDataService.singleKey("APPL", "VIEW", ["FIELD1", "FIELD2"], "KEY")

        then:
        1 * dmiService.send(_) >> response
        1 * entityMetadataService.get("APPL", "VIEW") >> metadata
        e = thrown DmiServiceException
        e.getMessage().contains("Invalid field requested")

    }

    def "batchKeys / batchSelect - small batch"() {
        setup:
        def cdds = [new CddEntry("FIELD1", null, "VIEW", 10, 1, "D", "10", null, null, "D", null, null),
                    new CddEntry("FIELD2", null, "VIEW", 10, 2, "D", "10", null, null, "D", null, null)]

        def metadata = new EntityMetadata("VIEW", null, cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])

        def response = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response.setInResponseTo("SDAFQ")
        response.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "BATCHKEYS",
                "L",
                "BATCH",
                "VIEW",
                "25", // size of block from "BATCH" to the end (aka size of entire response minus 4 header lines)
                null,
                null,
                null,
                null,
                "1",
                "2", // number of records returned
                "2",
                "TUPLE",
                "KEY1",
                "2",
                null,
                "VALUE1",
                "VALUE2",
                "KEY1.END",
                "TUPLE",
                "KEY2",
                "2",
                null,
                "VALUE3",
                "VALUE4",
                "KEY2.END",
                "VIEW.END"
        ] as String[]))

        def selectResponse = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        selectResponse.setInResponseTo("SDAFQ")
        selectResponse.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SELECT",
                "L",
                "SELECT",
                "VIEW",
                "6", // size of block from "SELECT" to the end (aka size of entire response minus 4 header lines)
                "KEY1",
                "KEY2",
                "VIEW.END"
        ] as String[]))


        //
        // dmiDataService.batchKeys
        //
        when:
        def result = dmiDataService.batchKeys("APPL", "VIEW", ["FIELD1", "FIELD2"], ["KEY1", "KEY2"])

        then:
        1 * dmiService.send(_) >> response
        1 * entityMetadataService.get("APPL", "VIEW") >> metadata
        result.size() == 2
        result[0].key == "KEY1"
        result[0].values["FIELD1"] == "VALUE1"
        result[0].values["FIELD2"] == "VALUE2"
        result[1].key == "KEY2"
        result[1].values["FIELD1"] == "VALUE3"
        result[1].values["FIELD2"] == "VALUE4"

        //
        // dmiDataService.batchSelect
        //
        when:
        result = dmiDataService.batchSelect("APPL", "VIEW", ["FIELD1", "FIELD2"], "criteria")

        then:
        1 * dmiService.send(_) >> selectResponse
        1 * dmiService.send(_) >> response
        1 * entityMetadataService.get("APPL", "VIEW") >> metadata
        result.size() == 2
        result[0].key == "KEY1"
        result[0].values["FIELD1"] == "VALUE1"
        result[0].values["FIELD2"] == "VALUE2"
        result[1].key == "KEY2"
        result[1].values["FIELD1"] == "VALUE3"
        result[1].values["FIELD2"] == "VALUE4"

    }

    def "batchKeys - large batch"() {
        setup:
        def cdds = [new CddEntry("FIELD1", null, "VIEW", 10, 1, "D", "10", null, null, "D", null, null),
                    new CddEntry("FIELD2", null, "VIEW", 10, 2, "D", "10", null, null, "D", null, null)]

        def metadata = new EntityMetadata("VIEW", null, cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])

        // first 1000 records
        def response1 = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response1.setInResponseTo("SDAFQ")

        def commands1 = [
                "F",
                "STANDARD",
                "BATCHKEYS",
                "L",
                "BATCH",
                "VIEW",
                (1000 * 7 + 11).toString(), // size of block from "BATCH" to the end - 1234 records of size 7 each plus 11 header/footer
                null,
                null,
                null,
                null,
                "1",
                "1000", // number of records returned
                "1000"]

        (1..1000).forEach { i -> commands1.addAll(["TUPLE", "KEY" + i, "2", null, "VALUE1", "VALUE2", "KEY" + i + ".END"]) }

        commands1 << "VIEW.END"

        response1.addSubTransaction(new DmiSubTransaction("SDAFS", 0, commands1 as String[]))

        // second batch of records
        def response2 = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response2.setInResponseTo("SDAFQ")

        def commands2 = [
                "F",
                "STANDARD",
                "BATCHKEYS",
                "L",
                "BATCH",
                "VIEW",
                (234 * 7 + 11).toString(), // size of block from "BATCH" to the end - 1234 records of size 7 each plus 11 header/footer
                null,
                null,
                null,
                null,
                "1",
                "234", // number of records returned
                "234"]

        (1001..1234).forEach { i -> commands2.addAll(["TUPLE", "KEY" + i, "2", null, "VALUE1", "VALUE2", "KEY" + i + ".END"]) }

        commands2 << "VIEW.END"

        response2.addSubTransaction(new DmiSubTransaction("SDAFS", 0, commands2 as String[]))

        when:
        def result = dmiDataService.batchKeys("APPL", "VIEW", ["FIELD1", "FIELD2"], (1..1234).collect { i -> "KEY" + i })

        then:
        1 * dmiService.send(_) >> response1
        1 * dmiService.send(_) >> response2
        2 * entityMetadataService.get("APPL", "VIEW") >> metadata
        result.size() == 1234

        // check two random values from each batch
        result[12].key == "KEY13"
        result[12].values["FIELD1"] == "VALUE1"
        result[12].values["FIELD2"] == "VALUE2"
        result[1022].key == "KEY1023"
        result[1022].values["FIELD1"] == "VALUE1"
        result[1022].values["FIELD2"] == "VALUE2"
    }

    def "batchKeys - empty keys"() {
        when:
        def result = dmiDataService.batchKeys("APPL", "VEIW", ViewType.LOGI, ["FIELD1"], [])

        then:
        result == []
    }

    def "selectKeys - all"() {
        setup:
        def selectResponse = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        selectResponse.setInResponseTo("SDAFQ")
        selectResponse.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SELECT",
                "L",
                "SELECT",
                "VIEW",
                "6", // size of block from "SELECT" to the end (aka size of entire response minus 4 header lines)
                "KEY1",
                "KEY2",
                "VIEW.END"
        ] as String[]))

        when:
        def result = dmiDataService.selectKeys("VIEW")

        then:
        1 * dmiService.send(_) >> selectResponse
        result.size() == 2
        result[0] == "KEY1"
        result[1] == "KEY2"

    }


    def "non null parameters coverage"() {
        when: new DmiDataService(null, (DmiCTXService) null)
        then: thrown NullPointerException
        when: new DmiDataService(null, (EntityMetadataService) null)
        then: thrown NullPointerException
        when: new DmiDataService(dmiService, (DmiCTXService) null)
        then: thrown NullPointerException
        when: new DmiDataService(dmiService, (EntityMetadataService) null)
        then: thrown NullPointerException

        when: dmiDataService.singleKey(null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", "VIEW", null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", "VIEW", [], null)
        then: thrown NullPointerException

        when: dmiDataService.singleKey(null, null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", "VIEW", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", "VIEW", ViewType.BLOB, null, null)
        then: thrown NullPointerException
        when: dmiDataService.singleKey("APPL", "VIEW", ViewType.BLOB, [], null)
        then: thrown NullPointerException

        when: dmiDataService.batchKeys(null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", "VIEW", null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", "VIEW", [], null)
        then: thrown NullPointerException

        when: dmiDataService.batchKeys(null, null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", "VIEW", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", "VIEW", ViewType.PERM, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchKeys("APPL", "VIEW", ViewType.PERM, [], null)
        then: thrown NullPointerException

        when: dmiDataService.batchSelect(null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", "VIEW", null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", "VIEW", [], null)
        then: thrown NullPointerException

        when: dmiDataService.batchSelect(null, null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", null, null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", "VIEW", null, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", "VIEW", ViewType.PERM, null, null)
        then: thrown NullPointerException
        when: dmiDataService.batchSelect("APPL", "VIEW", ViewType.PERM, [], null)
        then: thrown NullPointerException

        when: dmiDataService.selectKeys(null)
        then: thrown NullPointerException
        when: dmiDataService.selectKeys(null, null)
        then: thrown NullPointerException
    }
    
}