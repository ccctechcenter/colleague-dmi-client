package org.ccctc.colleaguedmiclient.model;

import lombok.Data;

import java.util.List;

/**
 * Represents the result of an operation where a byte array is split at a delimiter and the last item of the split is
 * stored in the remainder.
 */
@Data
public class ByteSplitRemainder {
    /**
     * List of byte arrays
     */
    private final List<byte[]> split;

    /**
     * Remainder byte array
     */
    private final byte[] remainder;
}
