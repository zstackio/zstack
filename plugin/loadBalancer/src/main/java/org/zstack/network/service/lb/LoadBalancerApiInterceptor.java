package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.tag.PatternedSystemTag;

import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 8/12/2015.
 */
public class LoadBalancerApiInterceptor implements ApiMessageInterceptor {
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
        }

        return msg;
    }

    private void validate(APIRemoveVmNicFromLoadBalancerMsg msg) {
        SimpleQuery<LoadBalancerVmNicRefVO> q = dbf.createQuery(LoadBalancerVmNicRefVO.class);
        q.add(LoadBalancerVmNicRefVO_.vmNicUuid, Op.EQ, msg.getVmNicUuid());
        q.add(LoadBalancerVmNicRefVO_.loadBalancerUuid, Op.EQ, msg.getLoadBalancerUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the vm nic[uuid:%s] is not on the load balancer[uuid:%s]", msg.getVmNicUuid(), msg.getLoadBalancerUuid())
            ));
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

            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the vip[uuid:%s] is occupied by another load balancer[uuid:%s]", msg.getVipUuid(), lbuuid)
            ));
        }

        if (useFor != null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the vip[uuid:%s] is occupied by another service[%s]", msg.getVipUuid(), useFor)
            ));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIAddVmNicToLoadBalancerMsg msg) {
        String sql = "select nic.uuid from NetworkServiceL3NetworkRefVO ref, VmNicVO nic where nic.l3NetworkUuid = ref.l3NetworkUuid" +
                " and ref.networkServiceType = :ntype and nic.uuid = :nicUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuid", msg.getVmNicUuid());
        q.setParameter("ntype", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        if (q.getResultList().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("the L3 network of the vm nic[uuid:%s] has no network service[%s] enabled", msg.getVmNicUuid(), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
            ));
        }

        sql = "select ref.vmNicUuid from LoadBalancerVmNicRefVO ref where ref.vmNicUuid = :nicUuid and ref.loadBalancerUuid = :lbuuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuid", msg.getVmNicUuid());
        q.setParameter("lbuuid", msg.getLoadBalancerUuid());
        if (!q.getResultList().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("the vm nic[uuid:%s] is already on the load balancer[uuid:%s]", msg.getVmNicUuid(), msg.getLoadBalancerUuid())
            ));
        }
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
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("conflict loadBalancerPort[%s], a listener[uuid:%s] has used that port", msg.getLoadBalancerPort(), luuid)
            ));
        }

        q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.uuid);
        q.add(LoadBalancerListenerVO_.instancePort, Op.EQ, msg.getInstancePort());
        q.add(LoadBalancerListenerVO_.loadBalancerUuid, Op.EQ, msg.getLoadBalancerUuid());
        luuid = q.findValue();
        if (luuid != null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("conflict instancePort[%s], a listener[uuid:%s] has used that port", msg.getInstancePort(), luuid)
            ));
        }
    }

    private void validate(APIDeleteLoadBalancerListenerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.loadBalancerUuid);
        q.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getListenerUuid());
        String lbUuid = q.findValue();
        if (lbUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find load balancer uuid of LoadBalancerListenerVO[uuid:%s]", msg.getListenerUuid()));
        }

        msg.setLoadBalancerUuid(lbUuid);
    }
}
