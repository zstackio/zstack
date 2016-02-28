package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeNoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.header.network.service.NetworkServiceDhcpBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.kvm.*;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatDhcpBackend extends AbstractService implements NetworkServiceDhcpBackend, KVMHostConnectExtensionPoint,
        L3NetworkDeleteExtensionPoint, VmInstanceMigrateExtensionPoint, VmAbnormalLifeCycleExtensionPoint, IpRangeDeletionExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatDhcpBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    public static final String APPLY_DHCP_PATH = "/flatnetworkprovider/dhcp/apply";
    public static final String PREPARE_DHCP_PATH = "/flatnetworkprovider/dhcp/prepare";
    public static final String RELEASE_DHCP_PATH = "/flatnetworkprovider/dhcp/release";

    private Map<String, UsedIpInventory> l3NetworkDhcpServerIp = new ConcurrentHashMap<String, UsedIpInventory>();

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
    public void kvmHostConnected(KVMHostConnectedContext context) throws KVMHostConnectException {
        List<DhcpInfo> dhcpInfoList = getDhcpInfoForConnectedKvmHost(context);
        if (dhcpInfoList == null) {
            return;
        }

        FutureCompletion completion = new FutureCompletion();
        applyDhcpToHosts(dhcpInfoList, context.getInventory().getUuid(), true, completion);
        completion.await();
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof FlatDhcpAcquireDhcpServerIpMsg) {
            handle((FlatDhcpAcquireDhcpServerIpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
                UsedIpInventory ip = getIp();
                reply.setIp(ip.getIp());
                reply.setNetmask(ip.getNetmask());
                reply.setUsedIpUuid(ip.getUuid());
                bus.reply(msg, reply);
            }

            private UsedIpInventory getIp() {
                UsedIpInventory ip = l3NetworkDhcpServerIp.get(msg.getL3NetworkUuid());
                if (ip != null) {
                    return ip;
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
                    return ip;
                }

                AllocateIpMsg amsg = new AllocateIpMsg();
                amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                MessageReply reply = bus.call(amsg);
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                AllocateIpReply r = reply.castReply();
                ip = r.getIpInventory();

                FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.createInherentTag(
                        msg.getL3NetworkUuid(),
                        map(
                                e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN, ip.getIp()),
                                e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN, ip.getUuid())
                        )
                );

                l3NetworkDhcpServerIp.put(msg.getL3NetworkUuid(), ip);
                logger.debug(String.format("allocate DHCP server IP[ip:%s, uuid:%s] for l3 network[uuid:%s]", ip.getIp(), ip.getUuid(), ip.getL3NetworkUuid()));
                return ip;
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

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        UsedIpInventory dhchip = l3NetworkDhcpServerIp.get(inventory.getUuid());
        if (dhchip != null) {
            deleteDhcpServerIp(dhchip);
            logger.debug(String.format("delete DHCP IP[%s] of the flat network[uuid:%s] as the L3 network is deleted",
                    dhchip.getIp(), dhchip.getL3NetworkUuid()));
        }
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
    public String preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null) {
            return null;
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

        return null;
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

    @Override
    public void afterDeleteIpRange(IpRangeInventory ipRange) {
        UsedIpInventory dhcpIp =  l3NetworkDhcpServerIp.get(ipRange.getL3NetworkUuid());
        if (dhcpIp != null && NetworkUtils.isIpv4InRange(dhcpIp.getIp(), ipRange.getStartIp(), ipRange.getEndIp())) {
            deleteDhcpServerIp(dhcpIp);
            logger.debug(String.format("delete DHCP IP[%s] of the flat network[uuid:%s] as the IP range[uuid:%s] is deleted",
                    dhcpIp.getIp(), ipRange.getL3NetworkUuid(), ipRange.getUuid()));
        }
    }

    @Override
    public void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode) {

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
        public String l3NetworkUuid;
    }

    public static class ApplyDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<DhcpInfo> dhcp;
        public boolean rebuild;
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
    }

    public static class PrepareDhcpRsp extends KVMAgentCommands.AgentResponse {
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
                info.ip = arg.getIp();
                info.isDefaultL3Network = arg.isDefaultL3Network();
                info.netmask = arg.getNetmask();
                info.mac = arg.getMac();
                info.dns = arg.getL3Network().getDns();
                info.l3NetworkUuid = arg.getL3Network().getUuid();
                info.bridgeName = l3Bridges.get(arg.getL3Network().getUuid());
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
                                cmd.dhcpServerIp = dhcpServerIp;
                                cmd.dhcpNetmask = dhcpNetmask;

                                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                                msg.setHostUuid(hostUuid);
                                msg.setNoStatusCheck(true);
                                msg.setCommand(cmd);
                                msg.setPath(PREPARE_DHCP_PATH);
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

                                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                                msg.setCommand(cmd);
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
}
