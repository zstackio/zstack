package org.zstack.header.vm.devices;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.devices.PciAddressConfig;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressVO;

import java.util.List;
import java.util.Map;

public interface VmInstanceDeviceManager {
    final static String MEMBALLOON_UUID = "4780bf6d2fa65700f22e36c27e8ff05c";

    VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(String resourceUuid, PciAddressConfig pciAddress, String vmInstanceUuid, String metadata, String metadataClass);

    VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(VirtualDeviceInfo virtualDeviceInfo, String vmInstanceUuid);

    PciAddressConfig getVmDevicePciAddress(String resourceUuid, String vmInstanceUuid);

    ErrorCode deleteVmDeviceAddress(String resourceUuid, String vmInstanceUuid);

    ErrorCode deleteAllDeviceAddressesByVm(String vmInstanceUuid);

    VmInstanceDeviceAddressGroupVO archiveCurrentDeviceAddress(String vmInstanceUuid, String archiveForResourceUuid);

    List<VmInstanceDeviceAddressVO> revertDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid);

    List<VmInstanceDeviceAddressVO> revertRequestedDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, List<String> needRevertResourceUuidList);

    List<VmInstanceDeviceAddressVO> createDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, Map<String, String> resourceMap);

    void deleteArchiveVmInstanceDeviceAddressGroup(String archiveForResourceUuid);

    List<VmInstanceDeviceAddressArchiveVO> getAddressArchiveInfoFromArchiveForResourceUuid(String vmInstanceUuid, String archiveForResourceUuid, String metadataClass);
}
