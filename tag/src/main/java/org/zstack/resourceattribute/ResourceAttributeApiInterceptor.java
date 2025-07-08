package org.zstack.resourceattribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.resourceattribute.AttributeConstant;
import org.zstack.header.resourceattribute.AttributeErrors;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;

import static org.zstack.core.Platform.err;

public class ResourceAttributeApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ResourceAttributeMessage) {
            ResourceAttributeMessage castMessage = (ResourceAttributeMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AttributeConstant.SERVICE_ID, castMessage.getKeyUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateResourceAttributeKeyMsg) {
            validate((APICreateResourceAttributeKeyMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APICreateResourceAttributeKeyMsg msg) {
        boolean duplicateName = Q.New(ResourceAttributeKeyVO.class)
                .eq(ResourceAttributeKeyVO_.name, msg.getName())
                .isExists();
        if (duplicateName) {
            throw new ApiMessageInterceptionException(err(AttributeErrors.DUPLICATED_ATTRIBUTE,
                    "duplicate resource attribute key name[%s]", msg.getName()));
        }

        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
        }
        bus.makeTargetServiceIdByResourceUuid(msg, AttributeConstant.SERVICE_ID, ResourceAttributeKeyVO.class.getName());
    }
}
