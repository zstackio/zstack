package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.APINoSee;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/8/17.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/candidate-destinations",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateZonesClustersHostsForCreatingVmReply.class
)
public class APIGetCandidateZonesClustersHostsForCreatingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true, required = false)
    private String instanceOfferingUuid;
    @APIParam(required = false, resourceType = ImageVO.class, checkAccount = true)
    private String imageUuid;
    @APIParam(required = false, resourceType = VolumeSnapshotVO.class, checkAccount = true)
    private String volumeSnapshotUuid;

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    @APIParam(required = false, resourceType = VolumeSnapshotGroupVO.class, checkAccount = true)
    private String volumeSnapshotGroupUuid;

    public String getVolumeSnapshotGroupUuid() {
        return volumeSnapshotGroupUuid;
    }

    public void setVolumeSnapshotGroupUuid(String volumeSnapshotGroupUuid) {
        this.volumeSnapshotGroupUuid = volumeSnapshotGroupUuid;
    }


    @APIParam(resourceType = L3NetworkVO.class, nonempty = true, checkAccount = true)
    private List<String> l3NetworkUuids;
    @APIParam(required = false, resourceType = DiskOfferingVO.class, checkAccount = true)
    private String rootDiskOfferingUuid;
    @APIParam(numberRange = {1, Long.MAX_VALUE}, numberRangeUnit = {"byte", "bytes"}, required = false)
    private Long rootDiskSize;
    @APIParam(required = false, nonempty = true, resourceType = DiskOfferingVO.class, checkAccount = true)
    private List<String> dataDiskOfferingUuids;
    @APIParam(numberRange = {1, 1024}, required = false)
    private Integer cpuNum;
    @APIParam(numberRange = {1, Long.MAX_VALUE}, numberRangeUnit = {"byte", "bytes"}, required = false)
    private Long memorySize;
    private String zoneUuid;
    private String clusterUuid;
    private String defaultL3NetworkUuid;

    @APINoSee
    private String requiredHostUuid;
    @APINoSee
    private Set<String> requiredPrimaryStorageUuids = new HashSet<>();

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
    }

    public Set<String> getRequiredPrimaryStorageUuids() {
        return requiredPrimaryStorageUuids;
    }

    public void setRequiredPrimaryStorageUuids(Set<String> requiredPrimaryStorageUuids) {
        this.requiredPrimaryStorageUuids = requiredPrimaryStorageUuids;
    }

    public Long getRootDiskSize() {
        return rootDiskSize;
    }

    public void setRootDiskSize(Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
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

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
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

    public static APIGetCandidateZonesClustersHostsForCreatingVmMsg __example__() {
        APIGetCandidateZonesClustersHostsForCreatingVmMsg msg = new APIGetCandidateZonesClustersHostsForCreatingVmMsg();
        msg.setClusterUuid(uuid());
        msg.setDataDiskOfferingUuids(asList(uuid(), uuid()));
        msg.setImageUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));
        return msg;
    }

}
