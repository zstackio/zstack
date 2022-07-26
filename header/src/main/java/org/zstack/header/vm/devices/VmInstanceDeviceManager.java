package org.zstack.header.vm.devices;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;
import java.util.Map;

public interface VmInstanceDeviceManager {
    String MEM_BALLOON_UUID = "4780bf6d2fa65700f22e36c27e8ff05c";
    
    /**
     * create or update vm device address,
     * if no VmInstanceDeviceAddressVO with current resource, a new
     * record will be created, else update the existing one.
     *
     * @param resourceUuid uuid of resource need record device address
     * @param deviceAddress a instance of deviceAddressConfig record device address
     * @param vmInstanceUuid vm uuid of resource need record device address
     * @param metadata a string of anything request to be record with the device
     * @param metadataClass the canonical class name of metadata
     * @return VmInstanceDeviceAddressVO result vo of device address
     */
    VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(String resourceUuid, DeviceAddress deviceAddress, String vmInstanceUuid, String metadata, String metadataClass);

    /**
     * create or update vm device address,
     * if no VmInstanceDeviceAddressVO with current resource, a new
     * record will be created, else update the existing one.
     *
     * @param virtualDeviceInfo contains resourceUuid and deviceInfo a structure oriented method
     * @param vmInstanceUuid vm uuid of resource
     * @return VmInstanceDeviceAddressVO result vo of device address
     */
    VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(VirtualDeviceInfo virtualDeviceInfo, String vmInstanceUuid);

    /**
     * get vm device address
     *
     * @param resourceUuid the uuid of resource that want to get device address
     * @param vmInstanceUuid vm uuid of resource
     * @return DeviceAddressConfig device address of resourceUuid
     */
    DeviceAddress getVmDeviceAddress(String resourceUuid, String vmInstanceUuid);

    /**
     * delete vm device address
     *
     * @param resourceUuid the uuid of resource that want to delete device address
     * @param vmInstanceUuid vm uuid of resource
     * @return ErrorCode if success it is null else not
     */
    ErrorCode deleteVmDeviceAddress(String resourceUuid, String vmInstanceUuid);

    /**
     * delete vm related all devices' address
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @return ErrorCode if success it is null else not
     */
    ErrorCode deleteAllDeviceAddressesByVm(String vmInstanceUuid);

    /**
     * modify virtio ,pci address is modify, need vm clean related devices
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @return ErrorCode if success it is null else not
     */

    ErrorCode deleteDeviceAddressesByVmModifyVirtIO(String vmInstanceUuid);

    /**
     * archive current device address
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to mark those vm related
     *                               address as a group. note: do not use a duplicate
     *                               archiveForResourceUuid to confuse yourself
     * @return VmInstanceDeviceAddressGroupVO the group marked by archiveForResourceUuid
     * and has references with all vm current related address
     */
    VmInstanceDeviceAddressGroupVO archiveCurrentDeviceAddress(String vmInstanceUuid, String archiveForResourceUuid);

    /**
     * revert current vm device address to a specific device
     * address group
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @return List<VmInstanceDeviceAddressVO> a list of vm device address
     */
    List<VmInstanceDeviceAddressVO> revertDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid);


    List<VmInstanceDeviceAddressVO> revertRequestedDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, List<String> needRevertResourceUuidList);
    /**
     * create device address from archive
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address and create device address for
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @param resourceMap resource map will be used for uuid mapping, for example if new vm use uuidA to mark the
     *                    first disk which use uuidB, resourceMap.put(uuidB, uuidA), when create device address,
     *                    address with uuidB will be used to create a record with uuidA
     * @return List<VmInstanceDeviceAddressVO> a list of vm device address
     */
    List<VmInstanceDeviceAddressVO> createDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, Map<String, String> resourceMap);

    /**
     * delete archive device address group
     *
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     */
    void deleteArchiveVmInstanceDeviceAddressGroup(String archiveForResourceUuid);

    /**
     * get archive info from archiveForResourceUuid
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @param metadataClass the canonical class name of metadata
     * @return List<VmInstanceDeviceAddressArchiveVO> a list of vm archive device address
     */
    List<VmInstanceDeviceAddressArchiveVO> getAddressArchiveInfoFromArchiveForResourceUuid(String vmInstanceUuid, String archiveForResourceUuid, String metadataClass);
}
