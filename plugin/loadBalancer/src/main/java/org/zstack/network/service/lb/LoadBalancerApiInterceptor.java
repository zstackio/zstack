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
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
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
    @Autowired
    private TagManager tagMgr;

    private static final CLogger logger = CLoggerImpl.getLogger(LoadBalancerApiInterceptor.class);

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
        } else if(msg instanceof APIAddCertificateToLoadBalancerListenerMsg){
            validate((APIAddCertificateToLoadBalancerListenerMsg) msg);
        } else if(msg instanceof APIRemoveCertificateFromLoadBalancerListenerMsg){
            validate((APIRemoveCertificateFromLoadBalancerListenerMsg) msg);
        } else if(msg instanceof APIChangeLoadBalancerListenerMsg){
            validate((APIChangeLoadBalancerListenerMsg) msg);
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

        if(useFor != null){
            VipUseForList useForList = new VipUseForList(useFor);
            if(!useForList.validateNewAdded(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)){
                throw new ApiMessageInterceptionException(argerr("the vip[uuid:%s] has been occupied other network service entity[%s]", msg.getVipUuid(), useForList.toString()));
            }
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIAddVmNicToLoadBalancerMsg msg) {
        String sql = "select nic.l3NetworkUuid from VmNicVO nic where nic.uuid in (:uuids) group by nic.l3NetworkUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", msg.getVmNicUuids());
        Set<String> l3Uuids = new HashSet<>(q.getResultList());
        DebugUtils.Assert(!l3Uuids.isEmpty(), "cannot find the l3Network");

        Set<String> networksAttachedLbService = new HashSet<>(Q.New(NetworkServiceL3NetworkRefVO.class)
                .select(NetworkServiceL3NetworkRefVO_.l3NetworkUuid)
                .in(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3Uuids)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
                .listValues());

        l3Uuids.removeAll(networksAttachedLbService);
        if (l3Uuids.size() > 0) {
            throw new ApiMessageInterceptionException(
                    operr("L3 networks[uuids:%s] of the vm nics has no network service[%s] enabled",
                            l3Uuids, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING));
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

        if(LoadBalancerConstants.LB_PROTOCOL_UDP.equals(msg.getProtocol())) {
            insertTagIfNotExisting(
                    msg, LoadBalancerSystemTags.HEALTH_TARGET,
                    LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(
                            map(e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP+":default"))
                    )
            );
        } else {
            insertTagIfNotExisting(
                    msg, LoadBalancerSystemTags.HEALTH_TARGET,
                    LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(
                            map(e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, LoadBalancerGlobalConfig.HEALTH_TARGET.value()))
                    )
            );
        }

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

        /*check the validation of systemtags*/
        for (String tag : msg.getSystemTags()) {
            try {
                tagMgr.validateSystemTag(msg.getResourceUuid(), LoadBalancerListenerVO.class.getSimpleName(), tag);
                /*it'd better add this section code into the validateSystemTag function of MAX_CONNECTION.
                * keep the previous version to work and just add it in API intercepter only
                * */
                if (LoadBalancerSystemTags.MAX_CONNECTION.isMatch(tag)) {
                    String s = LoadBalancerSystemTags.MAX_CONNECTION.getTokenByTag(tag,
                            LoadBalancerSystemTags.MAX_CONNECTION_TOKEN);
                    if (Long.valueOf(s) > LoadBalancerConstants.MAX_CONNECTION_LIMIT) {
                        throw new OperationFailureException(argerr("invalid max connection[%s], %s is larger than upper threshold %d", tag, s, LoadBalancerConstants.MAX_CONNECTION_LIMIT));
                    }
                }
            } catch (OperationFailureException oe) {
                ApiMessageInterceptionException ae = new ApiMessageInterceptionException(oe.getErrorCode());
                ae.initCause(oe);
                throw ae;
            }
        }

        SimpleQuery<LoadBalancerListenerVO> q = dbf.createQuery(LoadBalancerListenerVO.class);
        q.select(LoadBalancerListenerVO_.uuid);
        q.add(LoadBalancerListenerVO_.loadBalancerPort, Op.EQ, msg.getLoadBalancerPort());
        q.add(LoadBalancerListenerVO_.loadBalancerUuid, Op.EQ, msg.getLoadBalancerUuid());
        if (LoadBalancerConstants.LB_PROTOCOL_UDP.equals(msg.getProtocol())) {
            q.add(LoadBalancerListenerVO_.protocol, Op.EQ, LoadBalancerConstants.LB_PROTOCOL_UDP);
        } else {
            q.add(LoadBalancerListenerVO_.protocol, Op.IN, Arrays.asList(LoadBalancerConstants.LB_PROTOCOL_TCP, LoadBalancerConstants.LB_PROTOCOL_HTTP,
                    LoadBalancerConstants.LB_PROTOCOL_HTTPS));
        }
        String luuid = q.findValue();
        if (luuid != null) {
            throw new ApiMessageInterceptionException(argerr("conflict loadBalancerPort[%s], a listener[uuid:%s] has used that port", msg.getLoadBalancerPort(), luuid));
        }

        /* udp port 53 can not be used */
        if (msg.getProtocol().equals(LoadBalancerConstants.LB_PROTOCOL_UDP) && msg.getLoadBalancerPort() == LoadBalancerConstants.DNS_PORT) {
            throw new ApiMessageInterceptionException(argerr("udp port 53 is used by dns daemon"));
        }

        /* tcp port 22, 7272 can not be used on vrouter public vip */
        LoadBalancerVO loadBalancerVO = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, msg.getLoadBalancerUuid()).find();
        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, loadBalancerVO.getVipUuid()).find();
        VipUseForList useForList = new VipUseForList(vipVO.getUseFor());
        boolean isTcpProto = msg.getProtocol().equals(LoadBalancerConstants.LB_PROTOCOL_TCP)
                || msg.getProtocol().equals(LoadBalancerConstants.LB_PROTOCOL_HTTP)
                || msg.getProtocol().equals(LoadBalancerConstants.LB_PROTOCOL_HTTPS);
        boolean isTcpReservePort = msg.getLoadBalancerPort() == LoadBalancerConstants.SSH_PORT
                || msg.getLoadBalancerPort() == LoadBalancerConstants.ZVR_PORT;
        if (isTcpProto && isTcpReservePort && useForList.isIncluded(useForList.SNAT_NETWORK_SERVICE_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("tcp port 22, 7272 is used by vrouter"));
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

    private void validate(APIAddCertificateToLoadBalancerListenerMsg msg) {
        LoadBalancerListenerVO vo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
        if (!vo.getProtocol().equals(LoadBalancerConstants.LB_PROTOCOL_HTTPS)) {
            throw new ApiMessageInterceptionException(argerr("loadbalancer listener with type %s does not need certificate", vo.getProtocol()));
        }

        if (Q.New(LoadBalancerListenerCertificateRefVO.class).eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("loadbalancer listener [uuid:%s] already had certificate",
                    msg.getCertificateUuid(), msg.getListenerUuid()));
        }

        msg.setLoadBalancerUuid(vo.getLoadBalancerUuid());
    }

    private void validate(APIRemoveCertificateFromLoadBalancerListenerMsg msg) {
        if (!Q.New(LoadBalancerListenerCertificateRefVO.class).eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid())
                .eq(LoadBalancerListenerCertificateRefVO_.certificateUuid, msg.getCertificateUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("certificate [uuid:%s] is not added to loadbalancer listener [uuid:%s]",
                    msg.getCertificateUuid(), msg.getListenerUuid()));
        }

        LoadBalancerListenerVO vo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
        msg.setLoadBalancerUuid(vo.getLoadBalancerUuid());
    }

    private void validate(APIChangeLoadBalancerListenerMsg msg) {
        String target = msg.getHealthCheckTarget();
        if (target != null) {
            if (!LoadBalancerConstants.HEALTH_CHECK_TARGET_DEFAULT.equals(target)) {
                try {
                    int port = Integer.valueOf(target);
                    if (port < 1 || port > 65535) {
                        throw new ApiMessageInterceptionException(argerr("healthCheck target [%s] error, it must be 'default' or number between[1~65535] ",
                                target));
                    }
                } catch (Exception e) {
                    throw new ApiMessageInterceptionException(argerr("healthCheck target [%s] error, it must be 'default' or number between[1~65535] ",
                            target));
                }
            }
        }

        String loadBalancerUuid = Q.New(LoadBalancerListenerVO.class).
                select(LoadBalancerListenerVO_.loadBalancerUuid).
                eq(LoadBalancerListenerVO_.uuid,msg.getLoadBalancerListenerUuid()).findValue();
        msg.setLoadBalancerUuid(loadBalancerUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, loadBalancerUuid);
    }
}
