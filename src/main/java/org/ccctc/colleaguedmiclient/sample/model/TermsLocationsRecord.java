package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;

@Getter
@Setter
@Entity(appl = "ST", name = "TERMS.LOCATIONS")
public class TermsLocationsRecord extends ColleagueRecord {

    LocalDate tlocRegStartDate;
    LocalDate tlocRegEndDate;
    LocalDate tlocPreregStartDate;
    LocalDate tlocPreregEndDate;
    LocalDate tlocAddEndDate;
    LocalDate tlocDropEndDate;
    LocalDate tlocDropGradeReqdDate;
    LocalDate[] tlocCensusDates;

}