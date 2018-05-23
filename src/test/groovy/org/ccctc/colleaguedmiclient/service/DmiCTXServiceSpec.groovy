package org.ccctc.colleaguedmiclient.service

import org.ccctc.colleaguedmiclient.exception.DmiTransactionException
import org.ccctc.colleaguedmiclient.model.CTXAssociation
import org.ccctc.colleaguedmiclient.model.CTXMetadata
import org.ccctc.colleaguedmiclient.model.CTXVariable
import org.ccctc.colleaguedmiclient.model.KeyValuePair
import org.ccctc.colleaguedmiclient.model.SessionCredentials
import org.ccctc.colleaguedmiclient.model.DmiSubTransaction
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction
import org.ccctc.colleaguedmiclient.util.StringUtils
import spock.lang.Specification

import java.time.LocalDateTime

class DmiCTXServiceSpec extends Specification{

    def "extra coverage"() {
        // alternate constructor, getter for metadata service
        when:
        def d = new DmiCTXService(Mock(DmiService))
        def m = d.getCtxMetadataService()

        then:
        d != null
        m != null

        // null parameters
        when: new DmiCTXService(null)
        then: thrown NullPointerException
        when: new DmiCTXService(Mock(DmiService), null)
        then: thrown NullPointerException
        when: d.execute(null, null, null)
        then: thrown NullPointerException
        when: d.execute("appl", null, null)
        then: thrown NullPointerException
        when: d.executeRaw(null, null, null)
        then: thrown NullPointerException
        when: d.executeRaw("appl", null, null)
        then: thrown NullPointerException
    }


    def "executeRaw - missing sub transactions"() {
        setup:
        def d = Mock(DmiService)
        def c = new DmiCTXService(d)
        def t = new DmiTransaction("account", "CTRS", "application", "token", "controlId")

        when:
        c.executeRaw("CORE", "TRANSACTION.NAME", null)

        then:
        1 * d.getSessionCredentials() >> new SessionCredentials("token", "controlId", LocalDateTime.now().plusMinutes(60))
        1 * d.getAccount() >> "account"
        1 * d.getSharedSecret() >> "secret"
        1 * d.send(*_) >> t
        0 * _
        thrown DmiTransactionException
    }

    def "execute / executeRaw"() {
        setup:
        def d = Mock(DmiService)
        def m = Mock(CTXMetadataService)
        def c = new DmiCTXService(d, m)

        def metadata = new CTXMetadata("alias", null, null, 1,
                [
                    new CTXVariable("FIRST", "First", "Y", "IN", "D", null, null, "N", null, null),
                    new CTXVariable("SECOND", "Second", "Y", "INOUT", "D", null, null, "N", null, null),
                    new CTXVariable("VAR", "Var", "N", "OUT", "D", null, null, "N", null, null),
                    new CTXVariable("LIST", "List", "N", "OUT", "L", null, null, "N", null, null),
                    new CTXVariable("BOOLEAN1", "Boolean1", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN2", "Boolean2", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN3", "Boolean3", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN4", "Boolean4", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN5", "Boolean5", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN6", "Boolean6", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("BOOLEAN7", "", "Y", "OUT", "D", null, null, "Y", null, null),
                    new CTXVariable("INTEGER", "Integer", "Y", "OUT", "D", "MD0", null, "N", "5", null),
                    new CTXVariable("ASSOC1", "Assoc1", "N", "OUT", "A", null, "ASSOC", "N", null, null),
                    new CTXVariable("ASSOC2", "Assoc2", "N", "OUT", "A", null, "ASSOC", "N", null, null),
                    new CTXVariable("ASSOC3", "Assoc3", "N", "OUT", "D", null, "ASSOC", null, null, null),
                ],
                [],
                [
                    new CTXAssociation("ASSOC", "Assoc", null, null, ["ASSOC1", "ASSOC2", "ASSOC3"] as String[])
                ])

        // valid transaction
        def t = new DmiTransaction("account", "CTRS", "application", "token", "controlId")
        t.addSubTransaction(new DmiSubTransaction("SCTRS", 0, [] as String[]))
        t.addSubTransaction(new DmiSubTransaction("SCTVAL", 0,
                [null, metadata.variables.size() - 1,
                "SECOND", "SECOND", null, null,
                 "VAR", "VAR-VALUE", null, null,
                 "LIST", "1" + StringUtils.VM + "2", null, null,
                 "BOOLEAN1", "1", null, null,
                 "BOOLEAN2", "Y", null, null,
                 "BOOLEAN3", "y", null, null,
                 "BOOLEAN4", "0", null, null,
                 "BOOLEAN5", "NO", null, null,
                 "BOOLEAN6", "no", null, null,
                 "BOOLEAN7", "X", null, null,
                 "INTEGER", "123", null, null,
                 "ASSOC1", "ASSOC1-1" + StringUtils.VM + "ASSOC1-2", null, null,
                 "ASSOC2", "ASSOC2-1", null, null,
                 "ASSOC3", "ASSOC3", null, null,
                ] as String[]))

        when:
        def e1 = c.execute("CORE", "TRANSACTION.NAME",
                [new KeyValuePair<String, String>("FIRST", "FIRST"),
                 new KeyValuePair<String, String>("SECOND", "SECOND")
                ])

        then:
        1 * d.getSessionCredentials() >> new SessionCredentials("token", "controlId", LocalDateTime.now().plusMinutes(60))
        1 * d.getAccount() >> "account"
        1 * d.getSharedSecret() >> "secret"
        1 * d.send(*_) >> t
        2 * m.get("CORE", "TRANSACTION.NAME") >> metadata
        0 * _

        e1.variables["Second"] == "SECOND"
        e1.variables["Var"] == "VAR-VALUE"
        e1.variables["Boolean1"] == true
        e1.variables["Boolean2"] == true
        e1.variables["Boolean3"] == true
        e1.variables["Boolean4"] == false
        e1.variables["Boolean5"] == false
        e1.variables["Boolean6"] == false
        e1.variables["BOOLEAN7"] == null
        e1.variables["List"] == ["1", "2"]
        e1.variables["Integer"] == 123
        e1.variables["Assoc1"] == ["ASSOC1-1", "ASSOC1-2"]
        e1.variables["Assoc2"] == ["ASSOC2-1"]
        e1.associations["Assoc"].size() == 2
        e1.associations["Assoc"][0]["Assoc1"] == "ASSOC1-1"
        e1.associations["Assoc"][1]["Assoc1"] == "ASSOC1-2"
        e1.associations["Assoc"][0]["Assoc2"] == "ASSOC2-1"
        e1.associations["Assoc"][1]["Assoc2"] == null
        e1.associations["Assoc"][0]["Assoc3"] == "ASSOC3"
        e1.associations["Assoc"][1]["Assoc3"] == null

        when:
        def raw = c.executeRaw("CORE", "TRANSACTION.NAME",
                [new KeyValuePair<String, String>("FIRST", "VALUE1"),
                 new KeyValuePair<String, String>("SECOND", "VALUE1")
                ])

        then:
        1 * d.getSessionCredentials() >> new SessionCredentials("token", "controlId", LocalDateTime.now().plusMinutes(60))
        1 * d.getAccount() >> "account"
        1 * d.getSharedSecret() >> "secret"
        1 * d.send(*_) >> t
        0 * _
        raw.size() == metadata.variables.size() - 1
    }


}
