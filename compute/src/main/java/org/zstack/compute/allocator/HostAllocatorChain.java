package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.inerr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostAllocatorChain implements HostAllocatorTrigger, HostAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(HostAllocatorChain.class);

    private HostAllocatorSpec allocationSpec;
    private String name;
    private List<AbstractHostAllocatorFlow> flows;

    private Iterator<AbstractHostAllocatorFlow> it;
    private ErrorCode errorCode;

    private List<HostVO> result = null;
    private boolean isDryRun;
    private ReturnValueCompletion<List<HostInventory>> completion;
    private ReturnValueCompletion<List<HostInventory>> dryRunCompletion;

    private int skipCounter = 0;

    private AbstractHostAllocatorFlow lastFlow;

    private HostAllocationPaginationInfo paginationInfo;

    private Set<ErrorCode> seriesErrorWhenPagination = new HashSet<>();

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;

    public HostAllocatorSpec getAllocationSpec() {
        return allocationSpec;
    }

    public void setAllocationSpec(HostAllocatorSpec allocationSpec) {
        this.allocationSpec = allocationSpec;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AbstractHostAllocatorFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<AbstractHostAllocatorFlow> flows) {
        this.flows = flows;
    }

    private void done() {
        if (result == null) {
            if (isDryRun) {
                if (HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(errorCode.getCode())) {
                    dryRunCompletion.success(new ArrayList<HostInventory>());
                } else {
                    dryRunCompletion.fail(errorCode);
                }
            } else {
                completion.fail(errorCode);
            }
            return;
        }

        // in case a wrong flow returns an empty result set
        if (result.isEmpty()) {
            if (isDryRun) {
                dryRunCompletion.fail(err(HostAllocatorError.NO_AVAILABLE_HOST,
                        "host allocation flow doesn't indicate any details"));
            } else {
                completion.fail(err(HostAllocatorError.NO_AVAILABLE_HOST,
                        "host allocation flow doesn't indicate any details"));
            }
            return;
        }

        if (isDryRun) {
            dryRunCompletion.success(HostInventory.valueOf(result));
        } else {
            completion.success(HostInventory.valueOf(result));
        }
    }

    private void startOver() {
        it = flows.iterator();
        result = null;
        skipCounter = 0;
        runFlow(it.next());
    }

    private void runFlow(AbstractHostAllocatorFlow flow) {
        try {
            lastFlow = flow;
            flow.setCandidates(result);
            flow.setSpec(allocationSpec);
            flow.setTrigger(this);
            flow.setPaginationInfo(paginationInfo);
            flow.allocate();
        } catch (OperationFailureException ofe) {
            if (ofe.getErrorCode().getCode().equals(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getCode())) {
                logger.debug(String.format("[Host Allocation]: intermediate failure; " +
                                "because of pagination, will start over allocation again; " +
                                "current pagination info %s; failure details: %s",
                        JSONObjectUtil.toJsonString(paginationInfo), ofe.getErrorCode().getDetails()));
                seriesErrorWhenPagination.add(ofe.getErrorCode());
                startOver();
            } else {
                fail(ofe.getErrorCode());
            }
        } catch (Throwable t) {
            logger.warn("unhandled throwable", t);
            completion.fail(inerr(t.getMessage()));
        }
    }

    private void start() {
        for (HostAllocatorPreStartExtensionPoint processor : pluginRgty.getExtensionList(HostAllocatorPreStartExtensionPoint.class)) {
            processor.beforeHostAllocatorStart(allocationSpec, flows);
        }

        if (HostAllocatorGlobalConfig.USE_PAGINATION.value(Boolean.class)) {
            paginationInfo = new HostAllocationPaginationInfo();
            paginationInfo.setLimit(HostAllocatorGlobalConfig.PAGINATION_LIMIT.value(Integer.class));
        }
        it = flows.iterator();
        DebugUtils.Assert(it.hasNext(), "can not run an empty host allocation chain");
        runFlow(it.next());
    }

    private void allocate(ReturnValueCompletion<List<HostInventory>> completion) {
        isDryRun = false;
        this.completion = completion;
        start();
    }

    private void dryRun(ReturnValueCompletion<List<HostInventory>> completion) {
        isDryRun = true;
        this.dryRunCompletion = completion;
        start();
    }

    @Override
    public void next(List<HostVO> candidates) {
        DebugUtils.Assert(candidates != null, "cannot pass null to next() method");
        DebugUtils.Assert(!candidates.isEmpty(), "cannot pass empty candidates to next() method");
        result = candidates;
        VmInstanceInventory vm = allocationSpec.getVmInstance();
        logger.debug(String.format("[Host Allocation]: flow[%s] successfully found %s candidate hosts for vm[uuid:%s, name:%s]",
                lastFlow.getClass().getName(), result.size(), vm.getUuid(), vm.getName()));
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("[Host Allocation Details]:");
            for (HostVO vo : result) {
                sb.append(String.format("\ncandidate host[name:%s, uuid:%s, zoneUuid:%s, clusterUuid:%s, hypervisorType:%s]",
                        vo.getName(), vo.getUuid(), vo.getZoneUuid(), vo.getClusterUuid(), vo.getHypervisorType()));
            }
            logger.trace(sb.toString());
        }

        if (it.hasNext()) {
            runFlow(it.next());
            return;
        }

        done();
    }

    @Override
    public void skip() {
        logger.debug(String.format("[Host Allocation]: flow[%s] asks to skip itself, we are running to the next flow",
                lastFlow.getClass()));
        if (it.hasNext()) {
            if (isFirstFlow(lastFlow)) {
                skipCounter++;
            }
            runFlow(it.next());
            return;
        }

        done();
    }

    @Override
    public boolean isFirstFlow(AbstractHostAllocatorFlow flow) {
        return flows.indexOf(flow) == skipCounter;
    }

    @Override
    public void fail(ErrorCode errorCode) {
        result = null;
        if (seriesErrorWhenPagination.isEmpty()) {
            logger.debug(String.format("[Host Allocation] flow[%s] failed to allocate host; %s",
                    lastFlow.getClass().getName(), errorCode.getDetails()));
            this.errorCode = errorCode;
        } else {
            this.errorCode = err(HostAllocatorError.NO_AVAILABLE_HOST, seriesErrorWhenPagination.iterator().next(), "unable to allocate hosts; due to pagination is enabled, " +
                    "there might be several allocation failures happened before;" +
                    " the error list is %s", seriesErrorWhenPagination.stream().map(ErrorCode::getDetails).collect(Collectors.toList()));
            logger.debug(this.errorCode.getDetails());
        }

        done();
    }

    @Override
    public void allocate(HostAllocatorSpec spec, ReturnValueCompletion<List<HostInventory>> completion) {
        this.allocationSpec = spec;
        allocate(completion);
    }

    @Override
    public void dryRun(HostAllocatorSpec spec, ReturnValueCompletion<List<HostInventory>> completion) {
        this.allocationSpec = spec;
        dryRun(completion);
    }
}
