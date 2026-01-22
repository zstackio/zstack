package org.zstack.compute.allocator;

import org.apache.commons.collections.CollectionUtils;
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

import java.util.List;
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
 * | 7 | false                | Soft                    | true                 | Migrate in Current Cluster |
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

        // Truth table #5-8: Cross-cluster not allowed, check current cluster resources
        // Cluster resource sufficient: Enabled + Connected hosts excluding current VM's host
        List<HostVO> hostsInCurrentCluster = candidates.stream()
                .filter(h -> currentClusterUuid.equals(h.getClusterUuid()))
                .filter(h -> h.getState() == HostState.Enabled)
                .filter(h -> h.getStatus() == HostStatus.Connected)
                .collect(Collectors.toList());

        // Check if current cluster has sufficient resources
        boolean clusterHasAvailableHosts = CollectionUtils.isNotEmpty(hostsInCurrentCluster);

        if (clusterHasAvailableHosts) {
            // Truth table #5, #7: Current cluster has available hosts, migrate successfully
            next(hostsInCurrentCluster);
            return;
        }

        // Current cluster has no resources, decide behavior based on strategy
        String strategy = rcf.getResourceConfigValue(VmGlobalConfig.RESOURCE_BINDING_STRATEGY, vmUuid, String.class);

        if (ResourceBindingStrategy.Soft.toString().equals(strategy)) {
            // Truth table #8: Soft strategy, try other clusters
            next(candidates);
        } else {
            // Truth table #6: Hard strategy, fail
            fail(Platform.operr(ORG_ZSTACK_COMPUTE_ALLOCATOR_10005,
                    "no available host found in current cluster [uuid:%s], and vm.ha.across.clusters is disabled with Hard binding strategy",
                    currentClusterUuid));
        }
    }
}
