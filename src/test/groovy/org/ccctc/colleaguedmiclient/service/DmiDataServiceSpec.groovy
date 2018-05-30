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


    def "valcodes"() {
        setup:
        def cdds = [new CddEntry("VAL.INTERNAL.CODE"          , null, "VALCODES", 10, 1, "A", "10", null, null, "D", "VALS", "K"),
                    new CddEntry("VAL.EXTERNAL.REPRESENTATION", null, "VALCODES", 32, 2, "A", "25", null, null, "D", "VALS", "D"),
                    new CddEntry("VAL.ACTION.CODE.1"          , null, "VALCODES", 50, 3, "A", "10", null, null, "D", "VALS", "D"),
                    new CddEntry("VAL.ACTION.CODE.2"          , null, "VALCODES", 50, 4, "A", "10", null, null, "D", "VALS", "D")
        ]

        def metadata = new EntityMetadata("LOGI", "1", cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])

        def response1 = new DmiTransaction("account", "DAFS", "ST", "token", "controlid")
        response1.setInResponseTo("SDAFQ")
        response1.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SINGLEKEY",
                null,
                "SINGLE",
                "ST.VALCODES",
                "4",
                null,
                "COURSE.STATUSES",
                null,
                null,
                // values for each field
                String.join(StringUtils.VM as String, ["A", "C"]),
                String.join(StringUtils.VM as String, ["Active", "Cancelled"]),
                String.join(StringUtils.VM as String, ["1", "2"]),
                String.join(StringUtils.VM as String, ["3", "4"]),
                "ST.VALCODES.END"
        ] as String[]))

        def response2 = new DmiTransaction("account", "DAFS", "appl", "token", "controlid")
        response2.setInResponseTo("SDAFQ")
        response2.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "BATCHKEYS",
                "L",
                "BATCH",
                "ST.VALCODES",
                "29", // size of block from "BATCH" to the end (aka size of entire response minus 4 header lines)
                null,
                null,
                null,
                null,
                "1",
                "2", // number of records returned
                "2",
                "TUPLE",
                "COURSE.STATUSES",
                "4",
                null,
                String.join(StringUtils.VM as String, ["A", "C"]),
                String.join(StringUtils.VM as String, ["Active", "Cancelled"]),
                String.join(StringUtils.VM as String, ["1", "2"]),
                String.join(StringUtils.VM as String, ["3", "4"]),
                "COURSE.STATUSES.END",
                "TUPLE",
                "SECTION.STATUSES",
                "4",
                null,
                String.join(StringUtils.VM as String, ["SA", "SC"]),
                String.join(StringUtils.VM as String, ["Active", "Cancelled"]),
                String.join(StringUtils.VM as String, ["S1", "S2"]),
                null,
                "SECTION.STATUSES.END",
                "ST.VALCODES.END"
        ] as String[]))

        when:
        def v1 = dmiDataService.valcode("ST", "COURSE.STATUSES")
        def v2 = dmiDataService.valcodes("ST", ["COURSE.STATUSES", "SECTION.STATUSES"])

        then:
        2 * entityMetadataService.get("ST", "appl.VALCODES") >> metadata
        2 * dmiService.send(*_) >>> [response1, response2]
        v1.key == "COURSE.STATUSES"
        v1.entries.size() == 2
        v1.entries[0].internalCode == "A"
        v1.entries[0].externalRepresentation == "Active"
        v1.entries[0].action1 == "1"
        v1.entries[0].action2 == "3"
        v1.entries[1].internalCode == "C"
        v1.entries[1].externalRepresentation == "Cancelled"
        v1.entries[1].action1 == "2"
        v1.entries[1].action2 == "4"
        def m = v1.asMap()
        m.size() == 2
        m.get("A").externalRepresentation == "Active"
        m.get("C").externalRepresentation == "Cancelled"

        v2.size() == 2
        v2[0].key == "COURSE.STATUSES"
        v2[0].entries.size() == 2
        v2[0].entries[0].internalCode == "A"
        v2[0].entries[0].externalRepresentation == "Active"
        v2[0].entries[0].action1 == "1"
        v2[0].entries[0].action2 == "3"
        v2[0].entries[1].internalCode == "C"
        v2[0].entries[1].externalRepresentation == "Cancelled"
        v2[0].entries[1].action1 == "2"
        v2[0].entries[1].action2 == "4"
        v2[1].key == "SECTION.STATUSES"
        v2[1].entries.size() == 2
        v2[1].entries[0].internalCode == "SA"
        v2[1].entries[0].externalRepresentation == "Active"
        v2[1].entries[0].action1 == "S1"
        v2[1].entries[0].action2 == null
        v2[1].entries[1].internalCode == "SC"
        v2[1].entries[1].externalRepresentation == "Cancelled"
        v2[1].entries[1].action1 == "S2"
        v2[1].entries[1].action2 == null

    }


    def "elf translations"() {
        setup:
        def cdds = [new CddEntry("ELFT.ORIG.CODE.FIELD", null, "ELF.TRANSLATE.TABLES", 32, 1, "D", "10", null, null, null, null, null),
                    new CddEntry("ELFT.NEW.CODE.FIELD" , null, "ELF.TRANSLATE.TABLES", 32, 2, "D", "10", null, null, null, null, null),
                    new CddEntry("ELFT.DESC"           , null, "ELF.TRANSLATE.TABLES", 50, 3, "D", "10", null, null, null, null, null),
                    new CddEntry("ELFT.COMMENTS"       , null, "ELF.TRANSLATE.TABLES", 250, 4, "A", "65", null, null, null, "ELFT.ORA.COMMENTS", "K"),
                    new CddEntry("ELFT.ORIG.CODES"     , null, "ELF.TRANSLATE.TABLES", 20, 5, "A", "20", null, null, null, "ELFTBL", "K"),
                    new CddEntry("ELFT.NEW.CODES"      , null, "ELF.TRANSLATE.TABLES", 30, 6, "A", "20", null, null, null, "ELFTBL", "D"),
                    new CddEntry("ELFT.ACTION.CODES.1" , null, "ELF.TRANSLATE.TABLES", 80, 7, "A", "20", null, null, null, "ELFTBL", "D"),
                    new CddEntry("ELFT.ACTION.CODES.2" , null, "ELF.TRANSLATE.TABLES", 80, 8, "A", "20", null, null, null, "ELFTBL", "D")

        ]

        def metadata = new EntityMetadata("PHYS", null, cdds.collectEntries { i -> [i.name, i]}, cdds as CddEntry[])

        def response1 = new DmiTransaction("account", "DAFS", "CORE", "token", "controlid")
        response1.setInResponseTo("SDAFQ")
        response1.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "SINGLEKEY",
                null,
                "SINGLE",
                "ELF.TRANSLATE.TABLES",
                "8",
                null,
                "TRANS1",
                null,
                null,
                "ORIG.FIELD",
                "NEW.FIELD",
                "description",
                String.join(StringUtils.VM as String, ["comment 1", "comment 2"]),
                String.join(StringUtils.VM as String, ["A", "B"]),
                String.join(StringUtils.VM as String, ["C", "D"]),
                String.join(StringUtils.VM as String, ["1", "2"]),
                String.join(StringUtils.VM as String, ["3", "4"]),
                "ELF.TRANSLATE.TABLES.END"
        ] as String[]))

        def response2 = new DmiTransaction("account", "DAFS", "CORE", "token", "controlid")
        response2.setInResponseTo("SDAFQ")
        response2.addSubTransaction(new DmiSubTransaction("SDAFS", 0, [
                "F",
                "STANDARD",
                "BATCHKEYS",
                "L",
                "BATCH",
                "ELF.TRANSLATE.TABLES",
                "37", // size of block from "BATCH" to the end (aka size of entire response minus 4 header lines)
                null,
                null,
                null,
                null,
                "1",
                "2", // number of records returned
                "2",
                "TUPLE",
                "TRANS1",
                "8",
                null,
                "ORIG.FIELD",
                "NEW.FIELD",
                "description",
                String.join(StringUtils.VM as String, ["comment 1", "comment 2"]),
                String.join(StringUtils.VM as String, ["A", "B"]),
                String.join(StringUtils.VM as String, ["C", "D"]),
                String.join(StringUtils.VM as String, ["1", "2"]),
                String.join(StringUtils.VM as String, ["3", "4"]),
                "TRANS1.END",
                "TUPLE",
                "TRANS2",
                "8",
                null,
                "ORIG.FIELD",
                "NEW.FIELD",
                "description",
                String.join(StringUtils.VM as String, ["comment 1", "comment 2"]),
                String.join(StringUtils.VM as String, ["D", "E"]),
                String.join(StringUtils.VM as String, ["F", "G"]),
                String.join(StringUtils.VM as String, ["5", "6"]),
                null,
                "TRANS2.END",
                "ELF.TRANSLATE.TABLES.END"
        ] as String[]))

        when:
        def v1 = dmiDataService.elfTranslationTable("TRANS1")
        def v2 = dmiDataService.elfTranslationTables(["TRANS1", "TRANS2"])

        then:
        2 * entityMetadataService.get("CORE", "ELF.TRANSLATE.TABLES") >> metadata
        2 * dmiService.send(*_) >>> [response1, response2]
        v1.key == "TRANS1"
        v1.description == "description"
        v1.comments == ["comment 1", "comment 2"]
        v1.translations.size() == 2
        v1.translations[0].originalCode == "A"
        v1.translations[0].newCode == "C"
        v1.translations[0].action1 == "1"
        v1.translations[0].action2 == "3"
        v1.translations[1].originalCode == "B"
        v1.translations[1].newCode == "D"
        v1.translations[1].action1 == "2"
        v1.translations[1].action2 == "4"

        v2.size() == 2
        v2[0].toString() == v1.toString()
        def m = v2[1].asMap()
        m.size() == 2
        m.get("D").newCode == "F"
        m.get("E").newCode == "G"
        m.get("D").action1 == "5"
        m.get("D").action2 == null

    }
}