package org.zstack.core.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;

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
    // meilei: to do strict check for api
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        if (msg instanceof APIDeleteSchedulerMsg) {
            validate((APIDeleteSchedulerMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerMsg) {
            validate((APIUpdateSchedulerMsg) msg);
        } else if (msg instanceof APICreateSchedulerMessage ) {
            validate((APICreateSchedulerMessage) msg);
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
            APIUpdateSchedulerEvent evt = new APIUpdateSchedulerEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

    }

    private void validate(APICreateSchedulerMessage msg) {
        if (msg.getType().equals("simple")) {
            if (msg.getInterval() == null) {
                if (msg.getRepeatCount() != null) {
                    if (msg.getRepeatCount() != 1) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                                String.format("interval must be set when use simple scheduler when repeat more than once")
                        ));
                    }
                } else {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("interval must be set when use simple scheduler when repeat forever")
                    ));
                }
            } else if (msg.getInterval() != null && msg.getInterval() <= 0) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("interval must be positive integer")
                ));
            }

            if (msg.getStartTime() == null) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("startDate must be set when use simple scheduler")
                ));
            } else if (msg.getStartTime() != null && msg.getStartTime() < 0) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("startDate must be positive integer or 0")
                ));
            } else if (msg.getStartTime() != null && msg.getStartTime() > 2147454847 ){
                //  mysql timestamp range is '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
                //  we accept 0 as startDate means start from current time
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("startDate out of range")
                ));
            }

            if (msg.getRepeatCount() != null && msg.getRepeatCount() <= 0) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("repeatCount must be positive integer")
                ));
            }
        }

        if (msg.getType().equals("cron")) {
            if (msg.getCron() == null || ( msg.getCron() != null && msg.getCron().isEmpty())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("cron must be set when use cron scheduler")
                ));
            }
            if ( (! msg.getCron().contains("?")) || msg.getCron().split(" ").length != 6) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("cron task must follow format like this : \"0 0/3 17-23 * * ?\" ")
                ));
            }
            if (msg.getInterval() != null || msg.getRepeatCount() != null || msg.getStartTime() != null) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("cron scheduler only need to specify cron task")
                ));
            }
        }
    }

}
