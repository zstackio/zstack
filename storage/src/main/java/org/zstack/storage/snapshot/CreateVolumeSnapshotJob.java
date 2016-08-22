package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.scheduler.AbstractSchedulerJob;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.CreateVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.CreateVolumeSnapshotReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.volume.APICreateVolumeSnapshotEvent;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by Mei Lei on 7/11/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVolumeSnapshotJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CreateVolumeSnapshotJob.class);

    @Autowired
    private transient AccountManager acntMgr;
    private String volumeUuid;
    private String snapShotName;
    private String snapShotDescription;

    public CreateVolumeSnapshotJob(APICreateSchedulerMessage msg) {
        super(msg);
    }

    public CreateVolumeSnapshotJob() {
        super();
    }


    @Override
    public void run() {
        logger.debug(String.format("run scheduler for job: CreateVolumeSnapshotJob; volume uuid is %s", getVolumeUuid()));
        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
        cmsg.setName(getSnapShotName());
        cmsg.setDescription(getSnapShotDescription());
        cmsg.setVolumeUuid(getVolumeUuid());
        cmsg.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(getVolumeUuid()));
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, getVolumeUuid());
        bus.send(cmsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(cmsg.getId());
                if (reply.isSuccess()) {
                    CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                    evt.setInventory(creply.getInventory());
                } else {
                    evt.setErrorCode(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }


    public String getSnapShotName() {
        return snapShotName;
    }

    public void setSnapShotName(String snapShotName) {
        this.snapShotName = snapShotName;
    }

    public String getSnapShotDescription() {
        return snapShotDescription;
    }

    public void setSnapShotDescription(String snapShotDescription) {
        this.snapShotDescription = snapShotDescription;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

}
