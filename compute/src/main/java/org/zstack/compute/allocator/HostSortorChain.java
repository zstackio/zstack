package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.*;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostSortorChain implements HostSortorStrategy {
    private static final CLogger logger = Utils.getLogger(HostSortorChain.class);
    @Autowired
    private PluginRegistry pluginRgty;

    private HostAllocatorSpec allocationSpec;

    private List<AbstractHostSortorFlow> flows;

    private boolean isDryRun = false;
    private ReturnValueCompletion<HostInventory> completion;
    private ReturnValueCompletion<List<HostInventory>> dryRunCompletion;

    public List<AbstractHostSortorFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<AbstractHostSortorFlow> flows) {
        this.flows = flows;
    }

    @Override
    public void sort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<HostInventory> completion) {
        allocationSpec = spec;
        this.completion = completion;
        sort(hosts);
    }

    // adjust hosts
    private void reSortHosts(final List<HostInventory> sub, List<HostInventory> hosts) {
        DebugUtils.Assert(sub.size() <= hosts.size(), "subHosts' size cannot larger than hosts' size");
        hosts.removeAll(sub);
        hosts.addAll(0, sub);
    }

    private void sort(List<HostInventory> hosts) {
        DebugUtils.Assert(hosts.size() > 0, "must sort at least 1 host");
        List<HostInventory> subHosts = new ArrayList<>();
        subHosts.addAll(hosts);
        for (AbstractHostSortorFlow flow: flows) {
            if (subHosts.size() == 0) {
                break;
            }

            flow.setCandidates(subHosts);
            flow.setSpec(allocationSpec);
            logger.debug(String.format("sort by flow: %s", flow.getClass().getSimpleName()));
            flow.sort();
            reSortHosts(subHosts, hosts);
            subHosts = flow.getSubCandidates();

            if (flow.skipNext()) {
                break;
            }
        }
        done(hosts);
    }

    @Override
    public void dryRunSort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<List<HostInventory>> completion) {
        allocationSpec = spec;
        this.dryRunCompletion = completion;
        isDryRun = true;
        sort(hosts);
    }

    private void done(List<HostInventory> hosts) {
        if (isDryRun) {
            dryRunCompletion.success(hosts);
        } else {
            try {
                for (HostInventory h : hosts) {
                    try {
                        reserveCapacity(h);
                        completion.success(h);
                        return;
                    } catch (UnableToReserveHostCapacityException e) {
                        logger.debug(String.format("[Host Allocation]: %s on host[uuid:%s]. try next one",
                                e.getMessage(), h.getUuid()), e);
                    }
                }
                completion.fail(Platform.err(HostAllocatorError.NO_AVAILABLE_HOST,
                        "reservation on cpu/memory failed on all candidates host"));
            } catch (Throwable t) {
                completion.fail(Platform.inerr(t.getMessage()));
            }
        }
    }

    private void reserveCapacity(final HostInventory host) {
        new HostAllocatorChain().reserveCapacity(host.getUuid(), allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity());
        logger.debug(String.format("[Host Allocation]: successfully reserved cpu[%s], memory[%s bytes] on host[uuid:%s] for vm[uuid:%s]",
                allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity(), host.getUuid(),
                allocationSpec.getVmInstance().getUuid()));
    }
}
