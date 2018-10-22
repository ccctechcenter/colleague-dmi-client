package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.AssociationEntity;

import java.math.BigDecimal;

@Getter
@Setter
@AssociationEntity
public class SecContactAssoc {

    String secInstrMethods;
    BigDecimal secLoad;
    BigDecimal secContactHours;
    BigDecimal secClockHours;
    String secContactMeasures;

}
