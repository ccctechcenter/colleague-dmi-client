package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.AssociationEntity;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Association: must have @AssociationEntity annotation
 *
 * Like an entity, field names are auto mapped, ie stcStatus is mapped to STC.STATUS
 */
@Getter
@Setter
@AssociationEntity
public class StcStatusesAssoc {

    String stcStatus;

    // Dates / times use LocalDate and Localtime
    LocalDate stcStatusDate;
    LocalTime stcStatusTime;

}
