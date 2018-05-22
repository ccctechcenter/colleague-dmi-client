package org.ccctc.colleaguedmiclient.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Metadata for a Colleague Entity
 */
@Getter
@AllArgsConstructor
public class EntityMetadata {

    /**
     * Entity type
     */
    private final String entityType;

    /**
     * Guid is Enabled
     */
    private final String guidEnabled;

    /**
     * Field names and CDD entries
     */
    private final Map<String, CddEntry> entries;

    /**
     * CDD entries ordered by their position in the entity
     */
    private final CddEntry[] orderedEntries;
}
