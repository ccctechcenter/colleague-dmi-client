package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Field;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.util.List;

@Getter
@Setter
@Entity(appl = "ST", name = "STUDENT.ACAD.LEVELS")
public class StudentAcadLevelsRecord extends ColleagueRecord {

    // extra fields from primary key
    @Field(value = "@ID[1]")
    String studentId;
    @Field(value = "@ID[2]")
    String acadLevel;

    // the primary key of STUDENT.ACAD.LEVELS is (student)*(acad level)
    // the primary key of STUDENT.RERMS is (student)*(term)*(acad level)
    // so we need to use a prefix and a suffix off components of the primary key to perform the join
    @Join(prefixKeys = "@ID[1]", suffixKeys = "@ID[2]")
    List<StudentTermsRecord> staTerms;
}
