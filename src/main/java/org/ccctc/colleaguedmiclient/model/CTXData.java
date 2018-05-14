package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Result from a CTX Transaction
 */
@Data
public class CTXData {
    /**
     * Variables returned from the CTX. Will include all variables, even those that are part of an association.
     */
    private final Map<String, Object> variables;

    /**
     * Associations returned from the CTX. Associations are keyed by the name of the association and contain a list
     * of the maps of associated values.
     */
    private final Map<String, List<Map<String, Object>>> associations;
}
