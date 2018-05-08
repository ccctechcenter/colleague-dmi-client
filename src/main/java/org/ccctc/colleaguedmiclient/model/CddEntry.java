package org.ccctc.colleaguedmiclient.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CddEntry {
    private final String name;
    private final String physName;
    private final String source;
    private final Integer maximumStorageSize;
    private final Integer fieldPlacement;
    private final String databaseUsageType;
    private final String defaultDisplaySize;
    private final String informFormatString;
    private final String informConversionString;
    private final String dataType;
    private final String elementAssocName;
    private final String elementAssocType;
}
