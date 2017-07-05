package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.core.scheduler.*;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Mei Lei on 7/5/16.
 */
public class SchedulerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof SchedulerMessage) {
            SchedulerMessage schedmsg = (SchedulerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, schedmsg.getSchedulerUuid());
        }
    }
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        if (msg instanceof APIDeleteSchedulerJobMsg) {
            validate((APIDeleteSchedulerJobMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerJobMsg) {
            validate((APIUpdateSchedulerJobMsg) msg);
        } else if (msg instanceof APICreateSchedulerJobMessage) {
            validate((APICreateSchedulerJobMessage) msg);
        } else if (msg instanceof APIChangeSchedulerStateMsg) {
            validate((APIChangeSchedulerStateMsg) msg);
        } else if (msg instanceof APICreateSchedulerTriggerMsg) {
            validate((APICreateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobToSchedulerTriggerMsg) {
            validate((APIAddSchedulerJobToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIDeleteSchedulerTriggerMsg) {
            validate((APIDeleteSchedulerTriggerMsg) msg);
        }
        return msg;
    }

    private void validate(APIDeleteSchedulerTriggerMsg msg) {
         if (Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getUuid())
                .isExists()) {
             throw new ApiMessageInterceptionException(argerr("This trigger[uuid:%s] is added to some job, please remove it first", msg.getUuid()));
         }
    }

    private void validate(APIAddSchedulerJobToSchedulerTriggerMsg msg) {
        SchedulerJobSchedulerTriggerRefVO vo = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, msg.getSchedulerJobUuid())
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getSchedulerTriggerUuid())
                .find();

        if (vo != null) {
            throw new ApiMessageInterceptionException(argerr("Can not add job[uuid:%s] twice to the same trigger[uuid:%s]", msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuid()));
        }
    }

    private void validate(APICreateSchedulerTriggerMsg msg) {
        if (msg.getSchedulerType().equals("simple")) {
            if (msg.getStartTime() == null) {
                throw new ApiMessageInterceptionException(argerr("startTime must be set when use simple scheduler"));
            } else if (msg.getStartTime() != null && msg.getStartTime() < 0) {
                throw new ApiMessageInterceptionException(argerr("startTime must be positive integer or 0"));
            } else if (msg.getStartTime() != null && msg.getStartTime() > 2147454847 ){
                //  mysql timestamp range is '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
                //  we accept 0 as startDate means start from current time
                throw new ApiMessageInterceptionException(argerr("startTime out of range"));
            }

            if (msg.getSchedulerInterval() == null && msg.getRepeatCount() == null) {
                throw new ApiMessageInterceptionException(argerr("interval must be set when use simple scheduler when repeat forever"));
            } else if (msg.getSchedulerInterval() == null && msg.getRepeatCount() != null && msg.getRepeatCount() != 1) {
                throw new ApiMessageInterceptionException(argerr("interval must be set when use simple scheduler when repeat more than once"));
            } else if (msg.getSchedulerInterval() != null && msg.getRepeatCount() != null && msg.getRepeatCount() != 0) {
                if (msg.getSchedulerInterval() <= 0) {
                    throw new ApiMessageInterceptionException(argerr("interval must be positive integer"));
                } else if (msg.getRepeatCount() <= 0) {
                    throw new ApiMessageInterceptionException(argerr("repeat count must be positive integer"));
                } else if ((long) msg.getSchedulerInterval() * (long) msg.getRepeatCount() * 1000L + msg.getStartTime() < 0 ) {
                    throw new ApiMessageInterceptionException(argerr("duration time out of range"));
                } else if ((long) msg.getSchedulerInterval() * (long) msg.getRepeatCount() * 1000L + msg.getStartTime() > 2147454847000L) {
                    throw new ApiMessageInterceptionException(argerr("stopTime out of mysql timestamp range"));
                }
            }
        } else if (msg.getSchedulerType().equals("cron")) {
            if (msg.getCron() == null || ( msg.getCron() != null && msg.getCron().isEmpty())) {
                throw new ApiMessageInterceptionException(argerr("cron must be set when use cron scheduler"));
            }
            if ( (! msg.getCron().contains("?")) || msg.getCron().split(" ").length != 6) {
                throw new ApiMessageInterceptionException(argerr("cron task must follow format like this : \"0 0/3 17-23 * * ?\" "));
            }
            if (msg.getSchedulerInterval() != null || msg.getRepeatCount() != null || msg.getStartTime() != null) {
                throw new ApiMessageInterceptionException(argerr("cron scheduler only need to specify cron task"));
            }
        }
    }

    private void validate(APIDeleteSchedulerJobMsg msg) {
    }

    private void validate(APIUpdateSchedulerJobMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SchedulerJobVO.class)) {
            APIUpdateSchedulerJobEvent evt = new APIUpdateSchedulerJobEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

    }

    private void validate(APIChangeSchedulerStateMsg msg) {
//        if (!dbf.isExist(msg.getUuid(), SchedulerJobVO.class)) {
//            APIUpdateSchedulerJobEvent evt = new APIUpdateSchedulerJobEvent(msg.getId());
//            bus.publish(evt);
//            throw new StopRoutingException();
//        }
//        SimpleQuery<SchedulerJobVO> q = dbf.createQuery(SchedulerJobVO.class);
//        q.select(SchedulerJobVO.state);
//        q.add(SchedulerJobVO.uuid, SimpleQuery.Op.EQ, msg.getUuid());
//        String state = q.findValue();
//        if (msg.getStateEvent().equals(SchedulerStateEvent.enable.toString()) && state.equals(SchedulerState.Enabled.toString())) {
//            throw new ApiMessageInterceptionException(operr("can not enable a Enabled scheduler" ));
//        }
//        if (msg.getStateEvent().equals(SchedulerStateEvent.disable.toString()) && state.equals(SchedulerState.Disabled.toString())) {
//            throw new ApiMessageInterceptionException(operr("can not disable a Disabled scheduler"));
//        }
    }

    private void validate(APICreateSchedulerJobMessage msg) {
//        ResourceVO vo = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, msg.getTargetResourceUuid()).find();
//        if (vo == null) {
//            throw new ApiMessageInterceptionException(argerr("resource[uuid:%s] does not exists", msg.getTargetResourceUuid()));
//        }
    }

}
