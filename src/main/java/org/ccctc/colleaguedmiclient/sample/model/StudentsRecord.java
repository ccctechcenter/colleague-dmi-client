package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.util.List;

@Getter
@Setter
@Entity(appl = "ST", name = "STUDENTS")
public class StudentsRecord extends ColleagueRecord {

    // join to STUDENT.PROGRAMS. the primary key of STUDENT.PROGRAMS is a multi-valued key so we need to
    // specify a "prefix" for our join, in this case the student ID.
    @Join(prefixKeys = "@ID")
    List<StudentProgramsRecord> stuAcadPrograms;

    // another mv key join
    @Join(prefixKeys = "@ID")
    List<StudentAcadLevelsRecord> stuAcadLevels;
}
