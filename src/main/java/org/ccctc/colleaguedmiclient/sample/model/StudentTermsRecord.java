package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.time.LocalDate;

@Getter
@Setter
@Entity(appl = "ST", name = "STUDENT.TERMS")
public class StudentTermsRecord extends ColleagueRecord {

    LocalDate sttrRegDate;
    String sttrCalworksStatus;

}
