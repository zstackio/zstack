package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "storageTrashes")
public class APIGetTrashOnBackupStorageReply extends APIReply {
    private List<StorageTrash> storageTrashes = new ArrayList<>();

    public List<StorageTrash> getStorageTrashes() {
        return storageTrashes;
    }

    public void setStorageTrashes(List<StorageTrash> storageTrashes) {
        this.storageTrashes = storageTrashes;
    }

    public static APIGetTrashOnBackupStorageReply __example__() {
        APIGetTrashOnBackupStorageReply reply = new APIGetTrashOnBackupStorageReply();

        reply.setStorageTrashes(asList(new StorageTrash(uuid(), "ImageVO", uuid(), "BackupStorageVO", "/zstack_bs/installpath", 1024000L)));

        return reply;
    }
}
