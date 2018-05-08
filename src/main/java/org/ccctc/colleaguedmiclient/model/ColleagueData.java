package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

import java.util.Map;

@Data
public class ColleagueData {
    /**
     * Primary key
     */
    private final String key;

    /**
     * Field names and values
     */
    private final Map<String, Object> values;

    public <T> T value(String key) {
        Object value = values.get(key);
        return (T) value;
    }
}
