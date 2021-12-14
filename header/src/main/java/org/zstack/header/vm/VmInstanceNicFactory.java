package org.zstack.header.vm;

import org.zstack.header.network.l3.UsedIpInventory;

import java.util.List;

public interface VmInstanceNicFactory {
    VmNicType getType();
    VmNicVO createVmNic(VmNicInventory inv, VmInstanceSpec spec, List<UsedIpInventory> ips);
    VmNicVO createApplianceVmNic(VmNicInventory inv, VmInstanceSpec spec);

    static VmNicVO createVmNic(VmNicInventory nic) {
        VmNicVO vnic = new VmNicVO();
        vnic.setUuid(nic.getUuid());
        vnic.setIp(nic.getIp());
        vnic.setL3NetworkUuid(nic.getL3NetworkUuid());
        vnic.setUsedIpUuid(nic.getUsedIpUuid());
        vnic.setVmInstanceUuid(nic.getVmInstanceUuid());
        vnic.setDeviceId(nic.getDeviceId());
        vnic.setMac(nic.getMac());
        vnic.setHypervisorType(nic.getHypervisorType());
        vnic.setNetmask(nic.getNetmask());
        vnic.setGateway(nic.getGateway());
        vnic.setIpVersion(nic.getIpVersion());
        vnic.setInternalName(nic.getInternalName());
        vnic.setDriverType(nic.getDriverType());
        return vnic;
    }

    static VmNicVO createApplianceVmNic(VmNicInventory nic) {
        VmNicVO vnic = createVmNic(nic);
        vnic.setMetaData(nic.getMetaData());
        return vnic;
    }
}
