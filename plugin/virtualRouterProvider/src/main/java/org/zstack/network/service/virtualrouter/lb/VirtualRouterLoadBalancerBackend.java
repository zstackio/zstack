package org.zstack.network.service.virtualrouter.lb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.acl.AccessControlListEntryVO;
import org.zstack.header.acl.AccessControlListEntryVO_;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vyos.VyosGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentResponse;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.utils.*;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend extends AbstractVirtualRouterBackend
        implements LoadBalancerBackend, GlobalApiMessageInterceptor, ApiMessageInterceptor, VirtualRouterHaGetCallbackExtensionPoint,
        VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterLoadBalancerBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    private VirtualRouterVipBackend vipVrBkd;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private LbConfigProxy proxy;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private ResourceConfigFacade rcf;

    private final String REFRESH_CERTIFICATE_TASK = "refreshCertificate";
    private final String DELETE_CERTIFICATE_TASK = "deleteCertificate";
    private final String REFRESH_LB_TASK = "refreshLb";
    private final String DESTROY_LB_TASK = "destroyLb";

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIAddVmNicToLoadBalancerMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddVmNicToLoadBalancerMsg) {
            validate((APIAddVmNicToLoadBalancerMsg) msg);
        }

        return msg;
    }

    @Transactional(readOnly = true)
    private void validate(APIAddVmNicToLoadBalancerMsg msg) {
        List<String> attachedVmNicUuids = SQL.New("select ref.vmNicUuid " +
                "from LoadBalancerListenerVmNicRefVO ref, LoadBalancerListenerVO lbl " +
                "where ref.listenerUuid = lbl.uuid " +
                "and lbl.loadBalancerUuid = :lbUuid")
                .param("lbUuid", msg.getLoadBalancerUuid())
                .list();

        attachedVmNicUuids.addAll(msg.getVmNicUuids());

        Set<String> l3NetworkUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                        .in(VmNicVO_.uuid, attachedVmNicUuids)
                        .listValues());

        Set<String> vrUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                        .in(VmNicVO_.l3NetworkUuid, l3NetworkUuids)
                        .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                        .listValues());

        boolean valid = true;
        if (vrUuids.size() == 2 ) {
            if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(msg.getLoadBalancerUuid())
                    && vrUuids.stream().anyMatch(uuid -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(uuid))) {
                logger.debug(String.format(
                        "there are two virtual routers[uuids:%s] on l3 networks[uuids:%s] which vmnics[uuids:%s]" +
                                "attached", vrUuids, l3NetworkUuids, attachedVmNicUuids));
                valid = true;
            } else if (isVirtualRouterHaPair(new ArrayList<>(vrUuids))){
                valid = true;
            } else {
                valid = false;
            }
        } else if (vrUuids.size() > 1) {
            valid = false;
        }
        if (!valid) {
            throw new ApiMessageInterceptionException(argerr(
                    "new add vm nics[uuids:%s] and attached vmnics are not on the same vrouter, " +
                            "they are on vrouters[uuids:%s]", msg.getVmNicUuids(), vrUuids));
        }

        List<String> peerL3NetworkUuids = SQL.New("select peer.l3NetworkUuid " +
                "from LoadBalancerVO lb, VipVO vip, VipPeerL3NetworkRefVO peer " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and lb.uuid = :lbUuid")
                .param("lbUuid", msg.getLoadBalancerUuid())
                .list();

        if (peerL3NetworkUuids == null || peerL3NetworkUuids.isEmpty()) {
            return;
        }

        List<String> requestVmNicsL3NetworkUuids = Q.New(VmNicVO.class)
                .select(VmNicVO_.l3NetworkUuid)
                .in(VmNicVO_.uuid, msg.getVmNicUuids())
                .listValues();

        requestVmNicsL3NetworkUuids.addAll(peerL3NetworkUuids);
        vrUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                        .in(VmNicVO_.l3NetworkUuid, requestVmNicsL3NetworkUuids)
                        .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                        .listValues());

        if (vrUuids.size() > 1 && !isVirtualRouterHaPair(new ArrayList<>(vrUuids))) {
            throw new ApiMessageInterceptionException(argerr(
                    "new add vm nics[uuids:%s] and peer l3s[uuids:%s] of loadbalancer[uuid: %s]'s vip are not on the same vrouter, " +
                            "they are on vrouters[uuids:%s]", msg.getVmNicUuids(), peerL3NetworkUuids, msg.getLoadBalancerUuid(), vrUuids));
        }
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid) {
        List<VirtualRouterVmVO> vrs = getAllVirtualRouters(lbUuid);
        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            Optional<VirtualRouterVmInventory> vr = vrs.stream()
                    .filter(v -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(v.getUuid()))
                    .map(VirtualRouterVmInventory::valueOf)
                    .findFirst();

            return vr.orElse(null);
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(ResourceVO::getUuid).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid, List<String> vmNics) {
        List<VirtualRouterVmVO> vrs = getAllVirtualRouters(lbUuid);

        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            Optional<VirtualRouterVmVO> vr = vrs.stream()
                    .filter(v -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(v.getUuid()))
                    .findFirst();

            if (!vr.isPresent()) {
                return null;
            }

            List<String> vmNicL3NetworkUuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vmNics).listValues();

            VirtualRouterVmInventory vrInventory = VirtualRouterVmInventory.valueOf(vr.get());
            vmNicL3NetworkUuids.removeAll(vrInventory.getGuestL3Networks());

            if (!vmNicL3NetworkUuids.isEmpty()) {
                logger.debug(String.format("found l3 networks[uuids:%s] not attached to separate vr[uuid:%s] for loadbalancer[uuid:%s]",
                        vmNicL3NetworkUuids, vr.get().getUuid(), lbUuid));
                throw new CloudRuntimeException("not support separate vr with multiple networks vpc!");
            }
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(ResourceVO::getUuid).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    public static class LbTO {
        String lbUuid;
        String listenerUuid;
        String vip;
        String publicNic;
        List<String> nicIps;
        int instancePort;
        int loadBalancerPort;
        String mode;
        List<String> parameters;
        String  certificateUuid;

        public String getListenerUuid() {
            return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
            this.listenerUuid = listenerUuid;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }

        public String getLbUuid() {
            return lbUuid;
        }

        public void setLbUuid(String lbUuid) {
            this.lbUuid = lbUuid;
        }

        public String getVip() {
            return vip;
        }

        public void setVip(String vip) {
            this.vip = vip;
        }

        public List<String> getNicIps() {
            return nicIps;
        }

        public void setNicIps(List<String> nicIps) {
            this.nicIps = nicIps;
        }

        public int getInstancePort() {
            return instancePort;
        }

        public void setInstancePort(int instancePort) {
            this.instancePort = instancePort;
        }

        public int getLoadBalancerPort() {
            return loadBalancerPort;
        }

        public void setLoadBalancerPort(int loadBalancerPort) {
            this.loadBalancerPort = loadBalancerPort;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getCertificateUuid() {
            return certificateUuid;
        }

        public void setCertificateUuid(String certificateUuid) {
            this.certificateUuid = certificateUuid;
        }

        public String getPublicNic() {
            return publicNic;
        }

        public void setPublicNic(String publicNic) {
            this.publicNic = publicNic;
        }
    }

    public static class RefreshLbCmd extends AgentCommand {
        List<LbTO> lbs;
        public Boolean  enableHaproxyLog;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class RefreshLbLogLevelCmd extends AgentCommand {
        String level;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    public static class RefreshLbLogLevelRsp extends AgentResponse {
    }

    public static class RefreshLbRsp extends AgentResponse {
    }

    public static class CertificateCmd extends AgentCommand {
        String uuid;
        String certificate;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }
    }

    public static class CertificateRsp extends AgentResponse {
    }

    public static class DeleteLbCmd extends AgentCommand {
        List<LbTO> lbs;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class DeleteLbRsp extends AgentResponse {
    }

    public static final String REFRESH_LB_PATH = "/lb/refresh";
    public static final String DELETE_LB_PATH = "/lb/delete";
    public static final String REFRESH_LB_LOG_LEVEL_PATH = "/lb/log/level";
    public static final String CREATE_CERTIFICATE_PATH = "/certificate/create";
    public static final String DELETE_CERTIFICATE_PATH = "/certificate/delete";

    private List<LbTO> makeLbTOs(final LoadBalancerStruct struct, VirtualRouterVmInventory vr) {
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.add(VipVO_.uuid, Op.EQ, struct.getLb().getVipUuid());
        final VipVO vip = q.find();
        String publicMac = vr.getVmNics().stream()
                .filter(n -> n.getL3NetworkUuid().equals(vip.getL3NetworkUuid()))
                .findFirst().get().getMac();

        return CollectionUtils.transformToList(struct.getListeners(), new Function<LbTO, LoadBalancerListenerInventory>() {
            private List<String> makeAcl(LoadBalancerListenerInventory listenerInv) {
                String  aclEntry = "";
                List<String> aclRules = new ArrayList<>();
                List<LoadBalancerListenerACLRefInventory> refs = listenerInv.getAclRefs();
                if (refs.isEmpty()) {
                    aclRules.add(String.format("aclEntry::%s", aclEntry));
                    return aclRules;
                }

                aclRules.add(String.format("aclType::%s", refs.get(0).getType()));

                List<String> aclUuids = refs.stream().map(LoadBalancerListenerACLRefInventory::getAclUuid).collect(Collectors.toList());
                List<String> entry = Q.New(AccessControlListEntryVO.class).select(AccessControlListEntryVO_.ipEntries)
                                      .in(AccessControlListEntryVO_.aclUuid, aclUuids).listValues();
                if (!entry.isEmpty()) {
                    aclEntry = StringUtils.join(entry.toArray(), ',');
                }
                aclRules.add(String.format("aclEntry::%s", aclEntry));
                return aclRules;
            }

            @Override
            public LbTO call(LoadBalancerListenerInventory l) {
                LbTO to = new LbTO();
                to.setInstancePort(l.getInstancePort());
                to.setLoadBalancerPort(l.getLoadBalancerPort());
                to.setLbUuid(l.getLoadBalancerUuid());
                to.setListenerUuid(l.getUuid());
                to.setMode(l.getProtocol());
                to.setVip(vip.getIp());
                if (l.getCertificateRefs() != null && !l.getCertificateRefs().isEmpty()) {
                    to.setCertificateUuid(l.getCertificateRefs().get(0).getCertificateUuid());
                }
                to.setNicIps(CollectionUtils.transformToList(l.getVmNicRefs(), new Function<String, LoadBalancerListenerVmNicRefInventory>() {
                    @Override
                    public String call(LoadBalancerListenerVmNicRefInventory arg) {
                        if (LoadBalancerVmNicStatus.Active.toString().equals(arg.getStatus()) || LoadBalancerVmNicStatus.Pending.toString().equals(arg.getStatus())) {
                            VmNicInventory nic = struct.getVmNics().get(arg.getVmNicUuid());
                            if (nic == null) {
                                throw new CloudRuntimeException(String.format("cannot find nic[uuid:%s]", arg.getVmNicUuid()));
                            }
                            return nic.getIp();
                        }
                        return null;
                    }
                }));

                to.setPublicNic(publicMac);

                to.setParameters(CollectionUtils.transformToList(struct.getTags().get(l.getUuid()), new Function<String, String>() {
                    // vnicUuid::weight
                    @Override
                    public String call(String arg) {
                        if(LoadBalancerSystemTags.BALANCER_WEIGHT.isMatch(arg)) {
                            Map<String, String> token = TagUtils.parse(LoadBalancerSystemTags.BALANCER_WEIGHT.getTagFormat(), arg);
                            String nicUuid = token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN);
                            VmNicInventory nic = struct.getVmNics().get(nicUuid);
                            if (nic == null || !to.getNicIps().contains(nic.getIp())) {
                                return null;
                            }
                            arg = arg.replace(nicUuid, nic.getIp());
                        }
                        return arg;
                    }
                }));
                to.getParameters().addAll(makeAcl(l));
                return to;
            }
        });
    }

    private Set<String> getCertificates(List<LoadBalancerStruct> structs) {
        List<LoadBalancerListenerInventory> listeners = new ArrayList<>();
        for (LoadBalancerStruct struct : structs) {
            listeners.addAll(struct.getListeners());
        }

        return  CollectionUtils.transformToSet(listeners, (Function<String, LoadBalancerListenerInventory>) arg -> {
            if (arg.getVmNicRefs() != null && !arg.getVmNicRefs().isEmpty()
                    && arg.getCertificateRefs() != null && !arg.getCertificateRefs().isEmpty()) {
                return arg.getCertificateRefs().get(0).getCertificateUuid();
            } else {
                return null;
            }
        });
    }

    private void refreshCertificate(VirtualRouterVmInventory vr, boolean checkVrState, List<LoadBalancerStruct> struct, final Completion completion){
        Set<String> certificateUuids = getCertificates(struct);

        List<ErrorCode> errors = new ArrayList<>();
        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(CREATE_CERTIFICATE_PATH);
            msg.setCheckStatus(checkVrState);

            CertificateCmd cmd = new CertificateCmd();
            CertificateVO vo = dbf.findByUuid(uuid, CertificateVO.class);
            cmd.setUuid(uuid);
            cmd.setCertificate(vo.getCertificate());

            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(msg, new CloudBusCallBack(wcmpl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CertificateRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(CertificateRsp.class);
                        if (rsp.isSuccess()) {
                            wcmpl.done();
                        } else {
                            errors.add(operr("operation error, because:%s", rsp.getError()));
                            wcmpl.allDone();
                        }
                    } else {
                        errors.add(reply.getError());
                        wcmpl.allDone();
                    }
                }
            });
        }).run(new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if (errors.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.get(0));
                }
            }
        });
    }

    private void rollbackCertificate(VirtualRouterVmInventory vr, boolean checkVrState, List<LoadBalancerStruct> struct, final NoErrorCompletion completion){
        Set<String> certificateUuids = getCertificates(struct);

        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(DELETE_CERTIFICATE_PATH);
            msg.setCheckStatus(checkVrState);

            CertificateCmd cmd = new CertificateCmd();
            cmd.setUuid(uuid);

            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(msg, new CloudBusCallBack(wcmpl) {
                @Override
                public void run(MessageReply reply) {
                    wcmpl.done();
                }
            });
        }).run(new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.done();
            }
        });
    }

    private void refreshLbToVirtualRouter(VirtualRouterVmInventory vr, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(REFRESH_LB_PATH);
        msg.setCheckStatus(true);

        RefreshLbCmd cmd = new RefreshLbCmd();
        cmd.lbs = makeLbTOs(struct, vr);
        if (cmd.lbs.isEmpty()) {
            completion.success();
            return;
        }
        cmd.enableHaproxyLog = rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_HAPROXY_LOG, vr.getUuid(), Boolean.class);

        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    RefreshLbRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(RefreshLbRsp.class);
                    if (rsp.isSuccess()) {
                        new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    public void refresh(VirtualRouterVmInventory vr, LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("refresh-lb-to-virtualRouter");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "refresh-lb-ceriticae-to-virtualRouter";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, true, Collections.singletonList(struct), new Completion(trigger) {
                            @Override
                            public void success() {
                                refreshCertificateOnHaRouter(vr.getUuid(), Collections.singletonList(struct), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        rollbackCertificate(vr, true, Collections.singletonList(struct), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                rollbackCertificateOnHaRouter(vr.getUuid(), Collections.singletonList(struct), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.rollback();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.rollback();
                                    }
                                });
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-listener-to-virtualRouter";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshLbToVirtualRouter(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                refreshLbToVirtualRouterHa(vr, struct, new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void stopVip(final LoadBalancerStruct struct, final List<VmNicInventory> nics, final Completion completion) {
        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        vipStruct.setServiceUuid(struct.getLb().getUuid());

        Set<String> guestL3NetworkUuids = nics.stream()
                                              .map(VmNicInventory::getL3NetworkUuid)
                                              .collect(Collectors.toSet());

        /*remove the l3networks still attached*/
        Set<String> vnicUuidsAttached = new HashSet<>();
        struct.getListeners().forEach(listenerRef ->
                vnicUuidsAttached.addAll(listenerRef.getVmNicRefs().stream().map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toSet())));
        if (!vnicUuidsAttached.isEmpty()) {
            List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vnicUuidsAttached).listValues();
            if (l3Uuids != null && !l3Uuids.isEmpty()) {
                guestL3NetworkUuids.removeAll(l3Uuids);
            }
        }

        if (guestL3NetworkUuids.isEmpty()) {
            completion.success();
            return;
        }
        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                vipStruct.getPeerL3NetworkUuids().get(0), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        vipStruct.setServiceProvider(providerType.toString());
        Vip v = new Vip(struct.getLb().getVipUuid());
        v.setStruct(vipStruct);
        v.stop(completion);
    }

    private void acquireVip(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final List<VmNicInventory> nics, final Completion completion) {
        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        vipStruct.setServiceUuid(struct.getLb().getUuid());
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        vipStruct.setServiceProvider(providerType.toString());

        Set<String> guestL3NetworkUuids = nics.stream()
                .map(VmNicInventory::getL3NetworkUuid)
                .collect(Collectors.toSet());

        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
        Vip v = new Vip(struct.getLb().getVipUuid());
        v.setStruct(vipStruct);
        v.acquire(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void startVrIfNeededAndRefresh(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, List<VmNicInventory> nics, final Completion completion) {
        acquireVip(vr, struct, nics, new Completion(completion) {
            @Override
            public void success() {
                startVrIfNeededAndRefresh(vr, struct, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void startVrIfNeededAndRefresh(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final Completion completion) {
        if (!VmInstanceState.Stopped.toString().equals(vr.getState())) {
            refresh(vr, struct, completion);
            return;
        }

        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("start-vr-%s-and-refresh-lb-%s", vr.getUuid(), struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "start-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        StartVmInstanceMsg msg = new StartVmInstanceMsg();
                        msg.setVmInstanceUuid(vr.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-vip-on-vr";
                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        vipStruct.setServiceUuid(struct.getLb().getUuid());
                        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType.toString());
                        vipStruct.setPeerL3NetworkUuids(vr.getGuestL3Networks());

                        Vip v = new Vip(struct.getLb().getVipUuid());
                        v.setStruct(vipStruct);
                        v.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                success = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (!success) {
                            trigger.rollback();
                            return;
                        }

                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        vipStruct.setServiceUuid(struct.getLb().getUuid());
                        vipStruct.setPeerL3NetworkUuids(vr.getGuestL3Networks());
                        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType.toString());
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(vipStruct);
                        v.stop(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addVmNics(final LoadBalancerStruct struct, List<VmNicInventory> nics, final Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid(),
                nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()));
        if (vr != null) {
            startVrIfNeededAndRefresh(vr, struct, nics, completion);
            return;
        }

        VmNicInventory nic = nics.get(0);
        final L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, struct.getLb().getVipUuid()).listValues();
        VipUseForList useForList = new VipUseForList(useFor);
        if (!useForList.isIncluded(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)) {
            logger.warn(String.format("the vip[uuid:%s, name:%s, ip:%s, useFor: %s] is not for load balancer", vip.getUuid(),
                            vip.getName(), vip.getIp(), vip.getUseFor()));
        }

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-nic-to-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            VirtualRouterVmInventory vr;

            @Override
            public void setup() {
                if (separateVr) {
                    flow(new Flow() {
                             String __name__ = "lock-vip";
                        /*
                        now the vip support multi services and it doesn't need to lock vip
                         that will be locked by itself in vip module via to VipNetworkServicesRefVO
                        * */
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());

                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            List<String> attachedVmNicUuids = new ArrayList<>();
                            for (LoadBalancerListenerInventory ll : struct.getLb().getListeners()) {

                                attachedVmNicUuids.addAll(ll.getVmNicRefs().stream()
                                                            .filter(r -> !LoadBalancerVmNicStatus.Pending.toString().equals(r.getStatus()))
                                                            .map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
                            }

                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());
                            guestL3NetworkUuids.removeAll(attachedVmNicUuids);
                            if (guestL3NetworkUuids.isEmpty()) {
                                logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                        "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                                trigger.rollback();
                                return;
                            }
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                                    vr.getGuestL3Networks().get(0), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                            vipStruct.setServiceProvider(providerType.toString());
                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.stop(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-separate-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct();
                            s.setInherentSystemTags(list(VirtualRouterSystemTags.DEDICATED_ROLE_VR.getTagFormat(), VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                            s.setVirtualRouterVmSelector(new VirtualRouterVmSelector() {
                                @Override
                                public VirtualRouterVmVO select(List<VirtualRouterVmVO> vrs) {
                                    return null;
                                }
                            });
                            s.setL3Network(l3);
                            s.setNotGatewayForGuestL3Network(true);

                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (vr == null) {
                                trigger.rollback();
                                return;
                            }

                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        //TODO:
                                        logger.warn(String.format("failed to destroy vr[uuid:%s], %s. Need a cleanup", vr.getUuid(), reply.getError()));
                                    }

                                    trigger.rollback();
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-vip-on-vr";
                        boolean success = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            vipVrBkd.acquireVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    success = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (!success) {
                                trigger.rollback();
                                return;
                            }

                            vipVrBkd.releaseVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        }
                    });

                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "acquire-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct();
                            s.setL3Network(l3);
                            /*flat network*/
                            if (l3.getNetworkServiceTypes().contains(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)) {
                                s.setNotGatewayForGuestL3Network(true);
                            }

                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "acquire-vip";
                        boolean success = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                    LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                            vipStruct.setServiceProvider(providerType.toString());

                            Set<String> guestL3NetworkUuids = nics.stream()
                                    .map(VmNicInventory::getL3NetworkUuid)
                                    .collect(Collectors.toSet());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    success = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (!success) {
                                trigger.rollback();
                                return;
                            }

                            List<String> attachedVmNicUuids = new ArrayList<>();
                            for (LoadBalancerListenerInventory ll : struct.getLb().getListeners()) {

                                attachedVmNicUuids.addAll(ll.getVmNicRefs().stream()
                                                            .filter(r -> !LoadBalancerVmNicStatus.Pending.toString().equals(r.getStatus()))
                                                            .map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
                            }

                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());
                            guestL3NetworkUuids.removeAll(attachedVmNicUuids);
                            if (guestL3NetworkUuids.isEmpty()) {
                                logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                        "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                                trigger.rollback();
                                return;
                            }
                            
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                    LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                            vipStruct.setServiceProvider(providerType.toString());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.stop(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });

                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        proxy.attachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addVmNic(final LoadBalancerStruct struct, VmNicInventory nic, final Completion completion) {
        addVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {
        removeVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNics(LoadBalancerStruct struct, List<VmNicInventory> nics, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());
        if ( separateVr ) {
            logger.error("not support the separate vrouter currently.");
            // no support the case
            completion.success();
            return;
        }

        if (vr == null) {
            // the vr has been destroyed, it just need modify the Vip
            stopVip(struct, nics, completion);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-nic-from-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            //VirtualRouterVmInventory vr;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
                            // no need to remove as the vr is stopped
                            trigger.next();
                            return;
                        }

                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-l3network-from-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        stopVip(struct, nics, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        if (struct.getListeners().isEmpty() || struct.getListeners().stream().allMatch(r->r.getVmNicRefs() == null || r.getVmNicRefs().isEmpty())) {
                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        }

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            throw new OperationFailureException(operr("cannot find virtual router for load balancer [uuid:%s]", struct.getLb().getUuid()));
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    @Override
    public void removeListener(final LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());
        if ( separateVr ) {
            logger.error("not support the separate vrouter currently.");
            // no support the case
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-Listener-from-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
                            trigger.next();
                            return;
                        }

                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-l3networks-from-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        vipStruct.setServiceUuid(struct.getLb().getUuid());

                        Set<String> nicUuids = listener.getVmNicRefs().stream().map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toSet());
                        if (nicUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        List<String> guestL3NetworkUuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, nicUuids).listValues();

                        /*remove the l3networks still attached*/
                        Set<String> vnicUuidsAttached = new HashSet<>();
                        struct.getListeners().forEach(listenerRef ->
                                vnicUuidsAttached.addAll(listenerRef.getVmNicRefs().stream().map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toSet())));
                        if (!vnicUuidsAttached.isEmpty()) {
                            List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vnicUuidsAttached).listValues();
                            if (l3Uuids != null && !l3Uuids.isEmpty()) {
                                guestL3NetworkUuids.removeAll(l3Uuids);
                            }
                        }
                        if (guestL3NetworkUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                                vipStruct.getPeerL3NetworkUuids().get(0), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType.toString());
                        Vip v = new Vip(struct.getLb().getVipUuid());
                        v.setStruct(vipStruct);
                        v.stop(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        if (struct.getListeners().isEmpty() || struct.getListeners().stream().allMatch(r->r.getVmNicRefs() == null || r.getVmNicRefs().isEmpty())) {
                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void destroyLoadBalancer(final LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-lb-%s-from-vr", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-from-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
                        if (vr == null) {
                            // the vr has been destroyed
                            trigger.next();
                            return;
                        }

                        List<String> roles = new VirtualRouterRoleManager().getAllRoles(vr.getUuid());
                        if (roles.size() == 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                            }
                                }
                            });
                        } else if (roles.size() > 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            destroyLoadBalancerOnVirtualRouter(vr, struct, new Completion(trigger) {
                                @Override
                                public void success() {
                                    destroyLoadBalancerOnHaRouter(vr.getUuid(), struct, new Completion(trigger) {
                                        @Override
                                        public void success() {
                                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    });
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        } else {
                            throw new CloudRuntimeException(String.format("wrong virtual router roles%s. it doesn't have the role[%s]",
                                    roles, VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                        }
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void refresh(LoadBalancerStruct struct, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    /* this api is called from VirtualRouterSyncLbOnStartFlow which is specified to a individual router */
    public void syncOnStart(VirtualRouterVmInventory vr, boolean checkStatus, List<LoadBalancerStruct> structs, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("lb-sync-on-Start");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "lb-sync-certificate-on-start";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, checkStatus, structs, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        rollbackCertificate(vr, false, structs, new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "lb-sync-listener-on-start";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<LbTO> tos = new ArrayList<LbTO>();
                        for (LoadBalancerStruct s : structs) {
                            tos.addAll(makeLbTOs(s, vr));
                        }

                        RefreshLbCmd cmd = new RefreshLbCmd();
                        cmd.lbs = tos;
                        cmd.enableHaproxyLog = rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_HAPROXY_LOG, vr.getUuid(), Boolean.class);

                        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setPath(REFRESH_LB_PATH);
                        msg.setVmInstanceUuid(vr.getUuid());
                        msg.setCheckStatus(checkStatus);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());

                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    VirtualRouterAsyncHttpCallReply kr = reply.castReply();
                                    RefreshLbRsp rsp = kr.toResponse(RefreshLbRsp.class);
                                    if (rsp.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    protected List<VirtualRouterVmVO> getAllVirtualRouters(String lbUuid) {
        List<String> vrUuids = proxy.getVrUuidsByNetworkService(LoadBalancerVO.class.getSimpleName(), lbUuid);
        if (vrUuids == null || vrUuids.isEmpty()) {
            return new ArrayList<>();
        }

        return Q.New(VirtualRouterVmVO.class).in(VirtualRouterVmVO_.uuid, vrUuids).list();
    }

    public void destroyLoadBalancerOnVirtualRouter(VirtualRouterVmInventory vr, LoadBalancerStruct struct, Completion completion) {
        DeleteLbCmd cmd = new DeleteLbCmd();
        cmd.setLbs(makeLbTOs(struct, vr));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(DELETE_LB_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    DeleteLbRsp rsp = ((VirtualRouterAsyncHttpCallReply)reply).toResponse(DeleteLbRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private boolean isVirtualRouterHaPair(List<String> vrUuids) {
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
             return ext.isVirtualRouterInSameHaPair(vrUuids);
        }

        return false;
    }

    private void refreshCertificateOnHaRouter(String vrUuid, List<LoadBalancerStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REFRESH_CERTIFICATE_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private void rollbackCertificateOnHaRouter(String vrUuid, List<LoadBalancerStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(DELETE_CERTIFICATE_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void refreshLbToVirtualRouterHa(VirtualRouterVmInventory vrInv, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REFRESH_LB_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void destroyLoadBalancerOnHaRouter(String vrUuid, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(DESTROY_LB_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private List<LoadBalancerStruct> getLoadBalancersByL3Networks(String l3Uuid, boolean detach) {
        List<LoadBalancerStruct> ret = new ArrayList<>();
        String sql = "select distinct l from LoadBalancerListenerVO l, LoadBalancerListenerVmNicRefVO ref, VmNicVO nic, UsedIpVO ip " +
                "where l.uuid=ref.listenerUuid and ref.status in (:status) and ref.vmNicUuid=nic.uuid and nic.uuid=ip.vmNicUuid and ip.l3NetworkUuid=(:l3Uuid)";

        List<LoadBalancerListenerVO> listenerVOS = SQL.New(sql, LoadBalancerListenerVO.class).param("l3Uuid", l3Uuid)
                .param("status", asList(LoadBalancerVmNicStatus.Active, LoadBalancerVmNicStatus.Pending)).list();
        if (listenerVOS == null || listenerVOS.isEmpty()){
            return ret;
        }

        HashMap<String, List<LoadBalancerListenerVO>> listenerMap = new HashMap<>();
        for (LoadBalancerListenerVO vo : listenerVOS) {
            listenerMap.computeIfAbsent(vo.getLoadBalancerUuid(), k-> new ArrayList<>()).add(vo);
        }

        for (Map.Entry<String, List<LoadBalancerListenerVO>> e : listenerMap.entrySet()) {
            List<String> listenerUuids = e.getValue().stream().map(LoadBalancerListenerVO::getUuid).collect(Collectors.toList());
            HashMap<String, VmNicInventory> nicMap = new HashMap<>();
            sql = "select nic from LoadBalancerListenerVmNicRefVO ref, VmNicVO nic " +
                    "where nic.uuid=ref.vmNicUuid and ref.listenerUuid in (:listenerUuids) and ref.status in (:status)";

            List<VmNicVO> nicVOS = SQL.New(sql, VmNicVO.class).param("listenerUuids", listenerUuids)
                    .param("status", asList(LoadBalancerVmNicStatus.Active, LoadBalancerVmNicStatus.Pending)).list();
            if (nicVOS != null && !nicVOS.isEmpty()){
                for (VmNicVO nic : nicVOS) {
                    if (!detach) {
                        nicMap.put(nic.getUuid(), VmNicInventory.valueOf(nic));
                    } else {
                        /* when detach nic, vm nics of same l3 should not be included */
                        List<String> nicL3Uuids = nic.getUsedIps().stream().map(UsedIpVO::getL3NetworkUuid).collect(Collectors.toList());
                        if (!nicL3Uuids.contains(l3Uuid)) {
                            nicMap.put(nic.getUuid(), VmNicInventory.valueOf(nic));
                        }
                    }
                }
            }

            Map<String, List<String>> systemTags = new HashMap<>();
            for (LoadBalancerListenerVO l : listenerVOS) {
                SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
                q.select(SystemTagVO_.tag);
                q.add(SystemTagVO_.resourceUuid, Op.EQ, l.getUuid());
                q.add(SystemTagVO_.resourceType, Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
                systemTags.put(l.getUuid(), q.listValue());
            }

            LoadBalancerStruct struct = new LoadBalancerStruct();
            LoadBalancerVO lb = dbf.findByUuid(e.getKey(), LoadBalancerVO.class);
            struct.setLb(LoadBalancerInventory.valueOf(lb));
            struct.setListeners(LoadBalancerListenerInventory.valueOf(e.getValue()));
            struct.setVmNics(nicMap);
            struct.setTags(systemTags);
            ret.add(struct);
        }

        return ret;
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<LoadBalancerStruct> lbs = getLoadBalancersByL3Networks(nic.getL3NetworkUuid(), false);
        if (lbs == null || lbs.isEmpty()) {
            completion.success();
            return;
        }

        syncOnStart(vr, true, lbs, new Completion(completion) {
            @Override
            public void success() {
                List<String> lbUuids = lbs.stream().map(s -> s.getLb().getUuid()).collect(Collectors.toList());
                proxy.attachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), lbUuids);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
       /* ZSTAC-24726 for lb, it's not necessary to implement this interface
       * delete network/detach user vm nic, under these cases, the removeVmNics extend point will be triggered
       * that will remove the lb reference with nic first, and refresh lb to agent.
       * */
       completion.success();
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct refreshCertificate = new VirtualRouterHaCallbackStruct();
        refreshCertificate.type = REFRESH_CERTIFICATE_TASK;
        refreshCertificate.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need refresh certificate on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct[] s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct[].class);
                refreshCertificate(VirtualRouterVmInventory.valueOf(vrVO), false, Arrays.asList(s), completion);
            }
        };
        structs.add(refreshCertificate);

        VirtualRouterHaCallbackStruct deleteCertificate = new VirtualRouterHaCallbackStruct();
        deleteCertificate.type = DELETE_CERTIFICATE_TASK;
        deleteCertificate.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need delete certificate on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct[] s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct[].class);
                rollbackCertificate(VirtualRouterVmInventory.valueOf(vrVO), false, Arrays.asList(s), new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.success();
                    }
                });
            }
        };
        structs.add(deleteCertificate);

        VirtualRouterHaCallbackStruct refreshLb = new VirtualRouterHaCallbackStruct();
        refreshLb.type = REFRESH_LB_TASK;
        refreshLb.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need refresh Lb on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct.class);
                refreshLbToVirtualRouter(VirtualRouterVmInventory.valueOf(vrVO), s, completion);
            }
        };
        structs.add(refreshLb);

        VirtualRouterHaCallbackStruct destroyLb = new VirtualRouterHaCallbackStruct();
        destroyLb.type = DESTROY_LB_TASK;
        destroyLb.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need d Lb on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct.class);
                destroyLoadBalancerOnVirtualRouter(VirtualRouterVmInventory.valueOf(vrVO), s, completion);
            }
        };
        structs.add(destroyLb);

        return structs;
    }
}
