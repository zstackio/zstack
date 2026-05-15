package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.progress.ProgressReportService.taskProgress;

/**
 * Placed after VmAllocateNicIpFlow in the flow chain.
 *
 * For each NIC belonging to an SDN-managed L2 network, this flow delegates
 * to the registered {@link AfterAllocateSdnNicExtensionPoint} implementations
 * (typically {@code SdnControllerManagerImpl}) which:
 *
 * - OVN: calls addLogicalPorts() with already-allocated IPs
 * - ZNS: calls createSegmentPort(), receives IP back from ZNS, writes to DB
 * - H3C/Sugon: default no-op
 *
 * Non-SDN NICs are skipped by the extension implementation internally.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateSdnNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateSdnNicFlow.class);
    public static final String ALLOCATED_IPS_ON_START = VmAllocateNicForStartingVmFlow.class.getName();

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        taskProgress("create SDN ports for nics");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> nics = spec.getDestNics();
        List<UsedIpInventory> allocatedIpsOnStart = (List<UsedIpInventory>) data.get(VmAllocateNicForStartingVmFlow.class);
        if (allocatedIpsOnStart != null) {
            spec.putExtensionData(ALLOCATED_IPS_ON_START, allocatedIpsOnStart.stream()
                    .map(UsedIpInventory::getVmNicUuid)
                    .collect(Collectors.toList()));
        }

        if (nics == null || nics.isEmpty()) {
            trigger.next();
            return;
        }

        List<AfterAllocateSdnNicExtensionPoint> exts =
                pluginRgty.getExtensionList(AfterAllocateSdnNicExtensionPoint.class);
        if (exts.isEmpty()) {
            trigger.next();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> {
            ext.afterAllocateSdnNic(spec, nics, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> nics = spec.getDestNics();

        List<AfterAllocateSdnNicExtensionPoint> exts =
                pluginRgty.getExtensionList(AfterAllocateSdnNicExtensionPoint.class);

        if (exts.isEmpty() || nics == null || nics.isEmpty()) {
            chain.rollback();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> {
            ext.rollbackSdnNic(spec, nics, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    // best-effort: log and continue
                    logger.warn(String.format("failed to rollback SDN nic: %s", errorCode));
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                chain.rollback();
            }
        });
    }
}
