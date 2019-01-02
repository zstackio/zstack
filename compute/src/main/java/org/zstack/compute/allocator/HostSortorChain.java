package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostSortorChain implements HostSortorStrategy {
    private static final CLogger logger = Utils.getLogger(HostSortorChain.class);
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ErrorFacade errf;

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

    private void reserveHost(HostInventory host, Completion cmpl){
        Map data = new HashMap();
        data.put(HostAllocatorConstant.Param.HOST, host);
        data.put(HostAllocatorConstant.Param.SPEC, allocationSpec);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName("hostAllocation-reserve-flow");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "hostAllocation-reserve-capacity";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
                        try {
                            reserveCapacity(host);
                            data.put(HostAllocatorConstant.Param.CAP_SUCCESS, true);
                            trigger.next();
                        } catch (UnableToReserveHostCapacityException e) {
                            logger.debug(String.format("[Host Allocation]: %s on host[uuid:%s]. try next one",
                                    e.getMessage(), host.getUuid()), e);
                            trigger.fail(operr(
                                    "[Host Allocation]: %s on host[uuid:%s]. try next one. %s", e.getMessage(), host.getUuid(), e.getMessage()));
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        boolean success = (boolean)data.get(HostAllocatorConstant.Param.CAP_SUCCESS);
                        if (success) {
                            HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
                            rollbackCapacity(host);
                        }
                        trigger.rollback();
                    }
                });

                for (HostAllocatorReserveExtensionPoint exp: pluginRgty.getExtensionList(HostAllocatorReserveExtensionPoint.class)) {
                    flow(exp.getExtension());
                }

                done(new FlowDoneHandler(cmpl) {
                    @Override
                    public void handle(Map data) {
                        cmpl.success();
                    }
                });

                error(new FlowErrorHandler(cmpl) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        cmpl.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void done(List<HostInventory> hosts) {
        if (isDryRun) {
            dryRunCompletion.success(hosts);
        } else {
            try {
                List<HostInventory> selectedHosts = new ArrayList<>();
                List<ErrorCode> errs = new ArrayList<>();
                new While<>(hosts).each((h, wcmpl) -> {
                    reserveHost(h, new Completion(wcmpl) {
                        @Override
                        public void success() {
                            selectedHosts.add(h);
                            /* alldone() will break the new While loop */
                            wcmpl.allDone();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errs.add(errorCode);
                            wcmpl.done();
                        }
                    });
                }).run(new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        if (!selectedHosts.isEmpty()) {
                            completion.success(selectedHosts.get(0));
                        } else {
                            /* return the error of last host */
                            completion.fail(errs.get(errs.size() - 1));
                        }
                    }
                });
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

    private void rollbackCapacity(final HostInventory host) {
        new HostAllocatorChain().reserveCapacity(host.getUuid(), 0L - allocationSpec.getCpuCapacity(),0L - allocationSpec.getMemoryCapacity());
        logger.debug(String.format("[Host Allocation]: successfully rollback cpu[%s], memory[%s bytes] on host[uuid:%s] for vm[uuid:%s]",
                allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity(), host.getUuid(),
                allocationSpec.getVmInstance().getUuid()));
    }
}
