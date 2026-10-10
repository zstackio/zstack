package org.zstack.kvm.memory;

import java.util.*;

/** Preview and execution use the same explicit target set; batching is internal. */
final class MemoryTargetRules {
    private MemoryTargetRules() { }
    static List<String> select(String scope, List<String> available, List<String> requested) {
        if ("Global".equals(scope) || "Cluster".equals(scope)) {
            if (requested == null || requested.isEmpty()
                    || requested.contains(null) || !new HashSet<>(available).containsAll(requested)) {
                throw new MemoryOperationException("MEMORY_INVALID_TARGETS",
                        "Select existing KVM Hosts explicitly within the policy scope");
            }
            return new ArrayList<>(new TreeSet<>(requested));
        }
        if (requested != null) {
            throw new MemoryOperationException("MEMORY_INVALID_TARGETS", "targetHostUuids is only valid for Global or Cluster scope");
        }
        return available;
    }
}
