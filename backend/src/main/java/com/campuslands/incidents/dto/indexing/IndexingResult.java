package com.campuslands.incidents.dto.indexing;

import java.util.List;

/**
 * Outcome of an indexing call (matches the n8n contract in n8n/README.md).
 *
 * @param indexed number of articles upserted into the vector store
 * @param deleted number of articles removed from the vector store
 * @param errors  human-readable errors; empty when everything succeeded
 */
public record IndexingResult(int indexed, int deleted, List<String> errors) {

    public IndexingResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static IndexingResult empty() {
        return new IndexingResult(0, 0, List.of());
    }

    public static IndexingResult failed(String error) {
        return new IndexingResult(0, 0, List.of(error));
    }

    public boolean successful() {
        return errors.isEmpty();
    }
}
