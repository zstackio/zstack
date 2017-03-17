package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.configuration.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategyType;
import org.zstack.utils.data.SizeUnit;
import static org.zstack.core.Platform.argerr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof InstanceOfferingMessage) {
            InstanceOfferingMessage imsg = (InstanceOfferingMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, imsg.getInstanceOfferingUuid());
        } else if (msg instanceof DiskOfferingMessage) {
            DiskOfferingMessage dmsg = (DiskOfferingMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, dmsg.getDiskOfferingUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateDiskOfferingMsg) {
            validate((APICreateDiskOfferingMsg) msg);
        } else if (msg instanceof APICreateInstanceOfferingMsg) {
            validate((APICreateInstanceOfferingMsg) msg);
        } else if (msg instanceof APIDeleteDiskOfferingMsg) {
            validate((APIDeleteDiskOfferingMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIDeleteDiskOfferingMsg msg) {
        if (!dbf.isExist(msg.getUuid(), DiskOfferingVO.class)) {
            APIDeleteDiskOfferingEvent evt = new APIDeleteDiskOfferingEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APICreateInstanceOfferingMsg msg) {
        if (msg.getAllocatorStrategy() != null && !HostAllocatorStrategyType.hasType(msg.getAllocatorStrategy())) {
            throw new ApiMessageInterceptionException(argerr("unsupported host allocation strategy[%s]", msg.getAllocatorStrategy()));
        }

        if (msg.getType() != null && !InstanceOfferingType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported instance offering type[%s]", msg.getType()));
        }

        if (msg.getCpuNum() < 1) {
            throw new ApiMessageInterceptionException(argerr("cpu num[%s] is less than 1", msg.getCpuNum()));
        }

        if (msg.getMemorySize() < SizeUnit.MEGABYTE.toByte(16)) {
            throw new ApiMessageInterceptionException(argerr("memory size[%s bytes] is less than 16M, no modern operating system is likely able to boot with such small memory size", msg.getMemorySize()));
        }
    }

    private void validate(APICreateDiskOfferingMsg msg) {
        if (msg.getAllocationStrategy() != null && !PrimaryStorageAllocatorStrategyType.hasType(msg.getAllocationStrategy())) {
            throw new ApiMessageInterceptionException(argerr("unsupported primary storage allocation strategy[%s]", msg.getAllocationStrategy()));
        }
    }
}
