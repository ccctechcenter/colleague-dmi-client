package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity(appl = "ST", name = "COURSE.SEC.FACULTY")
public class CourseSecFacultyRecord extends ColleagueRecord {

    LocalDate csfStartDate;
    LocalDate csfEndDate;
    String csfInstrMethod;
    BigDecimal csfFacultyLoad;

    @Join
    PersonRecord csfFaculty;
}
