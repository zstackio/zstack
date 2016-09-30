package org.zstack.kvm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostReservedCapacityExtensionPoint;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.utils.SizeUtils;

/**
 */
public class KvmHostReserveExtension implements HostReservedCapacityExtensionPoint, Component {
    private ReservedHostCapacity reserve = new ReservedHostCapacity();
    @Override
    public String getHypervisorTypeForHostReserveCapacityExtension() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacity() {
        return reserve;
    }

    @Override
    public boolean start() {
        long cpu = SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_CPU_CAPACITY.value());
        long mem = SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value());
        reserve.setReservedCpuCapacity(cpu);
        reserve.setReservedMemoryCapacity(mem);

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                reserve.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(newConfig.value()));
            }
        });
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                reserve.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(newConfig.value()));
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
