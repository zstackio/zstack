package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.StorageTrash;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "storageTrashes")
public class APIGetTrashOnPrimaryStorageReply extends APIReply {
    private List<StorageTrash> storageTrashes = new ArrayList<>();

    public List<StorageTrash> getStorageTrashes() {
        return storageTrashes;
    }

    public void setStorageTrashes(List<StorageTrash> storageTrashes) {
        this.storageTrashes = storageTrashes;
    }

    public static APIGetTrashOnPrimaryStorageReply __example__() {
        APIGetTrashOnPrimaryStorageReply reply = new APIGetTrashOnPrimaryStorageReply();

        reply.setStorageTrashes(asList(new StorageTrash(uuid(), "VolumeVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath", 1024000L)));
        reply.setStorageTrashes(asList(new StorageTrash(uuid(), "VolumeSnapshotVO", uuid(), "PrimaryStorageVO", "/zstack_ps/installpath/snapshot", 1024000L)));

        return reply;
    }
}