package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Colleague data for a single record. Contains a key (primary key of the record) and Map of key/value pairs for the rest
 * of the fields.
 */
@Getter
@AllArgsConstructor
public class ColleagueData {

    /**
     * Primary key of record
     */
    private final String key;

    /**
     * Field names and values
     */
    private final Map<String, Object> values;

}