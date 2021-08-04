package org.zstack.network.l2;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2NetworkApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceConfigFacade rcf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof L2NetworkMessage) {
            L2NetworkMessage l2msg = (L2NetworkMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l2msg.getL2NetworkUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateL2NetworkMsg) {
            validate((APICreateL2NetworkMsg)msg);
        } else if (msg instanceof APIDeleteL2NetworkMsg) {
            validate((APIDeleteL2NetworkMsg)msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            validate((APIDetachL2NetworkFromClusterMsg)msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(final APIAttachL2NetworkToClusterMsg msg) {
        SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
        q.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has attached to cluster[uuid:%s], can't attach again", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        }

        SimpleQuery<L2NetworkVO> q2 = dbf.createQuery(L2NetworkVO.class);
        q2.add(L2NetworkVO_.uuid, Op.EQ, msg.getL2NetworkUuid());
        q2.add(L2NetworkVO_.vSwitchType, Op.EQ, "OvsDpdk");
        boolean isOvsDpdk = q2.isExists();

        ResourceConfig ovsDpdkSup = rcf.getResourceConfig("premiumCluster.network.ovsdpdk");
        if (ovsDpdkSup != null) {
            boolean isOvsDpdkSup = ovsDpdkSup.getResourceConfigValue(msg.getClusterUuid(), Boolean.class);
            if (isOvsDpdk && !isOvsDpdkSup) {
                throw new ApiMessageInterceptionException(operr("cluster[uuid:%s] do not support ovsdpdk", msg.getClusterUuid()));
            }
        } else {
            if (isOvsDpdk) {
                throw new ApiMessageInterceptionException(operr("cluster[uuid:%s] do not support ovsdpdk", msg.getClusterUuid()));
            }
        }
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
        SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
        q.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has not attached to cluster[uuid:%s]", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        }
    }

    private void validate(APIDeleteL2NetworkMsg msg) {
        if (!dbf.isExist(msg.getUuid(), L2NetworkVO.class)) {
            APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APICreateL2NetworkMsg msg) {
        if (!L2NetworkType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported l2Network type[%s]", msg.getType()));
        }
        // once we already created a linux bridge with a physical interface,
        // we can not use it to create a OvsDpdk bridge
        SimpleQuery<L2NetworkVO> q = dbf.createQuery(L2NetworkVO.class);
        q.add(L2NetworkVO_.physicalInterface, Op.EQ, msg.getPhysicalInterface());
        q.add(L2NetworkVO_.vSwitchType, Op.NOT_EQ, msg.getvSwitchType());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("can not create %s L2Network with physicalInterface:[%s] which was already been used by another vSwitchType.", msg.getvSwitchType(), msg.getPhysicalInterface()));
        }
    }
}
