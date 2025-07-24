package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostReservedCapacityExtensionPoint;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.RecalculateHostCapacityMsg;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class KvmHostReserveExtension implements HostReservedCapacityExtensionPoint, Component {
    private static CLogger logger = Utils.getLogger(KvmHostReserveExtension.class);

    private ReservedHostCapacity reserve = new ReservedHostCapacity();

    @Autowired
    ResourceConfigFacade rcf;
    @Autowired
    CloudBus bus;

    @Override
    public String getHypervisorTypeForHostReserveCapacityExtension() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacity(String hostUuid) {
        ReservedHostCapacity hc = new ReservedHostCapacity();
        String reserveCpu = rcf.getResourceConfigValue(KVMGlobalConfig.RESERVED_CPU_CAPACITY, hostUuid, String.class);
        hc.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(reserveCpu));
        String reserveMem = rcf.getResourceConfigValue(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY, hostUuid, String.class);
        hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(reserveMem));
        return hc;
    }

    @Override
    public Map<String, ReservedHostCapacity> getReservedHostsCapacity(List<String> hostUuids) {
        Map<String, ReservedHostCapacity> results = new HashMap<>();

        Map<String, String> memoryValues = rcf.getResourceConfigValues(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY, hostUuids, String.class);
        Map<String, String> cpuValues = rcf.getResourceConfigValues(KVMGlobalConfig.RESERVED_CPU_CAPACITY, hostUuids, String.class);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("KvmHostReserveExtension get reserved capacity for hosts %s, memory config: %s, cpu config: %s",
                    hostUuids, JSONObjectUtil.toJsonString(memoryValues), JSONObjectUtil.toJsonString(cpuValues)));
        }

        for (String hostUuid : hostUuids) {
            String reserveMem = memoryValues.get(hostUuid);
            String reserveCpu = cpuValues.get(hostUuid);
            ReservedHostCapacity hc = new ReservedHostCapacity();
            hc.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(reserveCpu != null ? reserveCpu : "0"));
            hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(reserveMem != null ? reserveMem : "0"));
            results.put(hostUuid, hc);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("KvmHostReserveExtension returns %s for hosts %s", JSONObjectUtil.toJsonString(results), hostUuids));
        }

        return results;
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

        ResourceConfig reservedCpuConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_CPU_CAPACITY.getIdentity());
        reservedCpuConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
        reservedCpuConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));

        ResourceConfig reservedConfig = rcf.getResourceConfig(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.getIdentity());
        reservedConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
        reservedConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));

        return true;
    }

    private void recalculateHostCapacity(String resourceUuid, String resourceType) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, HostAllocatorConstant.SERVICE_ID, resourceUuid);
        if (resourceType.equals(ZoneVO.class.getSimpleName())) {
            msg.setZoneUuid(resourceUuid);
        } else if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            msg.setClusterUuid(resourceUuid);
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            msg.setHostUuid(resourceUuid);
        }
        bus.send(msg);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
