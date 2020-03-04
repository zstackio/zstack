package org.zstack.header.vm;

public interface VmInstanceNicFactory {
    VmNicType getType();
    VmNicVO createVmNic(VmNicInventory inv, VmInstanceSpec spec);

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
        return vnic;
    }
}
