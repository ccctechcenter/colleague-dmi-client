package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;

@Getter
@Setter
@Entity(appl = "ST", name = "STUDENT.PROGRAMS")
public class StudentProgramsRecord extends ColleagueRecord {

    LocalDate[] stprStartDate;
    String[] stprStatus;

    // join on the second value of the primary key
    @Join(value = "@ID[2]")
    AcadProgramsRecord academicProgram;
}
