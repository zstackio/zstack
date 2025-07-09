package org.zstack.resourceattribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIMessage;
import org.zstack.header.resourceattribute.AttributeConstant;
import org.zstack.header.resourceattribute.AttributeErrors;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionUtils.*;

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
        } else if (msg instanceof APIUpdateResourceAttributeKeyMsg) {
            validate((APIUpdateResourceAttributeKeyMsg) msg);
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

        if (CollectionUtils.isEmpty(msg.getResourceTypes())) {
            msg.setResourceTypes(transform(RBAC.attributeSupportResources, Class::getSimpleName));
        } else {
            msg.setResourceTypes(checkAttributeResourceTypeOrThrow(msg.getResourceTypes()));
        }

        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
        }
        bus.makeTargetServiceIdByResourceUuid(msg, AttributeConstant.SERVICE_ID, ResourceAttributeKeyVO.class.getName());
    }

    private void validate(APIUpdateResourceAttributeKeyMsg msg) {
        if (!CollectionUtils.isEmpty(msg.getResourceTypes())) {
            msg.setResourceTypes(checkAttributeResourceTypeOrThrow(msg.getResourceTypes()));
        }
    }

    private static List<String> checkAttributeResourceTypeOrThrow(List<String> types) {
        Set<String> supportResourceType = transformToSet(RBAC.attributeSupportResources, Class::getSimpleName);
        final Set<String> requiredTypes = new TreeSet<>(types);
        requiredTypes.removeAll(supportResourceType);

        if (!CollectionUtils.isEmpty(requiredTypes)) {
            throw new ApiMessageInterceptionException(err(AttributeErrors.UNSUPPORTED_RESOURCE_TYPE,
                    "invalid resource type[%s]", requiredTypes));
        }

        // duplicate removal
        requiredTypes.addAll(types);
        return new ArrayList<>(requiredTypes);
    }
}
