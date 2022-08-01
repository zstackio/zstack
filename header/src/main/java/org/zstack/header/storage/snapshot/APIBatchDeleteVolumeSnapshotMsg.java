package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/batch-delete",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIBatchDeleteVolumeSnapshotEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 6)
public class APIBatchDeleteVolumeSnapshotMsg extends APIDeleteMessage implements APIMultiAuditor {
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(resourceType = VolumeSnapshotVO.class, successIfResourceNotExisting = true, checkAccount = true, nonempty = true, emptyString = false)
    private List<String> uuids;

    /**
     * @ignore
     */
    @APINoSee
    private String volumeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIBatchDeleteVolumeSnapshotMsg __example__() {
        APIBatchDeleteVolumeSnapshotMsg msg = new APIBatchDeleteVolumeSnapshotMsg();

        msg.setUuids(Arrays.asList(uuid(), uuid()));

        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APIBatchDeleteVolumeSnapshotMsg amsg = (APIBatchDeleteVolumeSnapshotMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        for (String uuid : amsg.getUuids()) {
            res.add(new APIAuditor.Result(uuid, VolumeSnapshotVO.class));
        }
        res.add(new APIAuditor.Result(amsg.volumeUuid, VolumeVO.class));

        return res;
    }
}
