package org.zstack.kvm.memory;

import java.util.*;

/** A failed early resource cannot monopolize a bounded reconciliation batch. */
final class MemoryBatchCursor {
    private final Map<String, String> last = new HashMap<>();

    synchronized List<String> next(String scope, Collection<String> candidates, int limit) {
        if (limit <= 0) { throw new IllegalArgumentException("Batch size must be positive"); }
        List<String> ordered = new ArrayList<>(new TreeSet<>(candidates));
        if (ordered.isEmpty()) { last.remove(scope); return Collections.emptyList(); }
        String previous = last.get(scope);
        int start = previous == null ? 0 : Collections.binarySearch(ordered, previous);
        if (previous != null) { start = start < 0 ? -start - 1 : start + 1; }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, ordered.size()); i++) {
            result.add(ordered.get((start + i) % ordered.size()));
        }
        last.put(scope, result.get(result.size() - 1));
        return result;
    }
}
