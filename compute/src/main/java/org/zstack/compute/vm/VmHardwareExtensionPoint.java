package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VmHardwareExtensionPoint implements VmInstanceStartExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmHardwareExtensionPoint.class);

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return verifyCpuTopology(inv);
    }

    private String verifyCpuTopology(VmInstanceInventory inv) {
        String sockets = VmHardwareSystemTags.CPU_SOCKETS.getTokenByResourceUuid(inv.getUuid(), VmHardwareSystemTags.CPU_SOCKETS_TOKEN);
        String cores = VmHardwareSystemTags.CPU_CORES.getTokenByResourceUuid(inv.getUuid(), VmHardwareSystemTags.CPU_CORES_TOKEN);
        String threads = VmHardwareSystemTags.CPU_THREADS.getTokenByResourceUuid(inv.getUuid(), VmHardwareSystemTags.CPU_THREADS_TOKEN);

        // skip if no topology
        if (sockets == null && cores == null && threads == null) {
            return null;
        }

        if (sockets == null) {
            return "cpuSockets must be specified by system tag: cpuSockets::number";
        }

        Integer cpuNum = inv.getCpuNum();
        Integer maxVcpuNum = null;
        Boolean isNuma = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, inv.getUuid(), Boolean.class);
        if (isNuma != null && isNuma) {
            maxVcpuNum = rcf.getResourceConfigValue(VmGlobalConfig.VM_MAX_VCPU, inv.getUuid(), Integer.class);
        }

        int socketNum = Integer.parseInt(sockets);
        int coreNum, threadNum;
        if (threads == null && cores != null) {
            coreNum = Integer.parseInt(cores);
            threadNum = maxVcpuNum != null ? maxVcpuNum : cpuNum / coreNum / socketNum;
        } else if (threads == null) {
            threadNum = 1;
        } else {
            threadNum = Integer.parseInt(threads);
        }

        if (cores == null) {
            coreNum = maxVcpuNum != null ? maxVcpuNum : cpuNum / threadNum / socketNum;
        } else {
            coreNum = Integer.parseInt(cores);
        }

        if ((maxVcpuNum != null ? maxVcpuNum : cpuNum) != coreNum * threadNum * socketNum) {
            return String.format("cpu topology is not correct, cpuNum[%s], maxVcpuNum[%s], configured cpuSockets[%s], cpuCores[%s], cpuThreads[%s];" +
                            " Calculated cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                    cpuNum, maxVcpuNum, sockets, cores, threads, socketNum, coreNum, threadNum);
        }

        // update missing topology tag
        if (!String.valueOf(threadNum).equals(threads)) {
            SystemTagCreator creator = VmHardwareSystemTags.CPU_THREADS.newSystemTagCreator(inv.getUuid());
            creator.setTagByTokens(map(
                    e(VmHardwareSystemTags.CPU_THREADS_TOKEN, String.valueOf(threadNum))
            ));
            creator.recreate = true;
            creator.create();
        }

        if (!String.valueOf(coreNum).equals(cores)) {
            SystemTagCreator creator = VmHardwareSystemTags.CPU_CORES.newSystemTagCreator(inv.getUuid());
            creator.setTagByTokens(map(
                    e(VmHardwareSystemTags.CPU_CORES_TOKEN, String.valueOf(coreNum))
            ));
            creator.recreate = true;
            creator.create();
        }

        logger.debug(String.format("cpu topology is correct, cpuNum[%s], maxVcpuNum[%s], configured cpuSockets[%s], cpuCores[%s], cpuThreads[%s]. " +
                        "Calculated cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                cpuNum, maxVcpuNum, sockets, cores, threads, socketNum, coreNum, threadNum));

        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return verifyCpuTopology(inv);
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {

    }
}
