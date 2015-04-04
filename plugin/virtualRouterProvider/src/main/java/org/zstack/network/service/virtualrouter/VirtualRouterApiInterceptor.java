package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;

/**
 */
public class VirtualRouterApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIQueryVirtualRouterOfferingMsg) {
            validate((APIQueryVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APICreateVirtualRouterOfferingMsg) {
            validate((APICreateVirtualRouterOfferingMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateVirtualRouterOfferingMsg msg) {
        if (msg.getPublicNetworkUuid() == null) {
            msg.setPublicNetworkUuid(msg.getManagementNetworkUuid());
        }

        SimpleQuery<L3NetworkVO> q = dbf.createQuery(L3NetworkVO.class);
        q.select(L3NetworkVO_.zoneUuid);
        q.add(L3NetworkVO_.uuid, Op.EQ, msg.getManagementNetworkUuid());
        String zoneUuid = q.findValue();
        if (!zoneUuid.equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("management network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid())
            ));
        }

        q = dbf.createQuery(L3NetworkVO.class);
        q.select(L3NetworkVO_.zoneUuid);
        q.add(L3NetworkVO_.uuid, Op.EQ, msg.getPublicNetworkUuid());
        zoneUuid = q.findValue();
        if (!zoneUuid.equals(msg.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("public network[uuid:%s] is not in the same zone[uuid:%s] this offering is going to create",
                            msg.getManagementNetworkUuid(), msg.getZoneUuid())
            ));
        }

        SimpleQuery<ImageVO> imq = dbf.createQuery(ImageVO.class);
        imq.select(ImageVO_.mediaType);
        imq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        ImageMediaType type = imq.findValue();
        if (type != ImageMediaType.RootVolumeTemplate) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("image[uuid:%s] is not a RootVolumeTemplate, it's %s", msg.getImageUuid(), type)
            ));
        }
    }

    private void validate(APIQueryVirtualRouterOfferingMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
        }
    }
}
