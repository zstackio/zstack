package org.zstack.header.notification;

/**
 * Created by lining on 2017/7/27.
 */
public interface NotificationConstant {

    String CREATE_OPERATE_NOTIFICATION_CONTENT = "Created";

    String DELETE_OPERATE_NOTIFICATION_CONTENT = "Deleted";

    String EXPUNGE_OPERATE_NOTIFICATION_CONTENT = "Expunged";

    String UPDATE_OPERATE_NOTIFICATION_CONTENT = "Updated";

    interface Volume {
        String ATTACH_DATA_VOLUME_TO_VM = "Attached to vm[uuid:%s]";
        String CREATE_VOLUME_SNAPSHOT = "Created snapshot[uuid:%s]";
        String DETACH_DATA_VOLUME_FROM_VM = "Detached from vm[uuid:%s]";
        String SYNC_VOLUME_SIZE = "SyncSize";
        String CREATED_FROM_SNAPSHOT = "Created from snapshot[uuid:%s]";
        String CREATED_FROM_VOLUME_TEMPLATE = "Created from image[uuid:%s]";
    }

    interface VmInstance {
        String ATTACH_VOLUME = "Attached volume[%s]";
        String DETACH_VOLUME = "Detached volume[%s]";
    }
}
