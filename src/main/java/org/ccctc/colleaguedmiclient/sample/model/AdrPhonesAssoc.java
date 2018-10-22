package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.AssociationEntity;

@Getter
@Setter
@AssociationEntity
public class AdrPhonesAssoc {

    String addressPhones;
    String addressPhoneType;
    String addressPhoneExtension;

}
