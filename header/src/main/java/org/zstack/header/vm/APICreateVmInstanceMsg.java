package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

/**
 * @api create a new vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APICreateVmInstanceMsg": {
 * "name": "TestVm",
 * "instanceOfferingUuid": "1618154b462a48749ca9b114cf4a2979",
 * "imageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "l3NetworkUuids": [
 * "c4f6a370f80443798cc460ee07d56ff1",
 * "f5fbd96e0df745bdb7bc4f4c19febe65",
 * "c60285dca24d43a4b9a2e536674ddca1"
 * ],
 * "type": "UserVm",
 * "dataDiskOfferingUuids": [],
 * "description": "Test",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APICreateVmInstanceMsg": {
 * "name": "TestVm",
 * "instanceOfferingUuid": "1618154b462a48749ca9b114cf4a2979",
 * "imageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "l3NetworkUuids": [
 * "c4f6a370f80443798cc460ee07d56ff1",
 * "f5fbd96e0df745bdb7bc4f4c19febe65",
 * "c60285dca24d43a4b9a2e536674ddca1"
 * ],
 * "type": "UserVm",
 * "dataDiskOfferingUuids": [],
 * "description": "Test",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * },
 * "timeout": 1800000,
 * "id": "add5fb2198f14980adf26db572d035c5",
 * "serviceId": "api.portal",
 * "creatingTime": 1398912618016
 * }
 * }
 * @result See :ref:`APICreateVmInstanceEvent`
 * @since 0.1.0
 */
@TagResourceType(VmInstanceVO.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APICreateVmInstanceMsg extends APICreateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc uuid of instance offering. See :ref:`InstanceOfferingInventory`
     */
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true)
    private String instanceOfferingUuid;
    /**
     * @desc uuid of image. See :ref:`ImageInventory`
     */
    @APIParam(resourceType = ImageVO.class, checkAccount = true)
    private String imageUuid;
    /**
     * @desc a list of L3Network uuid the vm will create nic on. See :ref:`L3NetworkInventory`
     */
    @APIParam(resourceType = L3NetworkVO.class, nonempty = true, checkAccount = true)
    private List<String> l3NetworkUuids;
    /**
     * @desc see type of :ref:`VmInstanceInventory`
     * @choices - UserVm
     * - ApplianceVm
     */
    @APIParam(validValues = {"UserVm", "ApplianceVm"}, required = false)
    private String type;
    /**
     * @desc disk offering uuid for root volume. Optional when vm is created from RootVolumeTemplate,
     * mandatory when vm is created from ISO. See 'mediaType' of :ref:`ImageInventory`
     * @optional
     */
    @APIParam(required = false, resourceType = DiskOfferingVO.class, checkAccount = true)
    private String rootDiskOfferingUuid;
    /**
     * @desc disk offering uuid for data volumes. See :ref:`DiskOfferingInventory`
     */
    @APIParam(required = false, resourceType = DiskOfferingVO.class, checkAccount = true)
    private List<String> dataDiskOfferingUuids;
    /**
     * @desc when not null, vm will be created in the zone this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc when not null, vm will be created in the cluster this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;
    /**
     * @desc when not null, vm will be created on the host this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;
    /**
     * @desc when not null, vm will be created on the primary storage this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;
    /**
     * @desc max length of 255 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    /**
     * @desc user-defined root password
     * @optional
     */
    @APIParam(required = false, maxLength = 32, checkAccount = true, validRegexValues = VmInstanceConstant.USER_VM_REGEX_PASSWORD)
    private String rootPassword;

    private String defaultL3NetworkUuid;


    @APIParam(required = false, validValues = {"InstantStart", "JustCreate"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }
    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }
}
