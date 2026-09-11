package org.zstack.compute.allocator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.ResourceBindingStrategy;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * VM Cluster Binding Strategy Allocator
 * <p>
 * Truth Table (3 variables, 8 combinations):
 * | # | vm.ha.across.clusters | resourceBinding.strategy | Cluster Has Resources | Expected Behavior |
 * |---|----------------------|-------------------------|----------------------|-------------------|
 * | 1 | true                 | Hard                    | true                 | Migrate Freely    |
 * | 2 | true                 | Hard                    | false                | Migrate Freely    |
 * | 3 | true                 | Soft                    | true                 | Migrate Freely    |
 * | 4 | true                 | Soft                    | false                | Migrate Freely    |
 * | 5 | false                | Hard                    | true                 | Migrate in Current Cluster |
 * | 6 | false                | Hard                    | false                | Fail              |
 * | 7 | false                | Soft                    | true                 | Prefer Current Cluster, Keep Cross-Cluster Fallback |
 * | 8 | false                | Soft                    | false                | Try Other Clusters |
 *
 * @author yh.w (original), refactored for ZSTAC-75428
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceBindingAllocatorFlow extends AbstractHostAllocatorFlow {

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        String vmUuid = spec.getVmInstance().getUuid();
        String currentClusterUuid = spec.getVmInstance().getClusterUuid();
        Boolean allowAcrossClusters = rcf
                .getResourceConfigValue(VmGlobalConfig.VM_HA_ACROSS_CLUSTERS, vmUuid, Boolean.class);

        // Truth table #1-4: If cross-cluster is allowed, pass through (migrate freely)
        if (allowAcrossClusters) {
            next(candidates);
            return;
        }

        // Skip binding check if current cluster is null
        if (StringUtils.isBlank(currentClusterUuid)) {
            next(candidates);
            return;
        }

        String strategy = rcf.getResourceConfigValue(
                VmGlobalConfig.RESOURCE_BINDING_STRATEGY, vmUuid, String.class);

        // Truth table #5-8: Cross-cluster not allowed, check current cluster resources
        // Cluster resource sufficient: Enabled + Connected hosts excluding current VM's host
        List<HostVO> hostsInCurrentCluster = candidates.stream()
                .filter(h -> currentClusterUuid.equals(h.getClusterUuid()))
                .filter(h -> h.getState() == HostState.Enabled)
                .filter(h -> h.getStatus() == HostStatus.Connected)
                .collect(Collectors.toList());

        if (ResourceBindingStrategy.Soft.toString().equals(strategy)) {
            // Truth table #7: prefer current-cluster hosts without removing cross-cluster fallback.
            if (!hostsInCurrentCluster.isEmpty()) {
                Set<String> softAvoidHostUuids = new LinkedHashSet<>();
                if (spec.getSoftAvoidHostUuids() != null) {
                    softAvoidHostUuids.addAll(spec.getSoftAvoidHostUuids());
                }
                candidates.stream()
                        .filter(h -> !currentClusterUuid.equals(h.getClusterUuid()))
                        .map(HostVO::getUuid)
                        .forEach(softAvoidHostUuids::add);
                spec.setSoftAvoidHostUuids(new ArrayList<>(softAvoidHostUuids));
            }

            // Truth table #8: no current-cluster candidate, try other clusters directly.
            next(candidates);
            return;
        }

        if (hostsInCurrentCluster.isEmpty()) {
            // Truth table #6: Hard strategy, fail.
            fail(Platform.operr(ORG_ZSTACK_COMPUTE_ALLOCATOR_10005,
                    "no available host found in current cluster [uuid:%s], and vm.ha.across.clusters is disabled with Hard binding strategy",
                    currentClusterUuid));
            return;
        }

        // Truth table #5: Hard strategy only permits the current cluster.
        next(hostsInCurrentCluster);
    }
}
