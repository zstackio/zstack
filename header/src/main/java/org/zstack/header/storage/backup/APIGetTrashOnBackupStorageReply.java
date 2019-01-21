package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "storageTrashSpecs")
public class APIGetTrashOnBackupStorageReply extends APIReply {
    private List<StorageTrashSpec> storageTrashSpecs = new ArrayList<>();

    public List<StorageTrashSpec> getStorageTrashSpecs() {
        return storageTrashSpecs;
    }

    public void setStorageTrashSpecs(List<StorageTrashSpec> storageTrashSpecs) {
        this.storageTrashSpecs = storageTrashSpecs;
    }

    public static APIGetTrashOnBackupStorageReply __example__() {
        APIGetTrashOnBackupStorageReply reply = new APIGetTrashOnBackupStorageReply();

        StorageTrashSpec spec = new StorageTrashSpec(uuid(), "ImageVO", uuid(), "BackupStorageVO", "/zstack_bs/installpath", 1024000L);
        spec.setTrashId(1L);
        spec.setTrashType("MigrateImage");
        spec.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setStorageTrashSpecs(asList(spec));

        return reply;
    }
}
