package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity(appl = "ST", name = "COURSE.SEC.MEETING")
public class CourseSecMeetingRecord extends ColleagueRecord {

    String csmInstrMethod;
    LocalDate csmStartDate;
    LocalDate csmEndDate;
    LocalTime csmStartTime;
    LocalTime csmEndTime;

    String csmBldg;
    String csmRoom;

    Boolean csmMonday;
    Boolean csmTuesday;
    Boolean csmWednesday;
    Boolean csmThursday;
    Boolean csmFriday;
    Boolean csmSaturday;
    Boolean csmSunday;

}
