package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;

@Getter
@Setter
@Entity(appl = "ST", name = "COURSES")
public class CoursesRecord extends ColleagueRecord {

    String crsName;
    LocalDate crsStartDate;
    LocalDate crsEndDate;
    String[] crsLevels;
    String crsCip;
    String crsStandardArticulationNo;

}
