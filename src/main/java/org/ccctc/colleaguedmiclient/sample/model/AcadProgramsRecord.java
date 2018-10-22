package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

@Getter
@Setter
@Entity(appl = "ST", name = "ACAD.PROGRAMS")
public class AcadProgramsRecord extends ColleagueRecord {

    String acpgTitle;
    String acpgDesc;
    String acpgDegree;
    String[] acpgCcds;
    String acpgCip;

}
