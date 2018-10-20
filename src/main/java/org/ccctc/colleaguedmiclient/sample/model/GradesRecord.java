package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.math.BigDecimal;

@Getter
@Setter
@Entity(appl = "ST", name = "GRADES")
public class GradesRecord extends ColleagueRecord {

    String grdGrade;
    String grdAttCredFlag;
    String grdCmplCredFlag;
    String grdGpaCredFlag;
    BigDecimal grdValue;
    String grdLegend;
    String grdGradeScheme;

}
