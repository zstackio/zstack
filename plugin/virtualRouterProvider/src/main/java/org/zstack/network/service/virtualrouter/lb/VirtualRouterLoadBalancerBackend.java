package org.zstack.network.service.virtualrouter.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentResponse;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.VipUseForList;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend extends AbstractVirtualRouterBackend
        implements LoadBalancerBackend, GlobalApiMessageInterceptor, ApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(VirtualRouterLoadBalancerBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    private VirtualRouterVipBackend vipVrBkd;
    @Autowired
    private VipManager vipMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private NetworkServiceManager nwServiceMgr;

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

        if (vrUuids.size() == 2
                && LoadBalancerSystemTags.SEPARATE_VR.hasTag(msg.getLoadBalancerUuid())
                && vrUuids.stream().anyMatch(uuid -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(uuid))) {
            logger.debug(String.format(
                    "there are two virtual routers[uuids:%s] on l3 networks[uuids:%s] which vmnics[uuids:%s]" +
                            "attached", vrUuids, l3NetworkUuids, attachedVmNicUuids
            ));
        } else if (vrUuids.size() > 1) {
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

        if (vrUuids.size() > 1) {
            throw new ApiMessageInterceptionException(argerr(
                    "new add vm nics[uuids:%s] and peer l3s[uuids:%s] of loadbalancer[uuid: %s]'s vip are not on the same vrouter, " +
                            "they are on vrouters[uuids:%s]", msg.getVmNicUuids(), peerL3NetworkUuids, msg.getLoadBalancerUuid(), vrUuids));
        }
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid) {
        String sql = "select vr from VirtualRouterVmVO vr, VirtualRouterLoadBalancerRefVO ref where ref.virtualRouterVmUuid =" +
                " vr.uuid and ref.loadBalancerUuid = :lbUuid";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("lbUuid", lbUuid);
        List<VirtualRouterVmVO> vrs = q.getResultList();

        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            Optional<VirtualRouterVmInventory> vr = vrs.stream()
                    .filter(v -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(v.getUuid()))
                    .map(v -> VirtualRouterVmInventory.valueOf(v))
                    .findFirst();

            return vr.orElse(null);
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(v -> v.getUuid()).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid, List<String> vmNics) {
        String sql = "select vr from VirtualRouterVmVO vr, VirtualRouterLoadBalancerRefVO ref where ref.virtualRouterVmUuid =" +
                " vr.uuid and ref.loadBalancerUuid = :lbUuid";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("lbUuid", lbUuid);
        List<VirtualRouterVmVO> vrs = q.getResultList();

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
                throw new CloudRuntimeException(String.format("not support separate vr with multiple networks vpc!"));
            }
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(v -> v.getUuid()).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    public static class LbTO {
        String lbUuid;
        String listenerUuid;
        String vip;
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
    }

    public static class RefreshLbCmd extends AgentCommand {
        List<LbTO> lbs;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
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
    public static final String CREATE_CERTIFICATE_PATH = "/certificate/create";
    public static final String DELETE_CERTIFICATE_PATH = "/certificate/delete";

    private List<LbTO> makeLbTOs(final LoadBalancerStruct struct) {
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.select(VipVO_.ip);
        q.add(VipVO_.uuid, Op.EQ, struct.getLb().getVipUuid());
        final String vip = q.findValue();

        return CollectionUtils.transformToList(struct.getListeners(), new Function<LbTO, LoadBalancerListenerInventory>() {
            @Override
            public LbTO call(LoadBalancerListenerInventory l) {
                LbTO to = new LbTO();
                to.setInstancePort(l.getInstancePort());
                to.setLoadBalancerPort(l.getLoadBalancerPort());
                to.setLbUuid(l.getLoadBalancerUuid());
                to.setListenerUuid(l.getUuid());
                to.setMode(l.getProtocol());
                to.setVip(vip);
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

                SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
                q.select(SystemTagVO_.tag);
                q.add(SystemTagVO_.resourceUuid, Op.EQ, l.getUuid());
                q.add(SystemTagVO_.resourceType, Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
                List<String> tags = q.listValue();
                to.setParameters(tags);
                return to;
            }
        });
    }

    private Set<String> getCertificates(List<LoadBalancerStruct> structs) {
        List<LoadBalancerListenerInventory> listeners = new ArrayList<>();
        for (LoadBalancerStruct struct : structs) {
            listeners.addAll(struct.getListeners());
        }

        return  CollectionUtils.transformToSet(listeners, new Function<String, LoadBalancerListenerInventory>() {
            @Override
            public String call(LoadBalancerListenerInventory arg) {
                if (arg.getVmNicRefs() != null && !arg.getVmNicRefs().isEmpty()
                        && arg.getCertificateRefs() != null && !arg.getCertificateRefs().isEmpty()) {
                    return arg.getCertificateRefs().get(0).getCertificateUuid();
                } else {
                    return null;
                }
            }
        });
    }

    private void refreshCertificate(VirtualRouterVmInventory vr, List<LoadBalancerStruct> struct, final Completion completion){
        Set<String> certificateUuids = getCertificates(struct);

        List<ErrorCode> errors = new ArrayList<>();
        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(CREATE_CERTIFICATE_PATH);

            CertificateCmd cmd = new CertificateCmd();
            CertificateVO vo = dbf.findByUuid(uuid, CertificateVO.class);
            cmd.setUuid(uuid);
            cmd.setCertificate(vo.getCertificate());

            msg.setCommand(cmd);
            msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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

    private void rollbackCertificate(VirtualRouterVmInventory vr, List<LoadBalancerStruct> struct, final NoErrorCompletion completion){
        Set<String> certificateUuids = getCertificates(struct);

        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(DELETE_CERTIFICATE_PATH);

            CertificateCmd cmd = new CertificateCmd();
            cmd.setUuid(uuid);

            msg.setCommand(cmd);
            msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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

    private void refresh(VirtualRouterVmInventory vr, LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("refresh-lb-to-virtualRouter");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "refresh-lb-ceriticae-to-virtualRouter";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, Collections.singletonList(struct), new Completion(trigger) {
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
                        rollbackCertificate(vr, Collections.singletonList(struct), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-listener-to-virtualRouter";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                        msg.setVmInstanceUuid(vr.getUuid());
                        msg.setPath(REFRESH_LB_PATH);

                        RefreshLbCmd cmd = new RefreshLbCmd();
                        cmd.lbs = makeLbTOs(struct);

                        msg.setCommand(cmd);
                        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    RefreshLbRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(RefreshLbRsp.class);
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

    private void acquireVip(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final List<VmNicInventory> nics, final Completion completion) {
        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        vipStruct.setServiceProvider(providerType.toString());

        Set<String> guestL3NetworkUuids = nics.stream()
                .map(nic -> nic.getL3NetworkUuid())
                .collect(Collectors.toSet());
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(guestL3NetworkUuids).all((guestL3NetworkUuid, completion1) -> {
            vipStruct.setPeerL3NetworkUuid(guestL3NetworkUuid);

            Vip v = new Vip(struct.getLb().getVipUuid());
            v.setStruct(vipStruct);
            v.acquire(new Completion(completion1) {
                @Override
                public void success() {
                    completion1.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    completion1.done();
                }
            });
        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                } else {
                    completion.success();
                }
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
                        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType.toString());

                        ErrorCodeList errList = new ErrorCodeList();

                        new While<>(vr.getGuestL3Networks()).all((guestL3NetworkUuid, completion1) -> {
                            vipStruct.setPeerL3NetworkUuid(guestL3NetworkUuid);

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    completion1.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    completion1.done();
                                }
                            });
                        }).run(new NoErrorCompletion() {
                            @Override
                            public void done() {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(errList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
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
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(vipStruct);
                        v.release(new Completion(trigger) {
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
                nics.stream().map(n -> n.getUuid()).collect(Collectors.toList()));
        if (vr != null) {
            startVrIfNeededAndRefresh(vr, struct, nics, completion);
            return;
        }

        VmNicInventory nic = nics.get(0);
        final L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));

        VipUseForList useForList = new VipUseForList(vip.getUseFor());
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
                flow(new Flow() {
                    String __name__ = "lock-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);

                        Set<String> guestL3NetworkUuids = nics.stream()
                                .map(VmNicInventory::getL3NetworkUuid)
                                .collect(Collectors.toSet());
                        ErrorCodeList errList = new ErrorCodeList();

                        new While<>(guestL3NetworkUuids).all((guestL3NetworkUuid, completion1) -> {
                            vipStruct.setPeerL3NetworkUuid(guestL3NetworkUuid);

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    completion1.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    completion1.done();
                                }
                            });
                        }).run(new NoErrorCompletion() {
                            @Override
                            public void done() {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(errList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<String> attachedVmNicUuids = new ArrayList<>();
                        for (LoadBalancerListenerInventory ll : struct.getLb().getListeners()) {
                            attachedVmNicUuids.addAll(ll.getVmNicRefs().stream()
                                    .map(r -> r.getVmNicUuid()).collect(Collectors.toList()));
                        }
                        attachedVmNicUuids.removeAll(
                                nics.stream().map(n -> n.getUuid()).collect(Collectors.toSet()));
                        if (attachedVmNicUuids != null && !attachedVmNicUuids.isEmpty()) {
                            logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                    "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                            trigger.rollback();
                            return;
                        }

                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(vipStruct);
                        v.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                if (separateVr) {
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
                        String __name__ = "create-vip-on-vr";
                        boolean success = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vr.getGuestL3Networks().get(0),
                                    LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
                            vipStruct.setServiceProvider(providerType.toString());

                            Set<String> guestL3NetworkUuids = nics.stream()
                                    .map(VmNicInventory::getL3NetworkUuid)
                                    .collect(Collectors.toSet());
                            ErrorCodeList errList = new ErrorCodeList();

                            new While<>(guestL3NetworkUuids).all((guestL3NetworkUuid, completion1) -> {
                                vipStruct.setPeerL3NetworkUuid(guestL3NetworkUuid);

                                Vip v = new Vip(vip.getUuid());
                                v.setStruct(vipStruct);
                                v.acquire(new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        completion1.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        errList.getCauses().add(errorCode);
                                        completion1.done();
                                    }
                                });
                            }).run(new NoErrorCompletion() {
                                @Override
                                public void done() {
                                    if (!errList.getCauses().isEmpty()) {
                                        success = true;
                                        trigger.fail(errList.getCauses().get(0));
                                    } else {
                                        trigger.next();
                                    }
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
                                        .map(LoadBalancerListenerVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
                            }
                            attachedVmNicUuids.removeAll(
                                    nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toSet()));
                            if (!attachedVmNicUuids.isEmpty()) {
                                logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                        "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                                trigger.rollback();
                                return;
                            }

                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.release(new Completion(trigger) {
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

                        new SQLBatch(){

                            @Override
                            protected void scripts() {
                                List<VirtualRouterLoadBalancerRefVO> refs = Q.New(VirtualRouterLoadBalancerRefVO.class)
                                        .eq(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid,struct.getLb().getUuid())
                                        .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vr.getUuid()).list();
                                if (refs.size() == 0) {
                                    VirtualRouterLoadBalancerRefVO ref = new VirtualRouterLoadBalancerRefVO();
                                    ref.setLoadBalancerUuid(struct.getLb().getUuid());
                                    ref.setVirtualRouterVmUuid(vr.getUuid());
                                    persist(ref);
                                }
                            }
                        }.execute();
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
    public void removeVmNics(LoadBalancerStruct struct, List<VmNicInventory> nic, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
            // no need to remove as the vr is stopped
            completion.success();
            return;
        }

        refresh(vr, struct, completion);
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
    public void removeListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
            completion.success();
            return;
        }

        refresh(vr, struct, completion);
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
                            DeleteLbCmd cmd = new DeleteLbCmd();
                            cmd.setLbs(makeLbTOs(struct));

                            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            msg.setPath(DELETE_LB_PATH);
                            msg.setCommand(cmd);
                            msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        DeleteLbRsp rsp = ((VirtualRouterAsyncHttpCallReply)reply).toResponse(DeleteLbRsp.class);
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

    void syncOnStart(VirtualRouterVmInventory vr, List<LoadBalancerStruct> structs, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("lb-sync-on-Start");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "lb-sync-certificate-on-start";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, structs, new Completion(trigger) {
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
                        rollbackCertificate(vr, structs, new NoErrorCompletion(trigger) {
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
                            tos.addAll(makeLbTOs(s));
                        }

                        RefreshLbCmd cmd = new RefreshLbCmd();
                        cmd.lbs = tos;

                        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                        msg.setPath(REFRESH_LB_PATH);
                        msg.setVmInstanceUuid(vr.getUuid());
                        msg.setCheckStatus(false);
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
}
