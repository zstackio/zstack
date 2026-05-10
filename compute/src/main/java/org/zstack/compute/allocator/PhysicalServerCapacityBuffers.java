package org.zstack.compute.allocator;

/**
 * Mixed-deployment safety-buffer arithmetic shared by
 * {@link PhysicalServerCapacityUpdater#_recalculate} (subtracts buffer from
 * {@code PhysicalServerCapacityVO.available*} only when the host carries more
 * than one role) and {@code ContainerNodeCordonService.evaluate} (cordon
 * hysteresis cushion).
 *
 * <p>Reads {@link HostAllocatorGlobalConfig#PHYSICAL_SERVER_CPU_SAFETY_BUFFER_PERCENT}
 * and {@link HostAllocatorGlobalConfig#PHYSICAL_SERVER_MEMORY_SAFETY_BUFFER_PERCENT}
 * at call time — config changes take effect on the next call without restart.
 * Floors keep the buffer non-trivial on small-capacity hosts where the percent
 * computation rounds to 0.
 */
public final class PhysicalServerCapacityBuffers {
    public static final long CPU_BUFFER_FLOOR = 4L;
    public static final long MEMORY_BUFFER_FLOOR = 4L * 1024L * 1024L * 1024L;

    public static long calcCpuBuffer(long totalCpu) {
        int pct = HostAllocatorGlobalConfig.PHYSICAL_SERVER_CPU_SAFETY_BUFFER_PERCENT
                .value(Integer.class);
        return Math.max(CPU_BUFFER_FLOOR, totalCpu * pct / 100);
    }

    public static long calcMemBuffer(long totalMemory) {
        int pct = HostAllocatorGlobalConfig.PHYSICAL_SERVER_MEMORY_SAFETY_BUFFER_PERCENT
                .value(Integer.class);
        return Math.max(MEMORY_BUFFER_FLOOR, totalMemory * pct / 100);
    }

    private PhysicalServerCapacityBuffers() {}
}
