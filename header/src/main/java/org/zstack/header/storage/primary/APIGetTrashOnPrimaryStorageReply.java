package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.StorageTrashSpec;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "storageTrashSpecs")
public class APIGetTrashOnPrimaryStorageReply extends APIReply {
    private List<StorageTrashSpec> storageTrashSpecs = new ArrayList<>();

    public List<StorageTrashSpec> getStorageTrashSpecs() {
        return storageTrashSpecs;
    }

    public void setStorageTrashSpecs(List<StorageTrashSpec> storageTrashSpecs) {
        this.storageTrashSpecs = storageTrashSpecs;
    }

    public static APIGetTrashOnPrimaryStorageReply __example__() {
        APIGetTrashOnPrimaryStorageReply reply = new APIGetTrashOnPrimaryStorageReply();

        StorageTrashSpec spec1 = new StorageTrashSpec(uuid(), "VolumeVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath", 1024000L);
        spec1.setTrashId(1L);
        spec1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        StorageTrashSpec spec2 = new StorageTrashSpec(uuid(), "VolumeSnapshotVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath/snapshot", 1024000L);
        spec2.setTrashId(2L);
        spec2.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setStorageTrashSpecs(asList(spec1, spec2));

        return reply;
    }
}