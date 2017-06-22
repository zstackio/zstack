package org.zstack.scheduler.storage.volume.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeCreateSnapshotMsg;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.APICreateSchedulerJobMsg;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerFacadeImpl;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;

/**
 * Created by Mei Lei on 7/11/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVolumeSnapshotJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CreateVolumeSnapshotJob.class);

    @Autowired
    private transient AccountManager acntMgr;
    private String volumeUuid;

    public CreateVolumeSnapshotJob(APICreateSchedulerJobMsg msg) {
        super(msg);
    }

    public CreateVolumeSnapshotJob() {
        super();
    }


    @Override
    public void run() {
        logger.debug(String.format("run scheduler for job: CreateVolumeSnapshotJob; volume uuid is %s", volumeUuid));
        VolumeCreateSnapshotMsg cmsg = new VolumeCreateSnapshotMsg();
        cmsg.setName(volumeUuid + "-snapshot-" + new Timestamp(System.currentTimeMillis()).toString());
        cmsg.setVolumeUuid(volumeUuid);
        cmsg.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(getVolumeUuid()));
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, getVolumeUuid());
        if (SchedulerFacadeImpl.taskRunning.get(volumeUuid) == null || ! SchedulerFacadeImpl.taskRunning.get(volumeUuid)) {
            SchedulerFacadeImpl.taskRunning.put(volumeUuid, true);
            bus.send(cmsg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        logger.debug(String.format("CreateVolumeSnapshotJob for volume %s success", volumeUuid));
                    } else {
                        logger.debug(String.format("CreateVolumeSnapshotJob for volume %s failed", volumeUuid));
                    }
                    SchedulerFacadeImpl.taskRunning.put(volumeUuid, false);
                }
            });
        } else {
            logger.debug(String.format("CreateVolumeSnapshotJob for volume %s didn't finish, scheduler will ignore this trigger", volumeUuid));
        }
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

}
