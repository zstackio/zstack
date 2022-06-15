package org.zstack.header.vm.devices;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceDeviceAddressArchiveVO.class)
public class VmInstanceDeviceAddressArchiveVO_ {
    public static volatile SingularAttribute<VmInstanceDeviceAddressArchiveVO, Long> id;
    public static volatile SingularAttribute<VmInstanceDeviceAddressArchiveVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceDeviceAddressArchiveVO, String> addressGroupUuid;
}
