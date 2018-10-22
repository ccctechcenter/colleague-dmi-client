package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Association;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.util.List;

@Getter
@Setter
@Entity(appl = "CORE", name = "ADDRESS")
public class AddressRecord extends ColleagueRecord {

    String[] addressLines;
    String city;
    String state;
    String zip;

    @Association
    List<AdrPhonesAssoc> adrPhonesAssoc;

}
