package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
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
    @Autowired
    private PluginRegistry pluginRgty;

    private boolean memoryCheck(long vmMemSize, long oldMemory, HostVO hvo) {
        if (HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.value(Boolean.class)) {
            if ((vmMemSize + oldMemory) >= hvo.getCapacity().getTotalPhysicalMemory()) {
                return false;
            }
        }

        // aggregate reserved huge page bytes from extensions (non-overlapping, non-negative)
        long hugePageMemReservedForSysCom = 0L;
        final List<HugePageMemoryUsageExtensionPoint> extps =
                pluginRgty.getExtensionList(HugePageMemoryUsageExtensionPoint.class);
        for (HugePageMemoryUsageExtensionPoint extp : extps) {
            try {
                long usage = Math.max(0L, extp.getHugePageMemoryUsage(hvo.getUuid()));
                hugePageMemReservedForSysCom = Math.addExact(hugePageMemReservedForSysCom, usage);
            } catch (ArithmeticException ae) {
                logger.warn(String.format("reserved huge page bytes overflow on host[%s], cap to Long.MAX_VALUE",
                        hvo.getUuid()), ae);
                hugePageMemReservedForSysCom = Long.MAX_VALUE;
                break;
            } catch (Exception e) {
                logger.warn(String.format("failed to get huge page usage from %s on host[%s], ignore this extension",
                        extp.getClass().getSimpleName(), hvo.getUuid()), e);
            }
        }
        long available = hvo.getCapacity().getAvailableMemory();
        long availableAfter = Math.max(0L, available - hugePageMemReservedForSysCom);
        return ratioMgr.calculateHostAvailableMemoryByRatio(hvo.getUuid(), availableAfter) >= vmMemSize;
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
