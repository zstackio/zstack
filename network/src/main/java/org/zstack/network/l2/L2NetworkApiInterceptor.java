package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;

import javax.persistence.TypedQuery;
import java.util.List;

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
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("l2Network[uuid:%s] has attached to cluster[uuid:%s], can't attach again", msg.getL2NetworkUuid(), msg.getClusterUuid())
            ));
        }

        SimpleQuery<L2NetworkVO> l2q = dbf.createQuery(L2NetworkVO.class);
        l2q.select(L2NetworkVO_.type);
        l2q.add(L2NetworkVO_.uuid, Op.EQ, msg.getL2NetworkUuid());
        String type = l2q.findValue();

        if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(type)) {
            new Runnable() {
                @Override
                @Transactional(readOnly = true)
                public void run() {
                    String sql = "select l2 from L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid and type = 'L2NoVlanNetwork'";
                    TypedQuery<L2NetworkVO> q = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
                    q.setParameter("clusterUuid", msg.getClusterUuid());
                    List<L2NetworkVO> l2s = q.getResultList();
                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2NetworkVO tl2 = dbf.getEntityManager().find(L2NetworkVO.class, msg.getL2NetworkUuid());
                    for (L2NetworkVO l2 : l2s) {
                        if (l2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                                    String.format("There has been a l2Network[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s]. Failed to attach l2Network[uuid:%s]",
                                            l2.getUuid(), l2.getName(), msg.getClusterUuid(), l2.getPhysicalInterface(), tl2.getUuid())
                            ));
                        }
                    }
                }
            }.run();
        } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(type)) {
            new Runnable() {
                @Override
                @Transactional(readOnly = true)
                public void run() {
                    String sql = "select l2 from L2VlanNetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid";
                    TypedQuery<L2VlanNetworkVO> q = dbf.getEntityManager().createQuery(sql, L2VlanNetworkVO.class);
                    q.setParameter("clusterUuid", msg.getClusterUuid());
                    List<L2VlanNetworkVO> l2s = q.getResultList();
                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2VlanNetworkVO tl2 = dbf.getEntityManager().find(L2VlanNetworkVO.class, msg.getL2NetworkUuid());

                    for (L2VlanNetworkVO vl2 : l2s) {
                        if (vl2.getVlan() == tl2.getVlan() && vl2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                                    String.format("There has been a L2VlanNetwork[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s], vlan[%s]. Failed to attach L2VlanNetwork[uuid:%s]",
                                            vl2.getUuid(), vl2.getName(), msg.getClusterUuid(), vl2.getPhysicalInterface(), vl2.getVlan(), tl2.getUuid())
                            ));
                        }
                    }
                }
            }.run();
        }
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
        SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
        q.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("l2Network[uuid:%s] has not attached to cluster[uuid:%s]", msg.getL2NetworkUuid(), msg.getClusterUuid())
            ));
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
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("unsupported l2Network type[%s]", msg.getType())
            ));
        }
    }
}
