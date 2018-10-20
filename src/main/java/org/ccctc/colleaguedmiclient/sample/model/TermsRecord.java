package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity(appl = "ST", name = "TERMS")
public class TermsRecord extends ColleagueRecord {

    String termDesc;
    String termSession;
    LocalDate termStartDate;
    LocalDate termEndDate;
    LocalDate termRegStartDate;
    LocalDate termRegEndDate;
    LocalDate termPreregStartDate;
    LocalDate termPreregEndDate;
    LocalDate termAddEndDate;
    LocalDate termDropEndDate;
    LocalDate termDropGradeReqdDate;
    LocalDate[] termCensusDates;

    // this tells it to join on @ID + TERMS.LOCATIONS, where @ID is the id of the TERMS record
    // for example, if the term is 2017FA and the location is MAIN, the key will be 2017FA*MAIN
    @Join(prefixKeys = "@ID")
    List<TermsLocationsRecord> termLocations;
}
