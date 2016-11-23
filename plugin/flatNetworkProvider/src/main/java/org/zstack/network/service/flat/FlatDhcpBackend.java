package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.EventBasedGCPersistentContext;
import org.zstack.core.gc.GCEventTrigger;
import org.zstack.core.gc.GCFacade;
import org.zstack.core.logging.Event;
import org.zstack.core.logging.Log;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.header.network.service.NetworkServiceDhcpBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmCommandSender.SteppingSendCallback;
import org.zstack.network.service.NetworkProviderFinder;
import org.zstack.network.service.NetworkServiceProviderLookup;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatDhcpBackend extends AbstractService implements NetworkServiceDhcpBackend, KVMHostConnectExtensionPoint,
        L3NetworkDeleteExtensionPoint, VmInstanceMigrateExtensionPoint, VmAbnormalLifeCycleExtensionPoint, IpRangeDeletionExtensionPoint,
        BeforeStartNewCreatedVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatDhcpBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private GCFacade gcf;

    public static final String APPLY_DHCP_PATH = "/flatnetworkprovider/dhcp/apply";
    public static final String PREPARE_DHCP_PATH = "/flatnetworkprovider/dhcp/prepare";
    public static final String RELEASE_DHCP_PATH = "/flatnetworkprovider/dhcp/release";
    public static final String DHCP_CONNECT_PATH = "/flatnetworkprovider/dhcp/connect";
    public static final String RESET_DEFAULT_GATEWAY_PATH = "/flatnetworkprovider/dhcp/resetDefaultGateway";
    public static final String DHCP_DELETE_NAMESPACE_PATH = "/flatnetworkprovider/dhcp/deletenamespace";

    private Map<String, UsedIpInventory> l3NetworkDhcpServerIp = new ConcurrentHashMap<String, UsedIpInventory>();

    public static String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid);
    }

    @Transactional(readOnly = true)
    private List<DhcpInfo> getDhcpInfoForConnectedKvmHost(KVMHostConnectedContext context) {
        String sql = "select vm.uuid, vm.defaultL3NetworkUuid from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state in (:states) and vm.type = :vtype";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("huuid", context.getInventory().getUuid());
        q.setParameter("states", list(VmInstanceState.Running, VmInstanceState.Unknown));
        q.setParameter("vtype", VmInstanceConstant.USER_VM_TYPE);
        List<Tuple> ts = q.getResultList();
        if (ts.isEmpty()) {
            return null;
        }

        Map<String, String> vmDefaultL3 = new HashMap<String, String>();
        for (Tuple t : ts) {
            vmDefaultL3.put(t.get(0, String.class), t.get(1, String.class));
        }

        sql = "select nic from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider where nic.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and provider.type = :ptype and nic.vmInstanceUuid in (:vmUuids) group by nic.uuid";

        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("vmUuids", vmDefaultL3.keySet());
        List<VmNicVO> nics = nq.getResultList();
        if (nics.isEmpty()) {
            return null;
        }

        List<String> l3Uuids = CollectionUtils.transformToList(nics, new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getL3NetworkUuid();
            }
        });

        sql = "select t.tag, l3.uuid from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid in (:l3Uuids)";
        TypedQuery<Tuple> tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()));
        tq.setParameter("l3Uuids", l3Uuids);
        tq.setParameter("ttype", L2NetworkVO.class.getSimpleName());
        ts = tq.getResultList();

        Map<String, String> bridgeNames = new HashMap<String, String>();
        for (Tuple t : ts) {
            bridgeNames.put(t.get(1, String.class), t.get(0, String.class));
        }

        sql = "select t.tag, vm.uuid from SystemTagVO t, VmInstanceVO vm where t.resourceType = :ttype" +
                " and t.tag like :tag and t.resourceUuid = vm.uuid and vm.uuid in (:vmUuids)";
        tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(VmSystemTags.HOSTNAME.getTagFormat()));
        tq.setParameter("ttype", VmInstanceVO.class.getSimpleName());
        tq.setParameter("vmUuids", vmDefaultL3.keySet());
        Map<String, String> hostnames = new HashMap<String, String>();
        for (Tuple t : ts) {
            hostnames.put(t.get(1, String.class), t.get(0, String.class));
        }

        sql = "select l3 from L3NetworkVO l3 where l3.uuid in (:l3Uuids)";
        TypedQuery<L3NetworkVO> l3q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        l3q.setParameter("l3Uuids", l3Uuids);
        List<L3NetworkVO> l3s = l3q.getResultList();
        Map<String, L3NetworkVO> l3Map = new HashMap<String, L3NetworkVO>();
        for (L3NetworkVO l3 : l3s) {
            l3Map.put(l3.getUuid(), l3);
        }

        List<DhcpInfo> dhcpInfoList = new ArrayList<DhcpInfo>();
        for (VmNicVO nic : nics) {
            DhcpInfo info = new DhcpInfo();
            info.bridgeName = KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeNames.get(nic.getL3NetworkUuid()), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
            info.namespaceName = makeNamespaceName(
                    info.bridgeName,
                    nic.getL3NetworkUuid()
            );
            DebugUtils.Assert(info.bridgeName != null, "bridge name cannot be null");
            info.mac = nic.getMac();
            info.netmask = nic.getNetmask();
            info.isDefaultL3Network = nic.getL3NetworkUuid().equals(vmDefaultL3.get(nic.getVmInstanceUuid()));
            info.ip = nic.getIp();
            info.gateway = nic.getGateway();

            L3NetworkVO l3 = l3Map.get(nic.getL3NetworkUuid());
            info.dnsDomain = l3.getDnsDomain();
            info.dns = CollectionUtils.transformToList(l3.getDns(), new Function<String, L3NetworkDnsVO>() {
                @Override
                public String call(L3NetworkDnsVO arg) {
                    return arg.getDns();
                }
            });

            if (info.isDefaultL3Network) {
                info.hostname = hostnames.get(nic.getVmInstanceUuid());
                if (info.hostname == null) {
                    info.hostname = nic.getIp().replaceAll("\\.", "-");
                }

                if (info.dnsDomain != null) {
                    info.hostname = String.format("%s.%s", info.hostname, info.dnsDomain);
                }
            }

            info.l3NetworkUuid = l3.getUuid();

            dhcpInfoList.add(info);
        }

        return dhcpInfoList;
    }


    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetL3NetworkDhcpIpAddressMsg) {
            handle((APIGetL3NetworkDhcpIpAddressMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof FlatDhcpAcquireDhcpServerIpMsg) {
            handle((FlatDhcpAcquireDhcpServerIpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetL3NetworkDhcpIpAddressMsg msg) {
        APIGetL3NetworkDhcpIpAddressReply reply = new APIGetL3NetworkDhcpIpAddressReply();

        if (msg.getL3NetworkUuid() == null) {
            reply.setError(errf.stringToOperationError("l3 network uuid cannot be null"));
            bus.reply(msg, reply);
            return;
        }

        UsedIpInventory ip = l3NetworkDhcpServerIp.get(msg.getL3NetworkUuid());
        if (ip != null) {
            reply.setIp(ip.getIp());
            bus.reply(msg, reply);
            return;
        }

        String tag = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTag(msg.getL3NetworkUuid());
        if (tag != null) {
            Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByTag(tag);
            String ipUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
            UsedIpVO vo = dbf.findByUuid(ipUuid, UsedIpVO.class);
            if (vo == null) {
                throw new CloudRuntimeException(String.format("cannot find used ip [uuid:%s]", ipUuid));
            }

            ip = UsedIpInventory.valueOf(vo);
            l3NetworkDhcpServerIp.put(msg.getL3NetworkUuid(), ip);
            reply.setIp(ip.getIp());
            bus.reply(msg, reply);
            logger.debug(String.format("APIGetL3NetworkDhcpIpAddressMsg[ip:%s, uuid:%s] for l3 network[uuid:%s]",
                    ip.getIp(), ip.getUuid(), ip.getL3NetworkUuid()));
            return;
        }

        reply.setError(errf.stringToOperationError(
                String.format("Cannot find DhcpIp for l3 network[uuid:%s]", msg.getL3NetworkUuid())));
        bus.reply(msg, reply);
    }

    @Deferred
    public UsedIpInventory allocateDhcpIp(String l3Uuid) {
        UsedIpInventory ip = l3NetworkDhcpServerIp.get(l3Uuid);
        if (ip != null) {
            return ip;
        }

        // TODO: static allocate the IP to avoid the lock
        GLock lock = new GLock(String.format("l3-%s-allocate-dhcp-ip", l3Uuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        String tag = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTag(l3Uuid);
        if (tag != null) {
            Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByTag(tag);
            String ipUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
            UsedIpVO vo = dbf.findByUuid(ipUuid, UsedIpVO.class);
            if (vo == null) {
                throw new CloudRuntimeException(String.format("cannot find used ip [uuid:%s]", ipUuid));
            }

            ip = UsedIpInventory.valueOf(vo);
            l3NetworkDhcpServerIp.put(l3Uuid, ip);
            return ip;
        }

        AllocateIpMsg amsg = new AllocateIpMsg();
        amsg.setL3NetworkUuid(l3Uuid);
        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
        MessageReply reply = bus.call(amsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        AllocateIpReply r = reply.castReply();
        ip = r.getIpInventory();

        FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.createInherentTag(
                l3Uuid,
                map(
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN, ip.getIp()),
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN, ip.getUuid())
                )
        );

        l3NetworkDhcpServerIp.put(l3Uuid, ip);
        logger.debug(String.format("allocate DHCP server IP[ip:%s, uuid:%s] for l3 network[uuid:%s]", ip.getIp(), ip.getUuid(), ip.getL3NetworkUuid()));
        return ip;
    }

    private void handle(final FlatDhcpAcquireDhcpServerIpMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                dealMessage(msg);
                return null;
            }

            @MessageSafe
            private void dealMessage(FlatDhcpAcquireDhcpServerIpMsg msg) {
                FlatDhcpAcquireDhcpServerIpReply reply = new FlatDhcpAcquireDhcpServerIpReply();
                UsedIpInventory ip = allocateDhcpIp(msg.getL3NetworkUuid());
                reply.setIp(ip.getIp());
                reply.setNetmask(ip.getNetmask());
                reply.setUsedIpUuid(ip.getUuid());
                bus.reply(msg, reply);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return String.format("flat-dhcp-get-dhcp-ip-for-l3-network-%s", msg.getL3NetworkUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(FlatNetworkServiceConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    private boolean isProvidedbyMe(L3NetworkInventory l3) {
        String providerType = new NetworkProviderFinder().getNetworkProviderTypeByNetworkServiceType(l3.getUuid(), NetworkServiceType.DHCP.toString());
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING.equals(providerType);
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        if (!isProvidedbyMe(inventory)) {
            return;
        }

        UsedIpInventory dhchip = getDHCPServerIP(inventory.getUuid());
        if (dhchip != null) {
            deleteDhcpServerIp(dhchip);
            logger.debug(String.format("delete DHCP IP[%s] of the flat network[uuid:%s] as the L3 network is deleted",
                    dhchip.getIp(), dhchip.getL3NetworkUuid()));
        }

        deleteNameSpace(inventory, dhchip);
    }

    private void deleteNameSpace(L3NetworkInventory inventory, UsedIpInventory dhchip) {
        List<String> huuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select host.uuid from HostVO host, L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = host.clusterUuid and l2.uuid = :uuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("uuid", inventory.getL2NetworkUuid());
                return q.getResultList();
            }
        }.call();

        if (huuids.isEmpty()) {
            return;
        }

        String brName = new BridgeNameFinder().findByL3Uuid(inventory.getUuid());
        DeleteNamespaceCmd cmd = new DeleteNamespaceCmd();
        cmd.bridgeName = brName;
        cmd.namespaceName = makeNamespaceName(brName, inventory.getUuid());
        cmd.dhcpIp = dhchip == null ? null : dhchip.getIp();

        new KvmCommandSender(huuids).send(cmd, DHCP_DELETE_NAMESPACE_PATH, wrapper -> {
            DeleteNamespaceRsp rsp = wrapper.getResponse(DeleteNamespaceRsp.class);
            return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
        }, new SteppingSendCallback<KvmResponseWrapper>() {
            @Override
            public void success(KvmResponseWrapper w) {
                logger.debug(String.format("successfully deleted namespace for L3 network[uuid:%s, name:%s] on the " +
                        "KVM host[uuid:%s]", inventory.getUuid(), inventory.getName(), getHostUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    new Event().log(FlatNetworkLabels.DELETE_NAMESPACE_FAILURE, inventory.getName(), inventory.getUuid(),
                            getHostUuid(), errorCode.toString());
                    return;
                }

                GCFlatDHCPDeleteNamespaceContext c = new GCFlatDHCPDeleteNamespaceContext();
                c.setHostUuid(getHostUuid());
                c.setCommand(cmd);
                c.setTriggerHostStatus(HostStatus.Connected.toString());

                EventBasedGCPersistentContext<GCFlatDHCPDeleteNamespaceContext> ctx = new EventBasedGCPersistentContext<GCFlatDHCPDeleteNamespaceContext>();
                ctx.setRunnerClass(GCFlatDHCPDeleteNamespaceRunner.class);
                ctx.setContextClass(GCFlatDHCPDeleteNamespaceContext.class);
                ctx.setName(String.format("delete-namespace-for-l3-%s", inventory.getUuid()));
                ctx.setContext(c);

                GCEventTrigger trigger = new GCEventTrigger();
                trigger.setCodeName("gc-delete-vm-on-host-connected");
                trigger.setEventPath(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH);
                String code = ln(
                        "import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData",
                        "import org.zstack.network.service.flat.GCFlatDHCPDeleteNamespaceContext",
                        "HostStatusChangedData d = (HostStatusChangedData) data",
                        "GCFlatDHCPDeleteNamespaceContext c = (GCFlatDHCPDeleteNamespaceContext) context",
                        "return c.hostUuid == d.hostUuid && d.newStatus == c.triggerHostStatus"
                ).toString();
                trigger.setCode(code);
                ctx.addTrigger(trigger);

                trigger = new GCEventTrigger();
                trigger.setCodeName("gc-delete-vm-on-host-deleted");
                trigger.setEventPath(HostCanonicalEvents.HOST_DELETED_PATH);
                code = ln(
                        "import org.zstack.header.host.HostCanonicalEvents.HostDeletedData",
                        "import org.zstack.network.service.flat.GCFlatDHCPDeleteNamespaceContext",
                        "HostDeletedData d = (HostDeletedData) data",
                        "GCFlatDHCPDeleteNamespaceContext c = (GCFlatDHCPDeleteNamespaceContext) context",
                        "return c.hostUuid == d.hostUuid"
                ).toString();
                trigger.setCode(code);
                ctx.addTrigger(trigger);

                gcf.schedule(ctx);
            }
        });
    }

    @Transactional(readOnly = true)
    private List<DhcpInfo> getVmDhcpInfo(VmInstanceInventory vm) {
        String sql = "select nic from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider where nic.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and provider.type = :ptype and nic.vmInstanceUuid = :vmUuid group by nic.uuid";

        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("vmUuid", vm.getUuid());
        List<VmNicVO> nics = nq.getResultList();
        if (nics.isEmpty()) {
            return null;
        }

        List<String> l3Uuids = CollectionUtils.transformToList(nics, new Function<String, VmNicVO>() {
            @Override
            public String call(VmNicVO arg) {
                return arg.getL3NetworkUuid();
            }
        });

        sql = "select t.tag, l3.uuid from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid in (:l3Uuids)";
        TypedQuery<Tuple> tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()));
        tq.setParameter("l3Uuids", l3Uuids);
        tq.setParameter("ttype", L2NetworkVO.class.getSimpleName());
        List<Tuple> ts = tq.getResultList();

        Map<String, String> bridgeNames = new HashMap<String, String>();
        for (Tuple t : ts) {
            bridgeNames.put(t.get(1, String.class), t.get(0, String.class));
        }

        sql = "select t.tag, vm.uuid from SystemTagVO t, VmInstanceVO vm where t.resourceType = :ttype" +
                " and t.tag like :tag and t.resourceUuid = vm.uuid and vm.uuid = :vmUuid";
        tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(VmSystemTags.HOSTNAME.getTagFormat()));
        tq.setParameter("ttype", VmInstanceVO.class.getSimpleName());
        tq.setParameter("vmUuid", vm.getUuid());
        Map<String, String> hostnames = new HashMap<String, String>();
        for (Tuple t : ts) {
            hostnames.put(t.get(1, String.class), t.get(0, String.class));
        }

        sql = "select l3 from L3NetworkVO l3 where l3.uuid in (:l3Uuids)";
        TypedQuery<L3NetworkVO> l3q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        l3q.setParameter("l3Uuids", l3Uuids);
        List<L3NetworkVO> l3s = l3q.getResultList();
        Map<String, L3NetworkVO> l3Map = new HashMap<String, L3NetworkVO>();
        for (L3NetworkVO l3 : l3s) {
            l3Map.put(l3.getUuid(), l3);
        }

        List<DhcpInfo> dhcpInfoList = new ArrayList<DhcpInfo>();
        for (VmNicVO nic : nics) {
            DhcpInfo info = new DhcpInfo();
            info.bridgeName = KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeNames.get(nic.getL3NetworkUuid()), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
            info.namespaceName = makeNamespaceName(
                    info.bridgeName,
                    nic.getL3NetworkUuid()
            );
            DebugUtils.Assert(info.bridgeName != null, "bridge name cannot be null");
            info.mac = nic.getMac();
            info.netmask = nic.getNetmask();
            info.isDefaultL3Network = nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid());
            info.ip = nic.getIp();
            info.gateway = nic.getGateway();

            L3NetworkVO l3 = l3Map.get(nic.getL3NetworkUuid());
            info.dnsDomain = l3.getDnsDomain();
            info.dns = CollectionUtils.transformToList(l3.getDns(), new Function<String, L3NetworkDnsVO>() {
                @Override
                public String call(L3NetworkDnsVO arg) {
                    return arg.getDns();
                }
            });

            if (info.isDefaultL3Network) {
                info.hostname = hostnames.get(nic.getVmInstanceUuid());
                if (info.hostname == null) {
                    info.hostname = nic.getIp().replaceAll("\\.", "-");
                }

                if (info.dnsDomain != null) {
                    info.hostname = String.format("%s.%s", info.hostname, info.dnsDomain);
                }
            }

            info.l3NetworkUuid = l3.getUuid();

            dhcpInfoList.add(info);
        }

        return dhcpInfoList;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null) {
            return;
        }

        FutureCompletion completion = new FutureCompletion();
        applyDhcpToHosts(info, destHostUuid, false, completion);
        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("cannot configure DHCP for vm[uuid:%s] on the destination host[uuid:%s]",
                            inv.getUuid(), destHostUuid), completion.getErrorCode()
            ));
        }
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null) {
            return;
        }

        releaseDhcpService(info, inv.getUuid(), srcHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null) {
            return;
        }

        releaseDhcpService(info, inv.getUuid(), destHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(final VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "flat-network-configure-dhcp";
            VmAbnormalLifeCycleOperation operation = struct.getOperation();
            VmInstanceInventory vm = struct.getVmInstance();
            List<DhcpInfo> info = getVmDhcpInfo(vm);

            String applyHostUuidForRollback;
            String releaseHostUuidForRollback;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (info == null) {
                    trigger.next();
                    return;
                }

                if (operation == VmAbnormalLifeCycleOperation.VmRunningOnTheHost) {
                    vmRunningOnTheHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost) {
                    vmStoppedOnTheSameHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged) {
                    vmRunningFromUnknownStateHostChanged(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostNotChanged) {
                    vmRunningFromUnknownStateHostNotChanged(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                    vmMigrateToAnotherHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmRunningFromIntermediateState(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmMigrateToAnotherHost(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                applyHostUuidForRollback = struct.getOriginalHostUuid();

                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmRunningFromUnknownStateHostNotChanged(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmRunningFromUnknownStateHostChanged(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                applyHostUuidForRollback = struct.getCurrentHostUuid();
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmStoppedOnTheSameHost(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getCurrentHostUuid(), new NoErrorCompletion(trigger) {
                    @Override
                    public void done() {
                        applyHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }
                });
            }

            private void vmRunningOnTheHost(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
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
                if (info == null) {
                    trigger.rollback();
                    return;
                }

                if (releaseHostUuidForRollback != null) {
                    releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                }
                if (applyHostUuidForRollback != null) {
                    applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion() {
                        @Override
                        public void success() {
                            //ignore
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to re-apply DHCP info of the vm[uuid:%s] to the host[uuid:%s], %s",
                                    vm.getUuid(), applyHostUuidForRollback, errorCode));
                        }
                    });
                }

                trigger.rollback();
            }
        };
    }

    @Override
    public void preDeleteIpRange(IpRangeInventory ipRange) {

    }

    @Override
    public void beforeDeleteIpRange(IpRangeInventory ipRange) {

    }

    private void deleteDhcpServerIp(UsedIpInventory ip) {
        l3NetworkDhcpServerIp.remove(ip.getL3NetworkUuid());
        FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.deleteInherentTag(ip.getL3NetworkUuid());
        dbf.removeByPrimaryKey(ip.getUuid(), UsedIpVO.class);
    }

    private UsedIpInventory getDHCPServerIP(String l3Uuid) {
        UsedIpInventory dhcpIp = l3NetworkDhcpServerIp.get(l3Uuid);
        if (dhcpIp != null) {
            return dhcpIp;
        }

        String tag = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTag(l3Uuid);
        if (tag != null) {
            Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByTag(tag);
            String ipUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
            UsedIpVO vo = dbf.findByUuid(ipUuid, UsedIpVO.class);
            if (vo != null) {
                return UsedIpInventory.valueOf(vo);
            }
        }

        return null;
    }

    @Override
    public void afterDeleteIpRange(IpRangeInventory ipRange) {
        UsedIpInventory dhcpIp = getDHCPServerIP(ipRange.getL3NetworkUuid());

        if (dhcpIp != null && NetworkUtils.isIpv4InRange(dhcpIp.getIp(), ipRange.getStartIp(), ipRange.getEndIp())) {
            deleteDhcpServerIp(dhcpIp);
            logger.debug(String.format("delete DHCP IP[%s] of the flat network[uuid:%s] as the IP range[uuid:%s] is deleted",
                    dhcpIp.getIp(), ipRange.getL3NetworkUuid(), ipRange.getUuid()));
        }
    }

    @Override
    public void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode) {

    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-flat-dhcp";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final List<DhcpInfo> dhcpInfoList = getDhcpInfoForConnectedKvmHost(context);
                if (dhcpInfoList == null) {
                    trigger.next();
                    return;
                }

                new Log(context.getInventory().getUuid()).log(FlatNetworkLabel.SYNC_DHCP);

                // to flush ebtables
                ConnectCmd cmd = new ConnectCmd();
                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(context.getInventory().getUuid());
                msg.setCommand(cmd);
                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                msg.setNoStatusCheck(true);
                msg.setPath(DHCP_CONNECT_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, context.getInventory().getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            applyDhcpToHosts(dhcpInfoList, context.getInventory().getUuid(), true, new Completion(trigger) {
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
                    }
                });
            }
        };
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceSpec spec) {
        String providerUuid = new NetworkServiceProviderLookup().lookupUuidByType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);

        // make sure the Flat DHCP acquired DHCP server IP before starting VMs,
        // otherwise it may not be able to get IP when lots of VMs start concurrently
        // because the logic of VM acquiring IP is ahead flat DHCP acquiring IP
        for (L3NetworkInventory l3 : spec.getL3Networks()) {
            List<String> serviceTypes = l3.getNetworkServiceTypesFromProvider(providerUuid);
            if (serviceTypes.contains(NetworkServiceType.DHCP.toString())) {
                allocateDhcpIp(l3.getUuid());
            }
        }
    }

    public static class DhcpInfo {
        public String ip;
        public String mac;
        public String netmask;
        public String gateway;
        public String hostname;
        public boolean isDefaultL3Network;
        public String dnsDomain;
        public List<String> dns;
        public String bridgeName;
        public String namespaceName;
        public String l3NetworkUuid;
    }

    public static class ApplyDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<DhcpInfo> dhcp;
        public boolean rebuild;
        public String l3NetworkUuid;
    }

    public static class ApplyDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ReleaseDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<DhcpInfo> dhcp;
    }

    public static class ReleaseDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class PrepareDhcpCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String dhcpServerIp;
        public String dhcpNetmask;
        public String namespaceName;
    }

    public static class PrepareDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ConnectCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class ConnectRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ResetDefaultGatewayCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeNameOfGatewayToRemove;
        public String namespaceNameOfGatewayToRemove;
        public String gatewayToRemove;
        public String macOfGatewayToRemove;
        public String gatewayToAdd;
        public String macOfGatewayToAdd;
        public String bridgeNameOfGatewayToAdd;
        public String namespaceNameOfGatewayToAdd;
    }

    public static class ResetDefaultGatewayRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DeleteNamespaceCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String namespaceName;
        public String dhcpIp;
    }

    public static class DeleteNamespaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    private List<DhcpInfo> toDhcpInfo(List<DhcpStruct> structs) {
        final Map<String, String> l3Bridges = new HashMap<String, String>();
        for (DhcpStruct s : structs) {
            if (!l3Bridges.containsKey(s.getL3Network().getUuid())) {
                l3Bridges.put(s.getL3Network().getUuid(),
                        KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(s.getL3Network().getL2NetworkUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
            }
        }

        return CollectionUtils.transformToList(structs, new Function<DhcpInfo, DhcpStruct>() {
            @Override
            public DhcpInfo call(DhcpStruct arg) {
                DhcpInfo info = new DhcpInfo();
                info.dnsDomain = arg.getDnsDomain();
                info.gateway = arg.getGateway();
                info.hostname = arg.getHostname();
                info.isDefaultL3Network = arg.isDefaultL3Network();

                if (info.isDefaultL3Network) {
                    if (info.hostname == null) {
                        info.hostname = arg.getIp().replaceAll("\\.", "-");
                    }

                    if (info.dnsDomain != null) {
                        info.hostname = String.format("%s.%s", info.hostname, info.dnsDomain);
                    }
                }

                info.ip = arg.getIp();
                info.netmask = arg.getNetmask();
                info.mac = arg.getMac();
                info.dns = arg.getL3Network().getDns();
                info.l3NetworkUuid = arg.getL3Network().getUuid();
                info.bridgeName = l3Bridges.get(arg.getL3Network().getUuid());
                info.namespaceName = makeNamespaceName(info.bridgeName, arg.getL3Network().getUuid());
                return info;
            }
        });
    }

    private void applyDhcpToHosts(List<DhcpInfo> dhcpInfo, final String hostUuid, final boolean rebuild, final Completion completion) {
        final Map<String, List<DhcpInfo>> l3DhcpMap = new HashMap<String, List<DhcpInfo>>();
        for (DhcpInfo d : dhcpInfo) {
            List<DhcpInfo> lst = l3DhcpMap.get(d.l3NetworkUuid);
            if (lst == null) {
                lst = new ArrayList<DhcpInfo>();
                l3DhcpMap.put(d.l3NetworkUuid, lst);
            }

            lst.add(d);
        }

        final Iterator<Map.Entry<String, List<DhcpInfo>>> it = l3DhcpMap.entrySet().iterator();
        class DhcpApply {
            void apply() {
                if (!it.hasNext()) {
                    completion.success();
                    return;
                }

                Map.Entry<String, List<DhcpInfo>> e = it.next();
                final String l3Uuid = e.getKey();
                final List<DhcpInfo> info = e.getValue();

                DebugUtils.Assert(!info.isEmpty(), "how can info be empty???");

                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName(String.format("flat-dhcp-provider-apply-dhcp-to-l3-network-%s", l3Uuid));
                chain.then(new ShareFlow() {
                    String dhcpServerIp;
                    String dhcpNetmask;

                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = "get-dhcp-server-ip";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                FlatDhcpAcquireDhcpServerIpMsg msg = new FlatDhcpAcquireDhcpServerIpMsg();
                                msg.setL3NetworkUuid(l3Uuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3Uuid);
                                bus.send(msg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                        } else {
                                            FlatDhcpAcquireDhcpServerIpReply r = reply.castReply();
                                            dhcpServerIp = r.getIp();
                                            dhcpNetmask = r.getNetmask();
                                            trigger.next();
                                        }
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "prepare-distributed-dhcp-server-on-host";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                DhcpInfo i = info.get(0);

                                PrepareDhcpCmd cmd = new PrepareDhcpCmd();
                                cmd.bridgeName = i.bridgeName;
                                cmd.namespaceName = i.namespaceName;
                                cmd.dhcpServerIp = dhcpServerIp;
                                cmd.dhcpNetmask = dhcpNetmask;

                                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                                msg.setHostUuid(hostUuid);
                                msg.setNoStatusCheck(true);
                                msg.setCommand(cmd);
                                msg.setPath(PREPARE_DHCP_PATH);
                                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                                bus.send(msg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }

                                        KVMHostAsyncHttpCallReply ar = reply.castReply();
                                        PrepareDhcpRsp rsp = ar.toResponse(PrepareDhcpRsp.class);
                                        if (!rsp.isSuccess()) {
                                            trigger.fail(errf.stringToOperationError(rsp.getError()));
                                            return;
                                        }

                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "apply-dhcp";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                ApplyDhcpCmd cmd = new ApplyDhcpCmd();
                                cmd.dhcp = info;
                                cmd.rebuild = rebuild;
                                cmd.l3NetworkUuid = l3Uuid;

                                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                                msg.setCommand(cmd);
                                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                                msg.setHostUuid(hostUuid);
                                msg.setPath(APPLY_DHCP_PATH);
                                msg.setNoStatusCheck(true);
                                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                                bus.send(msg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }

                                        KVMHostAsyncHttpCallReply r = reply.castReply();
                                        ApplyDhcpRsp rsp = r.toResponse(ApplyDhcpRsp.class);
                                        if (!rsp.isSuccess()) {
                                            trigger.fail(errf.stringToOperationError(rsp.getError()));
                                            return;
                                        }

                                        trigger.next();
                                    }
                                });
                            }
                        });

                        done(new FlowDoneHandler(completion) {
                            @Override
                            public void handle(Map data) {
                                apply();
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
        }

        new DhcpApply().apply();
    }

    @Override
    public void applyDhcpService(List<DhcpStruct> dhcpStructList, VmInstanceSpec spec, final Completion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDhcpToHosts(toDhcpInfo(dhcpStructList), spec.getDestHost().getUuid(), false, completion);
    }

    private void releaseDhcpService(List<DhcpInfo> info, final String vmUuid, final String hostUuid, final NoErrorCompletion completion) {
        final ReleaseDhcpCmd cmd = new ReleaseDhcpCmd();
        cmd.dhcp = info;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setHostUuid(hostUuid);
        msg.setPath(RELEASE_DHCP_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO:
                    logger.warn(String.format("failed to release dhcp%s for vm[uuid: %s] on the kvm host[uuid:%s]; %s",
                            cmd.dhcp, vmUuid, hostUuid, reply.getError()));
                    completion.done();
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ReleaseDhcpRsp rsp = r.toResponse(ReleaseDhcpRsp.class);
                if (!rsp.isSuccess()) {
                    //TODO
                    logger.warn(String.format("failed to release dhcp%s for vm[uuid: %s] on the kvm host[uuid:%s]; %s",
                            cmd.dhcp, vmUuid, hostUuid, rsp.getError()));
                    completion.done();
                    return;
                }

                completion.done();
            }
        });
    }

    @Override
    public void releaseDhcpService(List<DhcpStruct> dhcpStructsList, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (dhcpStructsList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDhcpService(toDhcpInfo(dhcpStructsList), spec.getVmInventory().getUuid(), spec.getDestHost().getUuid(), completion);
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, final Completion completion) {
        DebugUtils.Assert(previousL3 != null || nowL3 != null, "why I get two NULL L3 networks!!!!");

        if (!VmInstanceState.Running.toString().equals(vm.getState())) {
            return;
        }

        VmNicInventory pnic = null;
        VmNicInventory nnic = null;

        for (VmNicInventory nic : vm.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(previousL3)) {
                pnic = nic;
            } else if (nic.getL3NetworkUuid().equals(nowL3)) {
                nnic = nic;
            }
        }

        ResetDefaultGatewayCmd cmd = new ResetDefaultGatewayCmd();
        if (pnic != null) {
            cmd.gatewayToRemove = pnic.getGateway();
            cmd.macOfGatewayToRemove = pnic.getMac();
            cmd.bridgeNameOfGatewayToRemove = new BridgeNameFinder().findByL3Uuid(previousL3);
            cmd.namespaceNameOfGatewayToRemove = makeNamespaceName(cmd.bridgeNameOfGatewayToRemove, previousL3);
        }
        if (nnic != null) {
            cmd.gatewayToAdd = nnic.getGateway();
            cmd.macOfGatewayToAdd = nnic.getMac();
            cmd.bridgeNameOfGatewayToAdd = new BridgeNameFinder().findByL3Uuid(nowL3);
            cmd.namespaceNameOfGatewayToAdd = makeNamespaceName(cmd.bridgeNameOfGatewayToAdd, nowL3);
        }

        KvmCommandSender sender = new KvmCommandSender(vm.getHostUuid());
        sender.send(cmd, RESET_DEFAULT_GATEWAY_PATH, wrapper -> {
            ResetDefaultGatewayRsp rsp = wrapper.getResponse(ResetDefaultGatewayRsp.class);
            return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
