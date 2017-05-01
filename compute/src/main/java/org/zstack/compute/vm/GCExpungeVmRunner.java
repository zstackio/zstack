package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by david on 1/14/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCExpungeVmRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        GCExpungeVmContext ctx = (GCExpungeVmContext) context.getContext();
        String rootVolumeUuid = ctx.getInventory().getRootVolumeUuid();
        VolumeVO vvo = dbf.findByUuid(rootVolumeUuid, VolumeVO.class);

        if (vvo == null) {
            completion.success();
            return;
        }

        DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
        dmsg.setVolume(VolumeInventory.valueOf(vvo));
        dmsg.setUuid(vvo.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, vvo.getPrimaryStorageUuid());
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
