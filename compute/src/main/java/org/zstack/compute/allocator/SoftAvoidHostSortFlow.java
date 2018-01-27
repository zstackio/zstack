package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SoftAvoidHostSortFlow extends AbstractHostSortorFlow {
    @Override
    public void sort() {
        subCandidates.clear();
        subCandidates.addAll(candidates);

        List<String> softAvoidHosts = spec.getSoftAvoidHostUuids();
        if (softAvoidHosts == null || softAvoidHosts.isEmpty()) {
            return;
        }

        subCandidates.removeIf(inv -> softAvoidHosts.contains(inv.getUuid()));
    }

    @Override
    public boolean skipNext() {
        return false;
    }
}
