package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PreferredClusterHostSortFlow extends AbstractHostSortorFlow {
    @Override
    public void sort() {
        subCandidates.clear();

        String preferClusterUuid = spec.getPreferClusterUuid();
        if (preferClusterUuid != null) {
            Set<String> softAvoidHostUuids = toSet(spec.getSoftAvoidHostUuids());
            candidates.sort((a, b) -> Integer.compare(
                    hostRank(preferClusterUuid, softAvoidHostUuids, a),
                    hostRank(preferClusterUuid, softAvoidHostUuids, b)));
        }
        subCandidates.addAll(candidates);
    }

    private Set<String> toSet(List<String> hostUuids) {
        if (hostUuids == null || hostUuids.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(hostUuids);
    }

    private int hostRank(String preferClusterUuid, Set<String> softAvoidHostUuids, HostInventory host) {
        int rank = preferClusterUuid.equals(host.getClusterUuid()) ? 0 : 1;
        if (softAvoidHostUuids.contains(host.getUuid())) {
            rank += 2;
        }
        return rank;
    }

    @Override
    public boolean skipNext() {
        return false;
    }
}
