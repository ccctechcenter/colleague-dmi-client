package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

@Getter
@Setter
@Entity(appl = "ST", name = "STUDENT.COURSE.SEC")
public class StudentCourseSecRecord extends ColleagueRecord {

    String scsPassAudit;

    @Join
    CourseSectionsRecord scsCourseSection;

}
