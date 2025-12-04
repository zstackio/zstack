package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCapacityAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(HostCapacityAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;
    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;

    private boolean memoryCheck(long vmMemSize, long oldMemory, HostVO hvo) {
        if (HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.value(Boolean.class)) {
            if ((vmMemSize + oldMemory) >= hvo.getCapacity().getTotalPhysicalMemory()) {
                return false;
            }
        }

        return ratioMgr.calculateHostAvailableMemoryByRatio(hvo.getUuid(), hvo.getCapacity().getAvailableMemory()) >= vmMemSize;
    }


    private List<HostVO> allocate(List<HostVO> vos, long cpu, long memory, long oldMemory) {
        return vos.parallelStream()
                .filter(hvo -> (cpu == 0 || hvo.getCapacity().getAvailableCpu() >= cpu)
                        && (memory == 0 || memoryCheck(memory, oldMemory, hvo))).collect(Collectors.toList());
    }

    private boolean isNoCpu(int cpu) {
        return !candidates.stream().anyMatch(vo -> vo.getCapacity().getCpuNum() >= cpu);
    }

    private boolean isNoMemory(long mem) {
        return !candidates.stream().anyMatch(vo -> ratioMgr.calculateHostAvailableMemoryByRatio(vo.getUuid(), vo.getCapacity().getAvailableMemory()) >= mem);
    }

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        List<HostVO> ret =
                allocate(candidates, spec.getCpuCapacity(), spec.getMemoryCapacity(), spec.getOldMemoryCapacity());
        ret = reserveMgr.filterOutHostsByReservedCapacity(ret, spec.getCpuCapacity(), spec.getMemoryCapacity());

        if (ret.isEmpty()) {
            fail(Platform.operr("no host having cpu[%s], memory[%s bytes] found",
                    spec.getCpuCapacity(), spec.getMemoryCapacity()));
        } else {
            next(ret);
        }
    }
}
