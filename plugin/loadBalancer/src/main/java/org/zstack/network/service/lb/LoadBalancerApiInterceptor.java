package org.zstack.network.service.lb;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.acl.*;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.AddressPoolVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.network.service.vip.VipNetworkServicesRefVO;
import org.zstack.network.service.vip.VipNetworkServicesRefVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 8/12/2015.
 */
public class LoadBalancerApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private static final CLogger logger = CLoggerImpl.getLogger(LoadBalancerApiInterceptor.class);
    private static String SPLIT = "-";
    private static String IP_SPLIT = ",";

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIDeleteAccessControlListMsg.class);
        ret.add(APIAddAccessControlListEntryMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

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
        } else if (msg instanceof APIGetCandidateL3NetworksForLoadBalancerMsg) {
            validate((APIGetCandidateL3NetworksForLoadBalancerMsg) msg);
        } else if(msg instanceof APIUpdateLoadBalancerListenerMsg){
            validate((APIUpdateLoadBalancerListenerMsg) msg);
        } else if(msg instanceof APIAddCertificateToLoadBalancerListenerMsg){
            validate((APIAddCertificateToLoadBalancerListenerMsg) msg);
        } else if(msg instanceof APIRemoveCertificateFromLoadBalancerListenerMsg){
            validate((APIRemoveCertificateFromLoadBalancerListenerMsg) msg);
        } else if(msg instanceof APIChangeLoadBalancerListenerMsg){
            validate((APIChangeLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddAccessControlListToLoadBalancerMsg) {
            validate((APIAddAccessControlListToLoadBalancerMsg) msg);
        } else if (msg instanceof APIRemoveAccessControlListFromLoadBalancerMsg) {
            validate((APIRemoveAccessControlListFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIAddAccessControlListEntryMsg) {
            validate((APIAddAccessControlListEntryMsg) msg);
        } else if (msg instanceof APIDeleteAccessControlListMsg) {
            validate((APIDeleteAccessControlListMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteAccessControlListMsg msg) {
        List<String> refs = Q.New(LoadBalancerListenerACLRefVO.class).select(LoadBalancerListenerVmNicRefVO_.listenerUuid)
                             .eq(LoadBalancerListenerACLRefVO_.aclUuid, msg.getUuid()).listValues();
        if ( !refs.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("the access control list group[%s] is being used by the load balancer listeners[%s]", msg.getUuid(), refs));
        }
    }

    private void validate(APIGetCandidateVmNicsForLoadBalancerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> lq = dbf.createQuery(LoadBalancerListenerVO.class);
        lq.select(LoadBalancerListenerVO_.loadBalancerUuid);
        lq.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getListenerUuid());
        String lbuuid = lq.findValue();
        msg.setLoadBalancerUuid(lbuuid);
    }

    private void validate(APIGetCandidateL3NetworksForLoadBalancerMsg msg) {
        SimpleQuery<LoadBalancerListenerVO> lq = dbf.createQuery(LoadBalancerListenerVO.class);
        lq.select(LoadBalancerListenerVO_.loadBalancerUuid);
        lq.add(LoadBalancerListenerVO_.uuid, Op.EQ, msg.getListenerUuid());
        String lbuuid = lq.findValue();
        msg.setLoadBalancerUuid(lbuuid);
    }

    private void validate(APIRemoveAccessControlListFromLoadBalancerMsg msg) {
        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, msg.getListenerUuid()).find();
        msg.setLoadBalancerUuid(vo.getLoadBalancerUuid());

        List<String> existingAcls = Q.New(LoadBalancerListenerACLRefVO.class).select(LoadBalancerListenerACLRefVO_.aclUuid)
                                     .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                                     .listValues();
        if (existingAcls.isEmpty()) {
            APIRemoveAccessControlListFromLoadBalancerEvent evt = new APIRemoveAccessControlListFromLoadBalancerEvent(msg.getId());
            evt.setInventory(LoadBalancerListenerInventory.valueOf(vo));
            bus.publish(evt);
            throw new StopRoutingException();
        }
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
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APICreateLoadBalancerMsg msg) {
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, msg.getVipUuid()).listValues();
        if(useFor != null && !useFor.isEmpty()){
            VipUseForList useForList = new VipUseForList(useFor);
            if(!useForList.validateNewAdded(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)){
                throw new ApiMessageInterceptionException(argerr("the vip[uuid:%s] has been occupied other network service entity[%s]", msg.getVipUuid(), useForList.toString()));
            }
        }

        /* the vip can not the first of the last ip of the cidr */
        VipVO vipVO = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        if (NetworkUtils.isIpv4Address(vipVO.getIp())) {
            AddressPoolVO addressPoolVO = dbf.findByUuid(vipVO.getIpRangeUuid(), AddressPoolVO.class);
            if (addressPoolVO == null) {
                return;
            }

            SubnetUtils utils = new SubnetUtils(addressPoolVO.getNetworkCidr());
            SubnetUtils.SubnetInfo subnet = utils.getInfo();
            String firstIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getLowAddress()) - 1);
            String lastIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getHighAddress()) + 1);
            if (vipVO.getIp().equals(firstIp) || vipVO.getIp().equals(lastIp)) {
                throw new ApiMessageInterceptionException(argerr("Load balancer VIP [%s] cannot be the first or the last IP of the CIDR with the public address pool type", vipVO.getIp()));
            }
        }

    }

    private boolean validateIpRange(String startIp, String endIp) {
        if (NetworkUtils.isIpv4Address(startIp) && !NetworkUtils.isIpv4Address(endIp)) {
            return false;
        }

        if (IPv6NetworkUtils.isIpv6Address(startIp) && !IPv6NetworkUtils.isIpv6Address(endIp)) {
            return false;
        }

        try {
            if (NetworkUtils.isIpv4Address(startIp)) {
                NetworkUtils.validateIpRange(startIp, endIp);
            } else {
                //IPv6NetworkUtils.validateIpRange(startIp, endIp);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateIp(String ips, AccessControlListVO acl) {
        DebugUtils.Assert(acl != null, "the invalide null AccessControlListVO");
        Integer ipVer = acl.getIpVersion();
        if (!ipVer.equals(IPv6Constants.IPv4)) {
            throw new ApiMessageInterceptionException(argerr("operation failure, not support the ip version %d", ipVer));
        }
        try {
            RangeSet<Long> ipRanges = IpRangeSet.listAllRanges(ips);
            String[] ipcount = ips.split(IP_SPLIT);
            if (ipRanges.asRanges().size() < ipcount.length) {
                throw new ApiMessageInterceptionException(argerr("operation failure, duplicate/overlap ip entry in %s of accesscontrol list group:%s", ips, acl.getUuid()));
            }
            for (Range<Long> range : ipRanges.asRanges()) {
                final Range<Long> frange = ContiguousSet.create(range, DiscreteDomain.longs()).range();
                String startIp = NetworkUtils.longToIpv4String(frange.lowerEndpoint());
                String endIp = NetworkUtils.longToIpv4String(frange.upperEndpoint());
                if (!validateIpRange(startIp, endIp)) {
                    throw new ApiMessageInterceptionException(argerr("operation failure, ip format only supports ip/iprange/cidr, but find %s", ips));
                }
                ipRanges.asRanges().stream().forEach(r -> {
                    if (!frange.equals(r) && NetworkUtils.isIpv4RangeOverlap(startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint()))) {
                        throw new ApiMessageInterceptionException(argerr("ip range[%s, %s] is overlap with start ip:%s, end ip: %s of access-control-list group:%s",
                                startIp, endIp, NetworkUtils.longToIpv4String(r.lowerEndpoint()), NetworkUtils.longToIpv4String(r.upperEndpoint()), acl.getUuid()));
                    }
                });
            }

        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("Invalid rule expression, the detail: %s", e.getMessage()));
        }

    }

    private void validateAcl(List<String> newAclUuids, List<String> oriAclUuids, String lbUuid) {
        LoadBalancerVO lb = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, lbUuid).find();
        VipVO vip = Q.New(VipVO.class).eq(VipVO_.uuid, lb.getVipUuid()).find();

        List<AccessControlListVO> acls = Q.New(AccessControlListVO.class)
                                          .in(AccessControlListVO_.uuid, newAclUuids).list();
        if (!acls.isEmpty()) {
            /*check if the ip version is same*/
            List<String> aclUuids = acls.stream().filter(acl -> acl.getIpVersion() != NetworkUtils.getIpversion(vip.getIp())).map(AccessControlListVO::getUuid).collect(Collectors.toList());
            if (!aclUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("Can't attach the type access-control-list group[%s] whose ip version is different with LoadBalancer[%s]", aclUuids, lbUuid));
            }

            List<AccessControlListVO> allAcl = acls;
            if ( !oriAclUuids.isEmpty()) {
                List<AccessControlListVO> attached = Q.New(AccessControlListVO.class).in(AccessControlListVO_.uuid, oriAclUuids).list();
                allAcl.addAll(attached);
            }
            /*check all the ip entry not overlap include with each other*/
            for (AccessControlListVO acl : acls) {
                if (acl.getEntries().isEmpty()) {
                    continue;
                }
                List<String> ipentries = acl.getEntries().stream().map(AccessControlListEntryVO::getIpEntries).collect(Collectors.toList());
                for (AccessControlListVO acl2 : allAcl) {
                    if ( acl.getUuid().equals(acl2.getUuid())) {
                        continue;
                    }
                    if (acl2.getEntries().isEmpty()) {
                        continue;
                    }
                    List<String> ipentries2 = acl2.getEntries().stream().map(AccessControlListEntryVO::getIpEntries).collect(Collectors.toList());
                    ipentries2.addAll(ipentries);
                    validateIp(StringUtils.join(ipentries2.toArray(), ','), acl2);
                }
            }
        }
    }

    private void validate(APIAddAccessControlListEntryMsg msg) {
        List<String> listenerUuids = Q.New(LoadBalancerListenerACLRefVO.class).select(LoadBalancerListenerACLRefVO_.listenerUuid)
                                    .eq(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuid()).listValues();
        if (listenerUuids.isEmpty()) {
            return;
        }

        List<String> aclUuids = Q.New(LoadBalancerListenerACLRefVO.class).select(LoadBalancerListenerACLRefVO_.aclUuid)
                                 .in(LoadBalancerListenerACLRefVO_.listenerUuid, listenerUuids).listValues();
        if (aclUuids.isEmpty()) {
            return;
        }
        List<AccessControlListVO> acls = Q.New(AccessControlListVO.class).in(AccessControlListVO_.uuid, aclUuids).list();

        for (AccessControlListVO acl : acls) {
            if (acl.getEntries().isEmpty()) {
                continue;
            }
            List<String> ipentries = acl.getEntries().stream().map(AccessControlListEntryVO::getIpEntries).collect(Collectors.toList());
            ipentries.add(msg.getEntries());
            validateIp(StringUtils.join(ipentries.toArray(), ','), acl);
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIAddAccessControlListToLoadBalancerMsg msg) {
        List<LoadBalancerListenerACLRefVO> refVOs = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();
        if ( !refVOs.isEmpty()) {
            /*check if duplicated*/
            List<String> existingAcls = refVOs.stream().filter(vo -> msg.getAclUuids().contains(vo.getAclUuid())).map(vo -> vo.getAclUuid()).collect(Collectors.toList());
            if (!existingAcls.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("the access-control-list groups[uuid:%s] are already on the load balancer listener[uuid:%s]", existingAcls, msg.getListenerUuid()));
            }

            /*check if type is same*/
            LoadBalancerAclType type = refVOs.get(0).getType();
            if (!type.equals(LoadBalancerAclType.valueOf(msg.getAclType()))) {
                throw new ApiMessageInterceptionException(argerr("the load balancer listener[uuid:%s] just only attach the %s type access-control-list group", msg.getListenerUuid(), type.toString()));
            }
        }

        if (msg.getAclUuids().size() + refVOs.size() > LoadBalancerGlobalConfig.ACL_MAX_COUNT.value(Long.class)) {
            throw new ApiMessageInterceptionException(argerr("the load balancer listener[uuid:%s] can't  attach more than %d access-control-list groups", msg.getListenerUuid(), LoadBalancerGlobalConfig.ACL_MAX_COUNT.value(Long.class)));
        }

        String lbUuid = Q.New(LoadBalancerListenerVO.class).select(LoadBalancerListenerVO_.loadBalancerUuid).eq(LoadBalancerListenerVO_.uuid, msg.getListenerUuid()).findValue();
        msg.setLoadBalancerUuid(lbUuid);

        validateAcl(msg.getAclUuids(), refVOs.stream().map(LoadBalancerListenerACLRefVO::getAclUuid).collect(Collectors.toList()), lbUuid );
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

        if (LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN.equals(
                LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByResourceUuid(
                        msg.getListenerUuid(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN))) {
            Map<String, Long> weight = new LoadBalancerWeightOperator().getWeight(msg.getSystemTags());
            CollectionUtils.forEach(msg.getVmNicUuids(), new ForEachFunction<String>() {
                @Override
                public void run(String arg) {
                    if (!weight.containsKey(arg)) {
                        msg.addSystemTag(LoadBalancerSystemTags.BALANCER_WEIGHT.instantiateTag(
                                map(e(LoadBalancerSystemTags.BALANCER_NIC_TOKEN, arg),
                                        e(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN, LoadBalancerConstants.BALANCER_WEIGHT_default)))
                        );
                    }
                }
            });
        }

        sql = "select l.loadBalancerUuid from LoadBalancerListenerVO l where l.uuid = :uuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", msg.getListenerUuid());
        msg.setLoadBalancerUuid(q.getSingleResult());
    }

    private boolean hasTag(APIMessage msg, PatternedSystemTag tag) {
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

    private Boolean verifyHttpCode(String httpCode) {
        List<String> codes = Arrays.asList(httpCode.split(","));
        return codes.stream().allMatch(code -> {
            try {
                LoadBalancerConstants.HealthCheckStatusCode.valueOf(code);
                return true;
            } catch (IllegalArgumentException ec) {
                return false;
            }
        });
    }

    private void validate(APICreateLoadBalancerListenerMsg msg) {
        if (msg.getInstancePort() == null) {
            msg.setInstancePort(msg.getLoadBalancerPort());
        }
        if (msg.getProtocol() == null) {
            msg.setProtocol(LoadBalancerConstants.LB_PROTOCOL_TCP);
        }
        if (msg.getHealthCheckProtocol() == null) {
            if (LoadBalancerConstants.LB_PROTOCOL_UDP.equals(msg.getProtocol())) {
                msg.setHealthCheckProtocol(LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP);
            } else {
                msg.setHealthCheckProtocol(LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP);
            }
        } else {
            if (LoadBalancerConstants.LB_PROTOCOL_UDP.equals(msg.getProtocol()) && !LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP.equals(msg.getHealthCheckProtocol()) ||
                    !LoadBalancerConstants.LB_PROTOCOL_UDP.equals(msg.getProtocol()) && LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP.equals(msg.getHealthCheckProtocol())) {
                throw new ApiMessageInterceptionException(
                        operr("the listener with protocol [%s] doesn't support this health check:[%s]",
                                msg.getProtocol(), msg.getHealthCheckProtocol()));
            }
            if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
                if (msg.getHealthCheckURI() == null) {
                    throw new ApiMessageInterceptionException(
                            operr("the http health check protocol must be specified its healthy checking parameter healthCheckURI"));
                }

                if (msg.getHealthCheckMethod() == null) {
                    msg.setHealthCheckMethod(LoadBalancerConstants.HealthCheckMothod.HEAD.toString());
                }
            }
            if (msg.getHealthCheckHttpCode() != null && !verifyHttpCode(msg.getHealthCheckHttpCode())) {
                throw new ApiMessageInterceptionException(
                        operr("the http health check protocol's expecting code [%s] is invalidate", msg.getHealthCheckHttpCode()));
            }
        }

        if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
            String expectResult = LoadBalancerConstants.HealthCheckStatusCode.http_2xx.toString();
            if (msg.getHealthCheckHttpCode() != null) {
                expectResult = msg.getHealthCheckHttpCode();
            }
            insertTagIfNotExisting(
                    msg, LoadBalancerSystemTags.HEALTH_PARAMETER,
                    LoadBalancerSystemTags.HEALTH_PARAMETER.instantiateTag(
                            map(e(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN, String.format("%s:%s:%s", msg.getHealthCheckMethod(), msg.getHealthCheckURI(), expectResult)))
                    )
            );
        }

        if (msg.getAclUuids() != null) {
            if (msg.getAclUuids().size() > LoadBalancerGlobalConfig.ACL_MAX_COUNT.value(Long.class)) {
                throw new ApiMessageInterceptionException(argerr("Can't attach more than %d access-control-list groups to a listener", LoadBalancerGlobalConfig.ACL_MAX_COUNT.value(Long.class)));
            }
            validateAcl(msg.getAclUuids(),new ArrayList<>(), msg.getLoadBalancerUuid());
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
                        map(e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, msg.getHealthCheckProtocol()+":default"))
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

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.BALANCER_ACL,
                LoadBalancerSystemTags.BALANCER_ACL.instantiateTag(
                        map(e(LoadBalancerSystemTags.BALANCER_ACL_TOKEN, msg.getAclStatus()))
                )
        );

        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.NUMBER_OF_PROCESS,
                LoadBalancerSystemTags.NUMBER_OF_PROCESS.instantiateTag(
                        map(e(LoadBalancerSystemTags.NUMBER_OF_PROCESS_TOKEN, LoadBalancerGlobalConfig.NUMBER_OF_PROCESS.value()))
                )
        );


        insertTagIfNotExisting(
                msg, LoadBalancerSystemTags.HTTP_MODE,
                LoadBalancerSystemTags.HTTP_MODE.instantiateTag(
                        map(e(LoadBalancerSystemTags.HTTP_MODE_TOKEN, LoadBalancerGlobalConfig.HTTP_MODE.value()))
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
                    if (Long.parseLong(s) > LoadBalancerConstants.MAX_CONNECTION_LIMIT) {
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
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, loadBalancerVO.getVipUuid()).listValues();
        VipUseForList useForList = new VipUseForList(useFor);
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
            throw new ApiMessageInterceptionException(argerr("loadbalancer listener [uuid:%s] already had certificate[uuid:%s]",
                     msg.getListenerUuid(),msg.getCertificateUuid()));
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
                    int port = Integer.parseInt(target);
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

        if (msg.getHealthCheckProtocol() != null && LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
            if (msg.getHealthCheckMethod() == null) {
                msg.setHealthCheckMethod(LoadBalancerConstants.HealthCheckMothod.HEAD.toString());
            }
            String tg = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(msg.getUuid(),
                    LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);
            String[] ts = tg.split(":");
            if (!msg.getHealthCheckProtocol().equals(ts[0]) && msg.getHealthCheckURI() == null) {
                throw new ApiMessageInterceptionException(
                        operr("the http health check protocol must be specified its healthy checking parameter healthCheckURI"));
            }
        }

        if (msg.getHealthCheckHttpCode() != null) {
            if (!verifyHttpCode(msg.getHealthCheckHttpCode())) {
                throw new ApiMessageInterceptionException(
                        operr("the http health check protocol's expecting code [%s] is invalidate", msg.getHealthCheckHttpCode()));
            }
        }

        LoadBalancerListenerVO listenerVO = Q.New(LoadBalancerListenerVO.class).
                                           eq(LoadBalancerListenerVO_.uuid,msg.getLoadBalancerListenerUuid()).find();

        if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
            String healthTarget = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(msg.getLoadBalancerListenerUuid(),
                    LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);

            String[] ts = healthTarget.split(":");
            if (ts.length != 2) {
                throw new OperationFailureException(argerr("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", target));
            }

            if (LoadBalancerConstants.LB_PROTOCOL_UDP.equals(listenerVO.getProtocol()) && !LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP.equals(msg.getHealthCheckProtocol()) ||
                    !LoadBalancerConstants.LB_PROTOCOL_UDP.equals(listenerVO.getProtocol()) && LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP.equals(msg.getHealthCheckProtocol())) {
                throw new ApiMessageInterceptionException(
                        operr("the listener with protocol [%s] doesn't support this health check:[%s]",
                                listenerVO.getProtocol(), msg.getHealthCheckProtocol()));
            }
            if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP.equals(ts[0]) && (msg.getHealthCheckMethod() == null || msg.getHealthCheckURI() == null)) {
                throw new ApiMessageInterceptionException(
                        operr("the http health check protocol must be specified its healthy checking parameters including healthCheckMethod and healthCheckURI"));
            }
        }

        msg.setLoadBalancerUuid(listenerVO.getLoadBalancerUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, listenerVO.getLoadBalancerUuid());
    }
}
