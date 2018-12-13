package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.StorageTrashSpec;

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

        reply.setStorageTrashSpecs(asList(new StorageTrashSpec(uuid(), "VolumeVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath", 1024000L)));
        reply.setStorageTrashSpecs(asList(new StorageTrashSpec(uuid(), "VolumeSnapshotVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath/snapshot", 1024000L)));

        return reply;
    }
}