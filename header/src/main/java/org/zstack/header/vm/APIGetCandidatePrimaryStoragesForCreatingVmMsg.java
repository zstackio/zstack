package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by MaJin on 2017-08-16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/candidate-storages",
        method = HttpMethod.GET,
        responseClass = APIGetCandidatePrimaryStoragesForCreatingVmReply.class
)

public class APIGetCandidatePrimaryStoragesForCreatingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = ImageVO.class, checkAccount = true)
    private String imageUuid;
    @APIParam(resourceType = L3NetworkVO.class, nonempty = true, checkAccount = true)
    private List<String> l3NetworkUuids;
    @APIParam(required = false, resourceType = DiskOfferingVO.class, checkAccount = true)
    private String rootDiskOfferingUuid;
    @APIParam(required = false, nonempty = true, resourceType = DiskOfferingVO.class, checkAccount = true)
    private List<String> dataDiskOfferingUuids;
    private String zoneUuid;
    private String clusterUuid;
    private String defaultL3NetworkUuid;

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
