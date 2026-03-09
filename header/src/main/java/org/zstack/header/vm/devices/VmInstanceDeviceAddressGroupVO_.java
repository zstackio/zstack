package org.zstack.header.vm.devices;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceDeviceAddressGroupVO.class)
public class VmInstanceDeviceAddressGroupVO_ {
    public static volatile SingularAttribute<VmInstanceDeviceAddressGroupVO, String> uuid;
    public static volatile SingularAttribute<VmInstanceDeviceAddressGroupVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceDeviceAddressArchiveVO, String> vmInstanceUuid;
}
