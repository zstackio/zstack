package org.zstack.header.vm.devices;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceDeviceAddressVO.class)
public class VmInstanceDeviceAddressVO_ {
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, Long> id;
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, String> deviceAddress;
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, String> metadata;
    public static volatile SingularAttribute<VmInstanceDeviceAddressVO, String> metadataClass;
}
