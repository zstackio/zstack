package org.zstack.storage.ceph.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.ceph.CephGlobalConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kayo on 2018/7/24.
 */
public class CephDeleteVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public VolumeInventory volume;

    private final static String name_format = "gc-ceph-%s-volume-%s";

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setInstallPath(volume.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    void deduplicateSubmit(Long next, TimeUnit unit) {
        boolean existGc = false;

        GarbageCollectorVO gcVo = Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.name, NAME).notEq(GarbageCollectorVO_.status, GCStatus.Done).find();

        if (gcVo != null) {
            existGc = true;
        }

        if (existGc) {
            return;
        }

        submit(next, unit);
    }
}
