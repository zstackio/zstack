package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

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
 *
 * @summary 创建云主机
 */
@TagResourceType(VmInstanceVO.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances",
        method = HttpMethod.POST,
        responseClass = APICreateVmInstanceEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreateVmInstanceMsg extends APICreateMessage implements APIAuditor, NewVmInstanceMessage2 {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255, minLength = 1)
    private String name;
    /**
     * @desc uuid of instance offering. See :ref:`InstanceOfferingInventory`
     */
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true, required = false)
    private String instanceOfferingUuid;

    @APIParam(required = false)
    private Integer cpuNum;

    @APIParam(required = false)
    private Long memorySize;

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

    @APIParam(required = false)
    private Long rootDiskSize;

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
//    @APIParam(required = false, maxLength = 32, checkAccount = true, validRegexValues = VmInstanceConstant.USER_VM_REGEX_PASSWORD)
//    private String rootPassword;

    private String defaultL3NetworkUuid;

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false)
    private List<String> rootVolumeSystemTags;

    @APIParam(required = false)
    private List<String> dataVolumeSystemTags;

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

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
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

    public Long getRootDiskSize() {
        return rootDiskSize;
    }

    public void setRootDiskSize(Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public List<String> getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(List<String> dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
    }

    public static APICreateVmInstanceMsg __example__() {
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setName("vm1");
        msg.setDescription("this is a vm");
        msg.setClusterUuid(uuid());
        msg.setDataDiskOfferingUuids(asList(uuid(), uuid()));
        msg.setImageUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVmInstanceEvent)rsp).getInventory().getUuid() : "", VmInstanceVO.class);
    }
}
