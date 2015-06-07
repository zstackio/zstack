package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

/**
 * Created by frank on 5/23/2015.
 */
public class DownloadIsoToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private ImageSpec isoSpec;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public ImageSpec getIsoSpec() {
        return isoSpec;
    }

    public void setIsoSpec(ImageSpec isoSpec) {
        this.isoSpec = isoSpec;
    }
}
