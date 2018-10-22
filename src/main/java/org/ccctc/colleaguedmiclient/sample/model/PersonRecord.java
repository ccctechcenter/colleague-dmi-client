package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Association;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity(appl = "CORE", name = "PERSON")
public class PersonRecord extends ColleagueRecord {

    String firstName;
    String lastName;
    LocalDate birthDate;
    String ssn;
    String gender;
    String residenceState;

    @Association
    List<PersonAltIdsAssoc> personAltIdsAssoc;

    @Association
    List<PeopleEmailAssoc> peopleEmailAssoc;

    @Association
    List<PerphoneAssoc> perphoneAssoc;

    @Join
    AddressRecord preferredAddress;

    @Join
    List<AddressRecord> personAddresses;

}
