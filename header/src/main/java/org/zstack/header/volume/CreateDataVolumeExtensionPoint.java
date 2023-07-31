package org.zstack.header.volume;

import org.zstack.header.message.Message;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateDataVolumeExtensionPoint {
    default void preCreateVolume(PreCreateVolumeContext context) {
        // empty
    }

    void beforeCreateVolume(VolumeInventory volume);

    void afterCreateVolume(VolumeVO volume);

    static class PreCreateVolumeContext {
        public Message message;
        public String name;
        public String description;
        public String diskOfferingUuid;
        public Long diskSize;
        public String primaryStorageUuid;
        public boolean createFromLun = false;

        public static PreCreateVolumeContext valueOf(APICreateDataVolumeMsg msg) {
            PreCreateVolumeContext context = new PreCreateVolumeContext();
            context.message = msg;
            context.name = msg.getName();
            context.description = msg.getDescription();
            context.diskOfferingUuid = msg.getDiskOfferingUuid();
            context.diskSize = msg.getDiskSize();
            context.primaryStorageUuid = msg.getPrimaryStorageUuid();
            return context;
        }
    }
}
