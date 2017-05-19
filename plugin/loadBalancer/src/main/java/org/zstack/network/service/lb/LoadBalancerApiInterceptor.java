package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.DebugUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 8/12/2015.
 */
public class LoadBalancerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteLoadBalancerListenerMsg) {
            validate((APIDeleteLoadBalancerListenerMsg)msg);
        } else if (msg instanceof APICreateLoadBalancerListenerMsg) {
            validate((APICreateLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddVmNicToLoadBalancerMsg) {
            validate((APIAddVmNicToLoadBalancerMsg) msg);
        } else if (msg instanceof APICreateLoadBalancerMsg) {
            validate((APICreateLoadBalancerMsg) msg);
        } else if (msg instanceof APIRemoveVmNicFromLoadBalancerMsg) {
            validate((APIRemoveVmNicFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetCandidateVmNicsForLoadBalancerMsg) {
            validate((APIGetCandidateVmNicsForLoadBalancerMsg) msg);
        } else if(msg instanceof APIUpdateLoadBalancerListenerMsg){
            validate((APIUpdateLoadBalancerListenerMsg) msg);
        }
        return msg;
    }

    private void validate(APIGetCandidateVmNicsForLoadBalancerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> lq = dbf.createQuery(LoadBalancerListenerVO.class);
        lq.select(LoadBalancerListenerVO_.loadBalancerUuid);
        lq.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getListenerUuid());
        String lbuuid = lq.findValue();
        msg.setLoadBalancerUuid(lbuuid);
    }

    private void validate(APIRemoveVmNicFromLoadBalancerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> lq = dbf.createQuery(LoadBalancerListenerVO.class);
        lq.select(LoadBalancerListenerVO_.loadBalancerUuid);
        lq.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getListenerUuid());
        String lbuuid = lq.findValue();
        msg.setLoadBalancerUuid(lbuuid);

        SimpleQuery<LoadBalancerListenerVmNicRefVO> q = dbf.createQuery(LoadBalancerListenerVmNicRefVO.class);
        q.select(LoadBalancerListenerVmNicRefVO_.vmNicUuid);
        q.add(LoadBalancerListenerVmNicRefVO_.vmNicUuid, Op.IN, msg.getVmNicUuids());
        q.add(LoadBalancerListenerVmNicRefVO_.listenerUuid, Op.EQ, msg.getListenerUuid());
        List<String> vmNicUuids = q.listValue();
        if (vmNicUuids.isEmpty()) {
            APIRemoveVmNicFromLoadBalancerEvent evt = new APIRemoveVmNicFromLoadBalancerEvent(msg.getId());
            evt.setInventory(LoadBalancerInventory.valueOf(dbf.findByUuid(lbuuid, LoadBalancerVO.class)));
            throw new StopRoutingException();
        }
    }

    private void validate(APICreateLoadBalancerMsg msg) {
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.select(VipVO_.useFor);
        q.add(VipVO_.uuid, Op.EQ, msg.getVipUuid());
        String useFor = q.findValue();
        if (LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING.equals(useFor)) {
            SimpleQuery<LoadBalancerVO> lq = dbf.createQuery(LoadBalancerVO.class);
            lq.select(LoadBalancerVO_.uuid);
            lq.add(LoadBalancerVO_.vipUuid, Op.EQ, msg.getVipUuid());
            String lbuuid = lq.findValue();

            throw new ApiMessageInterceptionException(argerr("the vip[uuid:%s] is occupied by another load balancer[uuid:%s]", msg.getVipUuid(), lbuuid));
        }

        if (useFor != null) {
            throw new ApiMessageInterceptionException(argerr("the vip[uuid:%s] is occupied by another service[%s]", msg.getVipUuid(), useFor));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIAddVmNicToLoadBalancerMsg msg) {
        String sql = "select nic.l3NetworkUuid from VmNicVO nic where nic.uuid in (:uuids) group by nic.l3NetworkUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", msg.getVmNicUuids());
        List<String> l3Uuids = q.getResultList();
        DebugUtils.Assert(!l3Uuids.isEmpty(), "cannot find the l3Network");
        if (l3Uuids.size() > 1) {
            throw new ApiMessageInterceptionException(argerr("vm nics[uuids:%s] are not on the same L3 network. they are on L3 networks[uuids:%s]", msg.getVmNicUuids(), l3Uuids));
        }

        String l3Uuid = l3Uuids.get(0);
        sql = "select ref.l3NetworkUuid from NetworkServiceL3NetworkRefVO ref where ref.l3NetworkUuid = :uuid and ref.networkServiceType = :ntype";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", l3Uuid);
        q.setParameter("ntype", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        if (q.getResultList().isEmpty()) {
            throw new ApiMessageInterceptionException(operr("the L3 network[uuid:%s] of the vm nics has no network service[%s] enabled", l3Uuid, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING));
        }

        sql = "select ref.vmNicUuid from LoadBalancerListenerVmNicRefVO ref where ref.vmNicUuid in (:nicUuids) and ref.listenerUuid = :uuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", msg.getVmNicUuids());
        q.setParameter("uuid", msg.getListenerUuid());
        List<String> existingNics = q.getResultList();
        if (!existingNics.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("the vm nics[uuid:%s] are already on the load balancer listener[uuid:%s]", existingNics, msg.getListenerUuid()));
        }

        sql = "select l.loadBalancerUuid from LoadBalancerListenerVO l where l.uuid = :uuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", msg.getListenerUuid());
        msg.setLoadBalancerUuid(q.getSingleResult());
    }

    private boolean hasTag(APICreateMessage msg, PatternedSystemTag tag) {
        if (msg.getSystemTags() == null) {
            return false;
        }

        for (String t : msg.getSystemTags()) {
            if (tag.isMatch(t)) {
                return true;
            }
        }
        return false;
    }

    private void insertTagIfNotExisting(APICreateMessage msg, PatternedSystemTag tag, String value) {
        if (!hasTag(msg, tag)) {
            msg.addSystemTag(value);
        }
    }

    private void validate(APICreateLoadBalancerListenerMsg msg) {
        if (msg.getInstancePort() == null) {
            msg.setInstancePort(msg.getLoadBalancerPort());
        }
        if (msg.getProtocol() == null) {
            msg.setProtocol(LoadBalancerConstants.LB_PROTOCOL_TCP);
        }

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT,
                LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.instantiateTag(
                        map(e(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN, LoadBalancerGlobalConfig.CONNECTION_IDLE_TIMEOUT.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.HEALTHY_THRESHOLD,
                LoadBalancerSystemTags.HEALTHY_THRESHOLD.instantiateTag(
                        map(e(LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN, LoadBalancerGlobalConfig.HEALTHY_THRESHOLD.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.HEALTH_INTERVAL,
                LoadBalancerSystemTags.HEALTH_INTERVAL.instantiateTag(
                        map(e(LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN, LoadBalancerGlobalConfig.HEALTH_INTERVAL.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.HEALTH_TARGET,
                LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(
                        map(e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, LoadBalancerGlobalConfig.HEALTH_TARGET.value()))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.HEALTH_TIMEOUT,
                LoadBalancerSystemTags.HEALTH_TIMEOUT.instantiateTag(
                        map(e(LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN, LoadBalancerGlobalConfig.HEALTH_TIMEOUT.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.UNHEALTHY_THRESHOLD,
                LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.instantiateTag(
                        map(e(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, LoadBalancerGlobalConfig.UNHEALTHY_THRESHOLD.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.MAX_CONNECTION,
                LoadBalancerSystemTags.MAX_CONNECTION.instantiateTag(
                        map(e(LoadBalancerSystemTags.MAX_CONNECTION_TOKEN, LoadBalancerGlobalConfig.MAX_CONNECTION.value(Long.class)))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.BALANCER_ALGORITHM,
                LoadBalancerSystemTags.BALANCER_ALGORITHM.instantiateTag(
                        map(e(LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN, LoadBalancerGlobalConfig.BALANCER_ALGORITHM.value()))
                )
        );

        SimpleQuery<LoadBalancerListenerVO> q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.uuid);
        q.add(LoadBalancerListenerVO_.loadBalancerPort, Op.EQ, msg.getLoadBalancerPort());
        q.add(LoadBalancerListenerVO_.loadBalancerUuid, Op.EQ, msg.getLoadBalancerUuid());
        String luuid = q.findValue();
        if (luuid != null) {
            throw new ApiMessageInterceptionException(argerr("conflict loadBalancerPort[%s], a listener[uuid:%s] has used that port", msg.getLoadBalancerPort(), luuid));
        }

        q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.uuid);
        q.add(LoadBalancerListenerVO_.instancePort, Op.EQ, msg.getInstancePort());
        q.add(LoadBalancerListenerVO_.loadBalancerUuid, Op.EQ, msg.getLoadBalancerUuid());
        luuid = q.findValue();
        if (luuid != null) {
            throw new ApiMessageInterceptionException(argerr("conflict instancePort[%s], a listener[uuid:%s] has used that port", msg.getInstancePort(), luuid));
        }
    }

    private void validate(APIDeleteLoadBalancerListenerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.loadBalancerUuid);
        q.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getUuid());
        String lbUuid = q.findValue();
        if (lbUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find load balancer uuid of LoadBalancerListenerVO[uuid:%s]", msg.getUuid()));
        }

        msg.setLoadBalancerUuid(lbUuid);
    }
    private void validate(APIUpdateLoadBalancerListenerMsg msg) {
        String loadBalancerUuid = Q.New(LoadBalancerListenerVO.class).
                select(LoadBalancerListenerVO_.loadBalancerUuid).
                eq(LoadBalancerListenerVO_.uuid,msg.
                        getLoadBalancerListenerUuid()).findValue();
        msg.setLoadBalancerUuid(loadBalancerUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, loadBalancerUuid);
    }
}
