package org.zstack.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VmHardwareExtensionPoint implements VmInstanceStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmHardwareExtensionPoint.class);

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
            return "cpuSockets must be specified";
        }

        int socketNum = Integer.parseInt(sockets);
        int coreNum, threadNum;
        if (threads == null) {
            threadNum = 1;
        } else {
            threadNum = Integer.parseInt(threads);
        }

        if (cores == null) {
            coreNum = inv.getCpuNum() / threadNum / socketNum;
        } else {
            coreNum = Integer.parseInt(cores);
        }

        if (inv.getCpuNum() != coreNum * threadNum * socketNum) {
            return String.format("cpu topology is not correct, cpuNum[%s], configured cpuSockets[%s], cpuCores[%s], cpuThreads[%s];" +
                            " Calculated cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                    inv.getCpuNum(), sockets, cores, threads, socketNum, coreNum, threadNum);
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

        logger.debug(String.format("cpu topology is correct, cpuNum[%s], configured cpuSockets[%s], cpuCores[%s], cpuThreads[%s]. " +
                        "Calculated cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                inv.getCpuNum(), sockets, cores, threads, socketNum, coreNum, threadNum));

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
}
