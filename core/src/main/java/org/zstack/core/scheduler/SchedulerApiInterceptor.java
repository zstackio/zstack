package org.zstack.core.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.volume.APICreateVolumeSnapshotSchedulerEvent;
import org.zstack.header.volume.APICreateVolumeSnapshotSchedulerMsg;
import org.zstack.header.zone.ZoneConstant;
import org.zstack.header.zone.ZoneMessage;

/**
 * Created by Mei Lei on 7/5/16.
 */
public class SchedulerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    private void setServiceId(APIMessage msg) {
        if (msg instanceof SchedulerMessage) {
            SchedulerMessage schedmsg = (SchedulerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, schedmsg.getSchedulerUuid());
        }
    }
    // meilei: to do strict check for api
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        if (msg instanceof APIDeleteSchedulerMsg) {
            validate((APIDeleteSchedulerMsg) msg);
        }
        if (msg instanceof APIUpdateSchedulerMsg) {
            validate((APIUpdateSchedulerMsg) msg);
        }
        return msg;
    }

    private void validate(APIDeleteSchedulerMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SchedulerVO.class)) {
            APIDeleteSchedulerEvent evt = new APIDeleteSchedulerEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
    private void validate(APIUpdateSchedulerMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SchedulerVO.class)) {
            APIDeleteSchedulerEvent evt = new APIDeleteSchedulerEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

}
