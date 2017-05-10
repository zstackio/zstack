package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.exception.CloudRuntimeException;
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

    private List<HostVO> allocate(List<HostVO> vos, long cpu, long memory) {
        List<HostVO> ret = vos.stream()
                .filter(hvo -> hvo.getCapacity().getAvailableCpu() >= cpu
                        && ratioMgr.calculateHostAvailableMemoryByRatio(hvo.getUuid(), hvo.getCapacity().getAvailableMemory()) >= memory)
                .collect(Collectors.toList());

        return ret;
    }

    private boolean isNoCpu(int cpu) {
        return !candidates.stream().anyMatch(vo -> vo.getCapacity().getCpuNum() >= cpu);
    }

    private boolean isNoMemory(long mem) {
        return !candidates.stream().anyMatch(vo -> ratioMgr.calculateHostAvailableMemoryByRatio(vo.getUuid(), vo.getCapacity().getAvailableMemory()) >= mem);
    }

    @Override
    public void allocate() {
        List<HostVO> ret;
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException("HostCapacityAllocatorFlow cannot be the first allocator flow");
        } else {
            ret = allocate(candidates, spec.getCpuCapacity(), spec.getMemoryCapacity());
        }

        ret = reserveMgr.filterOutHostsByReservedCapacity(ret, spec.getCpuCapacity(), spec.getMemoryCapacity());

        if (ret.isEmpty()) {
            fail(String.format("no host having cpu[%s], memory[%s bytes] found",
                    spec.getCpuCapacity(), spec.getMemoryCapacity()));
        } else {
            next(ret);
        }
    }
}
