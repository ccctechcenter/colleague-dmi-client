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
@Entity(appl = "ST", name = "COURSE.SECTIONS")
public class CourseSectionsRecord extends ColleagueRecord {

    String secName;
    String secSubject;
    String secCourseNo;
    String secSynonym;
    String secShortTitle;
    LocalDate[] secOvrCensusDates;

    @Association
    List<SecContactAssoc> secContactAssoc;

    @Join
    List<CourseSecFacultyRecord> secFaculty;

    @Join
    List<CourseSecMeetingRecord> secMeeting;

}
