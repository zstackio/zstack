package org.zstack.network.l3;

import org.zstack.header.network.NetworkOperationContext;
import com.googlecode.ipv6.IPv6Address;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.header.message.*;
import org.zstack.header.network.IpAllocatedReason;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.stopwatch.StopWatch;

import javax.persistence.Tuple;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L3BasicNetwork implements L3Network {
    private static final CLogger logger = Utils.getLogger(L3BasicNetwork.class);
    private static final FieldPrinter printer = Utils.getFieldPrinter();

    @Autowired
    protected L3NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L3NetworkManager l3NwMgr;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected TagManager tagMgr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private NetworkServiceManager nsMgr;

    private L3NetworkVO self;

    protected String syncThreadName;

    public L3BasicNetwork(L3NetworkVO vo) {
        this.self = vo;
        syncThreadName = "l3-" + vo.getUuid();
    }

    protected L3NetworkVO getSelf() {
        return self;
    }

    protected L3NetworkInventory getSelfInventory() {
        return L3NetworkInventory.valueOf(getSelf());
    }

    private String getSyncId() {
        return String.format("operate-l3-%s", self.getUuid());
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    @Override
    public void deleteHook() {
    }

    private void setIpRangeSharedResource(String L3NetworkUuid, String IpRangeUuid) {
        SharedResourceVO l3NetworkSharedResource = Q.New(SharedResourceVO.class)
                .eq(SharedResourceVO_.resourceUuid, L3NetworkUuid)
                .eq(SharedResourceVO_.resourceType, L3NetworkVO.class.getSimpleName())
                .limit(1)
                .find();
        if (l3NetworkSharedResource == null) {
            return;
        }
        SharedResourceVO svo = new SharedResourceVO();
        svo.setOwnerAccountUuid(l3NetworkSharedResource.getOwnerAccountUuid());
        svo.setResourceType(IpRangeVO.class.getSimpleName());
        svo.setResourceUuid(IpRangeUuid);
        if (l3NetworkSharedResource.isToPublic()) {
            svo.setToPublic(l3NetworkSharedResource.isToPublic());
        } else {
            svo.setReceiverAccountUuid(l3NetworkSharedResource.getReceiverAccountUuid());
        }
        dbf.persistAndRefresh(svo);
    }

    private void handle(APIAddIpRangeMsg msg) {
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
        }
        AddIpRangeMsg add = AddIpRangeMsg.fromApi(msg);
        addIpRange(add, apiTagCreator(msg), new ReturnValueCompletion<IpRangeInventory>(msg) {
            @Override
            public void success(IpRangeInventory inv) {
                APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void addIpRange(AddIpRangeMsg msg, java.util.function.Consumer<String> tagCreator,
                            ReturnValueCompletion<IpRangeInventory> completion) {
        IpRangeInventory ipr = msg.getInventory();
        if (ipr == null || !Objects.equals(msg.getL3NetworkUuid(), ipr.getL3NetworkUuid())) {
            completion.fail(argerr(ORG_ZSTACK_NETWORK_L3_10101,
                    "internal IP range must belong to L3 network[uuid:%s]", msg.getL3NetworkUuid()));
            return;
        }
        addIpRange(ipr, msg, tagCreator, completion);
    }

    private void handle(AddIpRangeMsg msg) {
        addIpRange(msg, resourceUuid -> tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), resourceUuid,
                L3NetworkVO.class.getSimpleName()), new ReturnValueCompletion<IpRangeInventory>(msg) {
            @Override
            public void success(IpRangeInventory inv) {
                AddIpRangeReply reply = new AddIpRangeReply();
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                bus.replyErrorByMessageType(msg, errorCode);
            }
        });
    }

    private java.util.function.Consumer<String> apiTagCreator(APICreateMessage msg) {
        return resourceUuid -> tagMgr.createTagsFromAPICreateMessage(msg, resourceUuid,
                L3NetworkVO.class.getSimpleName());
    }

    private void addIpRange(IpRangeInventory ipr, AddIpRangeMsg msg,
                            java.util.function.Consumer<String> tagCreator,
                            ReturnValueCompletion<IpRangeInventory> completion) {
        addIpRanges(Collections.singletonList(ipr), msg, tagCreator,
                new ReturnValueCompletion<List<IpRangeInventory>>(completion) {
                    @Override
                    public void success(List<IpRangeInventory> inventories) {
                        completion.success(inventories.get(0));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void addIpRanges(List<IpRangeInventory> iprs, AddIpRangeMsg msg,
                             java.util.function.Consumer<String> tagCreator,
                             ReturnValueCompletion<List<IpRangeInventory>> completion) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                createIpRanges(iprs, msg, tagCreator, new ReturnValueCompletion<List<IpRangeInventory>>(completion) {
                    @Override
                    public void success(List<IpRangeInventory> invs) {
                        try {
                            completion.success(invs);
                        } finally {
                            chain.next();
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            completion.fail(errorCode);
                        } finally {
                            chain.next();
                        }
                    }
                });
            }

            @Override
            public String getSyncSignature() { return syncThreadName; }

            @Override
            public String getName() { return "add-ip-range"; }
        });
    }

    private void createIpRanges(List<IpRangeInventory> iprs, AddIpRangeMsg msg,
                                java.util.function.Consumer<String> tagCreator,
                                ReturnValueCompletion<List<IpRangeInventory>> completion) {
        for (IpRangeInventory ipr : iprs) {
            List<IpRangeVO> existing = Q.New(ipr.getIpVersion() == IPv6Constants.IPv6
                            ? NormalIpRangeVO.class : IpRangeVO.class)
                    .eq(IpRangeVO_.l3NetworkUuid, self.getUuid()).eq(IpRangeVO_.ipVersion, ipr.getIpVersion()).list();
            ErrorCode overlap = IpRangeHelper.checkOverlap(ipr, existing);
            if (overlap != null) {
                completion.fail(overlap);
                return;
            }
            ErrorCode errorCode = validateManagementNetworkIpRangeVersionBeforeCreate(ipr);
            if (errorCode != null) {
                completion.fail(errorCode);
                return;
            }
        }

        IpRangeFactory factory = l3NwMgr.getIpRangeFactory(iprs.get(0).getIpRangeType());
        factory.createIpRange(iprs, msg, new ReturnValueCompletion<List<IpRangeInventory>>(completion) {
            @Override
            public void success(List<IpRangeInventory> invs) {
                for (IpRangeInventory inv : invs) {
                    tagCreator.accept(inv.getL3NetworkUuid());
                    setIpRangeSharedResource(inv.getL3NetworkUuid(), inv.getUuid());
                }
                completion.success(invs);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private ErrorCode validateManagementNetworkIpRangeVersionBeforeCreate(IpRangeInventory ipr) {
        if (self.getCategory() != L3NetworkCategory.System) {
            return null;
        }

        int existingIpVersion = ipr.getIpVersion() == IPv6Constants.IPv4 ? IPv6Constants.IPv6 : IPv6Constants.IPv4;
        boolean hasExistingIpRange = Q.New(IpRangeVO.class)
                .eq(IpRangeVO_.l3NetworkUuid, self.getUuid())
                .eq(IpRangeVO_.ipVersion, existingIpVersion)
                .isExists();
        if (!hasExistingIpRange) {
            return null;
        }

        return argerr(ORG_ZSTACK_NETWORK_L3_10082,
                "management network l3[uuid:%s] cannot mix IPv4 and IPv6 IP ranges; existing IP version is %s, new IP version is %s",
                self.getUuid(), getIpVersionName(existingIpVersion), getIpVersionName(ipr.getIpVersion()));
    }

    private String getIpVersionName(int ipVersion) {
        return ipVersion == IPv6Constants.IPv6 ? "IPv6" : "IPv4";
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddIpRangeMsg) {
            handle((AddIpRangeMsg) msg);
        } else if (msg instanceof AllocateIpMsg) {
            handle((AllocateIpMsg)msg);
        } else if (msg instanceof ReturnIpMsg) {
            handle((ReturnIpMsg)msg);
        } else if (msg instanceof L3NetworkDeletionMsg) {
            handle((L3NetworkDeletionMsg) msg);
        } else if (msg instanceof IpRangeDeletionMsg) {
            handle((IpRangeDeletionMsg) msg);
        } else if (msg instanceof CheckIpAvailabilityMsg) {
            handle((CheckIpAvailabilityMsg) msg);
        } else if (msg instanceof AttachNetworkServiceToL3Msg) {
            handle((AttachNetworkServiceToL3Msg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CheckIpAvailabilityMsg msg) {
        CheckIpAvailabilityReply reply = checkIpAvailability(msg);
        bus.reply(msg, reply);
    }

    private boolean isLastNormalIpRangeOfVersion(String ipRangeUuid, int ipVersion) {
        List<IpRangeInventory> normalIpRanges = IpRangeHelper.getNormalIpRanges(self);
        normalIpRanges = normalIpRanges.stream()
                .filter(r -> r.getIpVersion() == ipVersion)
                .collect(Collectors.toList());
        return normalIpRanges.size() == 1 &&
                normalIpRanges.get(0).getUuid().equals(ipRangeUuid);
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, NetworkOperationContext> pendingDeleteIpRangeOperations =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, ErrorCode> pendingDeleteIpRangeErrors =
            new java.util.concurrent.ConcurrentHashMap<>();

    private void handle(IpRangeDeletionMsg msg) {
        NetworkOperationContext pendingOperation = pendingDeleteIpRangeOperations.remove(msg.getIpRangeUuid());
        if (pendingOperation != null) {
            msg.setNetworkOperation(pendingOperation);
        } else if (msg.getNetworkOperation().getOperationOrigin() == null) {
            msg.getNetworkOperation().setCascadeIpRangeDelete(Boolean.TRUE);
        }
        IpRangeDeletionReply reply = new IpRangeDeletionReply();
        doDeleteIpRange(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (Boolean.TRUE.equals(msg.getNetworkOperation().getStandaloneIpRangeDelete())) {
                    pendingDeleteIpRangeErrors.put(msg.getIpRangeUuid(), errorCode);
                }
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void doDeleteIpRange(IpRangeDeletionMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public void run(SyncTaskChain chainTask) {
                List<IpRangeDeletionExtensionPoint> exts = pluginRgty.getExtensionList(IpRangeDeletionExtensionPoint.class);
                IpRangeVO iprvo = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
                if (iprvo == null) {
                    completion.success();
                    chainTask.next();
                    return;
                }

                final IpRangeInventory inv = IpRangeInventory.valueOf(iprvo);
                final boolean isLastIpRange = isLastNormalIpRangeOfVersion(msg.getIpRangeUuid(), inv.getIpVersion());
                final SdnControllerL3 sdnL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());

                for (IpRangeDeletionExtensionPoint ext : exts) {
                    ext.preDeleteIpRange(inv);
                }

                CollectionUtils.safeForEach(exts, new ForEachFunction<IpRangeDeletionExtensionPoint>() {
                    @Override
                    public void run(IpRangeDeletionExtensionPoint arg) {
                        arg.beforeDeleteIpRange(inv);
                    }
                });

                FlowChain chain = new SimpleFlowChain();
                chain.setName(String.format("del-ip-range-%s", inv.getUuid()));
                chain.then(new NoRollbackFlow() {
                    String __name__ = "acquire-sdn-fence";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnL3 == null) {
                            trigger.next();
                            return;
                        }
                        sdnL3.beforeDeleteIpRange(inv, isLastIpRange, msg.getNetworkOperation(), new Completion(trigger) {
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
                }).then(new NoRollbackFlow() {
                    String __name__ = "disable-sdn-dhcp";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(self.getUuid());
                        if (sdnControllerUuid == null) {
                            trigger.next();
                            return;
                        }

                        SdnControllerDisableDHCPMsg dhcpMsg = new SdnControllerDisableDHCPMsg();
                        dhcpMsg.setL3NetworkUuid(self.getUuid());
                        dhcpMsg.setIpVersion(iprvo.getIpVersion());
                        dhcpMsg.setSdnControllerUuid(sdnControllerUuid);
                        dhcpMsg.setCheckIpRange(true);
                        dhcpMsg.setNetworkOperation(msg.getNetworkOperation().copyForRequest());
                        bus.makeTargetServiceIdByResourceUuid(dhcpMsg, SdnControllerConstant.SERVICE_ID, sdnControllerUuid);
                        bus.send(dhcpMsg, new CloudBusCallBack(trigger) {
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
                }).then(new NoRollbackFlow() {
                    String __name__ = "delete-ip-range";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnL3 == null) {
                            trigger.next();
                            return;
                        }

                        sdnL3.deleteIpRange(inv, isLastIpRange, msg.getNetworkOperation(), new Completion(trigger) {
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
                }).then(new NoRollbackFlow() {
                    String __name__ = "remove-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (sdnL3 != null) {
                                    sdnL3.beforeCommitDeleteIpRangeInDb(inv, msg.getNetworkOperation());
                                }
                                List<String> affectedNicUuids = q(UsedIpVO.class)
                                        .select(UsedIpVO_.vmNicUuid)
                                        .eq(UsedIpVO_.ipRangeUuid, iprvo.getUuid())
                                        .notNull(UsedIpVO_.vmNicUuid)
                                        .listValues();
                                SQL.New(UsedIpVO.class).eq(UsedIpVO_.ipRangeUuid, iprvo.getUuid()).hardDelete();
                                dbf.removeByPrimaryKeysInCurrentTransaction(
                                        Collections.singletonList(iprvo.getUuid()), IpRangeVO.class);
                                IpRangeHelper.updateL3NetworkIpversion(iprvo);
                                updateAffectedVmNicsPrimaryIp(affectedNicUuids, iprvo.getUuid());

                            }
                        }.execute();

                        CollectionUtils.safeForEach(exts, new ForEachFunction<IpRangeDeletionExtensionPoint>() {
                            @Override
                            public void run(IpRangeDeletionExtensionPoint arg) {
                                arg.afterDeleteIpRange(inv);
                            }
                        });
                        trigger.next();
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "after-delete-ip-range";

                    @Override
                    public void run (FlowTrigger trigger, Map data){
                        CollectionUtils.safeForEach(
                                pluginRgty.getExtensionList(AfterDeleteIpRangeExtensionPoint.class),
                                ext -> ext.afterDeleteIpRange(inv)
                        );
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    @Deferred
                    public void handle(ErrorCode errCode, Map data) {
                        try {
                            finishIpRangeDeletion(sdnL3, inv, msg.getNetworkOperation());
                            completion.fail(errCode);
                        } finally {
                            chainTask.next();
                        }
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    @Deferred
                    public void handle(Map data) {
                        try {
                            finishIpRangeDeletion(sdnL3, inv, msg.getNetworkOperation());
                            completion.success();
                        } finally {
                            chainTask.next();
                        }
                    }
                }).start();
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public String getName() {
                return "delete-ip-range";
            }
        });
    }

    private void updateAffectedVmNicsPrimaryIp(List<String> affectedNicUuids, String deletedIpRangeUuid) {
        if (affectedNicUuids == null || affectedNicUuids.isEmpty()) {
            return;
        }
        Set<String> nicUuids = affectedNicUuids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (nicUuids.isEmpty()) {
            return;
        }
        List<Tuple> remainingIpTuples = Q.New(UsedIpVO.class)
                .select(UsedIpVO_.vmNicUuid, UsedIpVO_.ipRangeUuid, UsedIpVO_.uuid, UsedIpVO_.ip,
                        UsedIpVO_.netmask, UsedIpVO_.gateway, UsedIpVO_.ipVersion, UsedIpVO_.l3NetworkUuid)
                .in(UsedIpVO_.vmNicUuid, nicUuids)
                .orderBy(UsedIpVO_.ipVersion, SimpleQuery.Od.ASC)
                .orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC)
                .listTuple();
        if (remainingIpTuples.isEmpty()) {
            return;
        }
        Map<String, Tuple> primaryIpByNicUuid = new LinkedHashMap<>();
        for (Tuple tuple : remainingIpTuples) {
            String vmNicUuid = tuple.get(0, String.class);
            String ipRangeUuid = tuple.get(1, String.class);
            if (vmNicUuid == null || Objects.equals(deletedIpRangeUuid, ipRangeUuid)) {
                continue;
            }
            primaryIpByNicUuid.putIfAbsent(vmNicUuid, tuple);
        }
        for (Map.Entry<String, Tuple> entry : primaryIpByNicUuid.entrySet()) {
            Tuple primaryIp = entry.getValue();
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, entry.getKey())
                    .set(VmNicVO_.usedIpUuid, primaryIp.get(2, String.class))
                    .set(VmNicVO_.ip, primaryIp.get(3, String.class))
                    .set(VmNicVO_.netmask, primaryIp.get(4, String.class))
                    .set(VmNicVO_.gateway, primaryIp.get(5, String.class))
                    .set(VmNicVO_.ipVersion, primaryIp.get(6, Integer.class))
                    .set(VmNicVO_.l3NetworkUuid, primaryIp.get(7, String.class))
                    .update();
        }
    }

    private void handle(L3NetworkDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                L3NetworkDeletionReply reply = new L3NetworkDeletionReply();
                L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
                L2NetworkVO l2NetworkVO = dbf.findByUuid(l3NetworkVO.getL2NetworkUuid(), L2NetworkVO.class);

                FlowChain fchain = new SimpleFlowChain();
                fchain.setName(String.format("del-l3-network-%s", msg.getL3NetworkUuid()));
                fchain.then(new NoRollbackFlow() {
                    String __name__ = "remove-from-sdn-controller";

                    @Override
                    @Deferred
                    public void run(FlowTrigger trigger, Map data) {
                        SdnControllerL3 controllerL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());
                        if (controllerL3 == null) {
                            trigger.next();
                            return;
                        }

                        controllerL3.deleteL3Network(L3NetworkInventory.valueOf(l3NetworkVO), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "remove-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                                .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
                        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.clusterUuid)
                                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkVO.getUuid()).listValues();
                        if (isExistSystemL3) {
                            if (clusterUuids != null && !clusterUuids.isEmpty()) {
                                for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                                    List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).in(HostVO_.clusterUuid, clusterUuids).listValues();
                                    if (l2NetworkVO.getType().equals(L2NetworkConstant.VXLAN_NETWORK_TYPE) || l2NetworkVO.getType().equals(L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE)) {
                                        ext.syncManagementServiceTypeExtensionPoint(hostUuids, "vxlan" + l2NetworkVO.getVirtualNetworkId(), null, true);
                                    }
                                    if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE) || l2NetworkVO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
                                        ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), true);
                                    }
                                }
                            }
                        }

                        if (!self.getReservedIpRanges().isEmpty()) {
                            SQL.New(ReservedIpRangeVO.class)
                                    .in(ReservedIpRangeVO_.uuid, self.getReservedIpRanges().stream().map(ReservedIpRangeVO::getUuid).collect(Collectors.toList()))
                                    .delete();
                        }

                        L3NetworkInventory inv = L3NetworkInventory.valueOf(self);
                        extpEmitter.beforeDelete(inv);
                        deleteHook();
                        extpEmitter.afterDelete(inv, msg.getCascadeParentIssuer());

                        trigger.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return "delete-l3-network-" + msg.getL3NetworkUuid();
            }
        });

    }

    private void handle(ReturnIpMsg msg) {
        ReturnIpReply reply = new ReturnIpReply();
        new Retry<Void>() {
            String __name__ = String.format("return-ip-%s-for-l3-%s", msg.getUsedIpUuid(), msg.getL3NetworkUuid());

            @Override
            @RetryCondition(times = 6)
            protected Void call() {
                String reserveRangeUuid = null;
                Tuple t = Q.New(UsedIpVO.class).select(UsedIpVO_.ip, UsedIpVO_.ipVersion)
                        .eq(UsedIpVO_.uuid, msg.getUsedIpUuid()).findTuple();
                if (t != null) {
                    String ip = t.get(0, String.class);
                    Integer ipVersion = t.get(1, Integer.class);
                    List<ReservedIpRangeVO> ranges = self.getReservedIpRanges()
                            .stream().filter(r -> r.getIpVersion() == ipVersion).collect(Collectors.toList());
                    for (ReservedIpRangeVO ripr : ranges) {
                        if (NetworkUtils.isInRange(ip, ripr.getStartIp(), ripr.getEndIp())) {
                            reserveRangeUuid = ripr.getUuid();
                            break;
                        }
                    }
                }

                if (reserveRangeUuid != null) {
                    SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, msg.getUsedIpUuid())
                            .set(UsedIpVO_.vmNicUuid, null)
                            .set(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                            .set(UsedIpVO_.metaData, reserveRangeUuid)
                            .update();
                } else {
                    SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, msg.getUsedIpUuid()).hardDelete();
                }

                return null;
            }
        }.run();
        logger.debug(String.format("Successfully released used ip[%s]", msg.getUsedIpUuid()));
        bus.reply(msg, reply);
    }

    private IpAllocatorType getIpAllocatorType(AllocateIpMsg msg) {
        if (msg.getAllocatorStrategy() != null) {
            return IpAllocatorType.valueOf(msg.getAllocatorStrategy());
        }

        if (msg.getIpVersion() == IPv6Constants.IPv4) {
            String ias = rcf.getResourceConfigValue(L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY, msg.getL3NetworkUuid(), String.class);
            if (ias != null) {
                return IpAllocatorType.valueOf(ias);
            }
            return RandomIpAllocatorStrategy.type;
        }

        String ias = rcf.getResourceConfigValue(L3NetworkGlobalConfig.IPV6_ALLOCATE_STRATEGY, msg.getL3NetworkUuid(), String.class);
        if (ias != null) {
            return IpAllocatorType.valueOf(ias);
        }
        return RandomIpv6AllocatorStrategy.type;
    }

    private void handle(AllocateIpMsg msg) {
        AllocateIpReply reply = new AllocateIpReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                IpAllocatorType strategyType = getIpAllocatorType(msg);
                IpAllocatorStrategy ias = l3NwMgr.getIpAllocatorStrategy(strategyType);
                UsedIpInventory ip = ias.allocateIp(msg);
                if (ip == null) {
                    String reason = msg.getRequiredIp() == null ?
                            String.format("no ip is available in this l3Network[name:%s, uuid:%s]", self.getName(), self.getUuid()) :
                            String.format("IP[%s] is not available", msg.getRequiredIp());
                    reply.setError(err(ORG_ZSTACK_NETWORK_L3_10078, L3Errors.ALLOCATE_IP_ERROR,
                            "IP allocator strategy[%s] failed, because %s", strategyType, reason));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                ip = overrideUsedIpIfNeeded(msg, ip);

                logger.debug(String.format("Ip allocator strategy[%s] successfully allocates an ip[%s]", strategyType, ip.getIp()));
                reply.setIpInventory(ip);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "allocate-ip-of-l3-" + msg.getL3NetworkUuid();
            }
        });
    }

    private UsedIpInventory overrideUsedIpIfNeeded(AllocateIpMsg msg, UsedIpInventory ip) {
        String overrideNetmask = null;
        String overrideGateway = null;
        Integer prefixLength = null;

        if (ip.getIpVersion() != null && ip.getIpVersion() == IPv6Constants.IPv4) {
            if (msg.getNetmask() != null) {
                overrideNetmask = msg.getNetmask();
            }
            if (msg.getGateway() != null) {
                overrideGateway = msg.getGateway();
            }
        } else if (ip.getIpVersion() != null && ip.getIpVersion() == IPv6Constants.IPv6) {
            if (msg.getIpv6Prefix() != null) {
                try {
                    prefixLength = Integer.parseInt(msg.getIpv6Prefix());
                    overrideNetmask = IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(ip.getIp() + "/" + msg.getIpv6Prefix());
                } catch (NumberFormatException e) {
                    logger.warn(String.format("failed to parse prefix length[%s], ignore it and use the default prefix length of the ip range",
                            msg.getIpv6Prefix()));
                }
            }
            if (msg.getIpv6Gateway() != null) {
                overrideGateway = msg.getIpv6Gateway().isEmpty() ? "" : IPv6NetworkUtils.getIpv6AddressCanonicalString(msg.getIpv6Gateway());
            }
        }

        if (overrideNetmask != null || overrideGateway != null) {
            UsedIpVO vo = dbf.findByUuid(ip.getUuid(), UsedIpVO.class);
            if (vo != null) {
                if (overrideNetmask != null) {
                    vo.setNetmask(overrideNetmask);
                }
                if (overrideGateway != null) {
                    vo.setGateway(overrideGateway);
                }
                if (prefixLength != null) {
                    vo.setPrefixLen(prefixLength);
                }
                vo = dbf.updateAndRefresh(vo);
                ip = UsedIpInventory.valueOf(vo);
            }
        }

        return ip;
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL3NetworkMsg) {
            handle((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteIpRangeMsg) {
            handle((APIDeleteIpRangeMsg)msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            handle((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            handle((APIAttachNetworkServiceToL3NetworkMsg) msg);
        } else if (msg instanceof APIDetachNetworkServiceFromL3NetworkMsg) {
            handle((APIDetachNetworkServiceFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddDnsToL3NetworkMsg) {
        	handle((APIAddDnsToL3NetworkMsg)msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            handle((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APIChangeL3NetworkStateMsg) {
            handle((APIChangeL3NetworkStateMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            handle((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            handle((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            handle((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIUpdateL3NetworkMsg) {
            handle((APIUpdateL3NetworkMsg) msg);
        } else if (msg instanceof APIGetFreeIpMsg) {
            handle((APIGetFreeIpMsg) msg);
        } else if (msg instanceof APIUpdateIpRangeMsg) {
            handle((APIUpdateIpRangeMsg) msg);
        } else if (msg instanceof APICheckIpAvailabilityMsg) {
            handle((APICheckIpAvailabilityMsg) msg);
        } else if (msg instanceof APISetL3NetworkRouterInterfaceIpMsg) {
            handle((APISetL3NetworkRouterInterfaceIpMsg) msg);
        } else if (msg instanceof APIGetL3NetworkRouterInterfaceIpMsg) {
            handle((APIGetL3NetworkRouterInterfaceIpMsg) msg);
        } else if (msg instanceof APIAddHostRouteToL3NetworkMsg) {
            handle((APIAddHostRouteToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveHostRouteFromL3NetworkMsg) {
            handle((APIRemoveHostRouteFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddReservedIpRangeMsg) {
            handle((APIAddReservedIpRangeMsg) msg);
        } else if (msg instanceof APIDeleteIpAddressMsg) {
            handle((APIDeleteIpAddressMsg) msg);
        } else if (msg instanceof APIDeleteReservedIpRangeMsg) {
            handle((APIDeleteReservedIpRangeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIAddReservedIpRangeMsg msg) {
        APIAddReservedIpRangeEvent event = new APIAddReservedIpRangeEvent(msg.getId());
        event.setInventory(createReservedIpRange(msg));
        bus.publish(event);
    }

    private ReservedIpRangeInventory createReservedIpRange(APIAddReservedIpRangeMsg msg) {
        /* step 1: create reservedIpRangeVO */
        ReservedIpRangeVO reservedIpRangeVO = new ReservedIpRangeVO();
        reservedIpRangeVO.setUuid(Platform.getUuid());
        reservedIpRangeVO.setL3NetworkUuid(msg.getL3NetworkUuid());
        reservedIpRangeVO.setStartIp(msg.getStartIp());
        reservedIpRangeVO.setEndIp(msg.getEndIp());
        if (NetworkUtils.isIpv4Address(msg.getStartIp())) {
            reservedIpRangeVO.setIpVersion(IPv6Constants.IPv4);
        } else {
            reservedIpRangeVO.setIpVersion(IPv6Constants.IPv6);
        }

        /* step 2: allocate usedIpVO */
        List<IpRangeVO> ipv4Ranges = self.getIpRanges().stream()
                .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4)
                .collect(Collectors.toList());
        List<IpRangeVO> ipv6Ranges = self.getIpRanges().stream()
                .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        List<UsedIpVO> usedIpVOS = new ArrayList<>();
        if(IPv6NetworkUtils.isValidIpv4(msg.getStartIp()) && !ipv4Ranges.isEmpty()) {
            long start = NetworkUtils.ipv4StringToLong(msg.getStartIp());
            long end  = NetworkUtils.ipv4StringToLong(msg.getEndIp());
            Comparator<IpRangeVO> ipv4Comparator
                    = Comparator.comparing(
                    IpRangeVO::getStartIp, (s1, s2) -> {
                        return NetworkUtils.compareIpv4Address(s1, s2);
                    });
            ipv4Ranges.sort(ipv4Comparator);
            Iterator<IpRangeVO> ip4 = ipv4Ranges.iterator();
            IpRangeVO ipr = ip4.next();

            for (long i = start; i <= end && ipr != null; i++) {
                String newIp = NetworkUtils.longToIpv4String(i);
                while (ipr != null && NetworkUtils.compareIpv4Address(newIp, ipr.getEndIp()) > 0) {
                    ipr = ip4.hasNext() ? ip4.next() : null;
                }
                if (ipr == null) {
                    break;
                }
                if (!NetworkUtils.isInRange(newIp, ipr.getStartIp(), ipr.getEndIp())) {
                    continue;
                }
                /* ip address is used, can not be reserved */
                if (Q.New(UsedIpVO.class)
                        .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(UsedIpVO_.ip, newIp).isExists()) {
                    continue;
                }

                UsedIpVO vo = new UsedIpVO();
                vo.setUuid(Platform.getUuid());
                vo.setIpRangeUuid(ipr.getUuid());
                vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                //vo.setVmNicUuid(nic.getUuid());
                vo.setIpVersion(ipr.getIpVersion());
                vo.setIp(newIp);
                vo.setNetmask(ipr.getNetmask());
                vo.setGateway(ipr.getGateway());
                vo.setIpInLong(i);
                vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                vo.setUsedFor(IpAllocatedReason.Reserved.toString());
                vo.setMetaData(reservedIpRangeVO.getUuid());

                usedIpVOS.add(vo);
            }
        } else if (IPv6NetworkUtils.isValidIpv6(msg.getStartIp()) && !ipv6Ranges.isEmpty()){
            BigInteger start = IPv6Address.fromString(msg.getStartIp()).toBigInteger();
            BigInteger end = IPv6Address.fromString(msg.getEndIp()).toBigInteger();
            Comparator<IpRangeVO> ipv6Comparator = Comparator.comparing(
                    IpRangeVO::getStartIp,
                    (s1, s2) -> IPv6Address.fromString(s1).toBigInteger()
                            .compareTo(IPv6Address.fromString(s2).toBigInteger()));
            ipv6Ranges.sort(ipv6Comparator);
            Iterator<IpRangeVO> ip6 = ipv6Ranges.iterator();
            IpRangeVO ipr = ip6.next();

            for (BigInteger i = start; i.compareTo(end) <= 0 && ipr != null; i = i.add(BigInteger.ONE)) {
                String newIp = IPv6NetworkUtils.IPv6AddressToString(i);
                while (ipr != null && i.compareTo(IPv6Address.fromString(ipr.getEndIp()).toBigInteger()) > 0) {
                    ipr = ip6.hasNext() ? ip6.next() : null;
                }
                if (ipr == null) {
                    break;
                }
                if (!IPv6NetworkUtils.isIpv6InRange(newIp, ipr.getStartIp(), ipr.getEndIp())) {
                    continue;
                }
                /* ip address is used, can not be reserved */
                if (Q.New(UsedIpVO.class)
                        .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .eq(UsedIpVO_.ip, newIp).isExists()) {
                    continue;
                }

                UsedIpVO vo = new UsedIpVO();
                vo.setUuid(Platform.getUuid());
                vo.setIpRangeUuid(ipr.getUuid());
                vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                //vo.setVmNicUuid(nic.getUuid());
                vo.setIpVersion(ipr.getIpVersion());
                vo.setIp(newIp);
                vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                vo.setNetmask(ipr.getNetmask());
                vo.setGateway(ipr.getGateway());
                vo.setUsedFor(IpAllocatedReason.Reserved.toString());
                vo.setMetaData(reservedIpRangeVO.getUuid());

                usedIpVOS.add(vo);
            }
        }

        return new SQLBatchWithReturn<ReservedIpRangeInventory>() {
            @Override
            protected ReservedIpRangeInventory scripts() {
                persist(reservedIpRangeVO);
                if (!usedIpVOS.isEmpty()) {
                    StopWatch watch = Utils.getStopWatch();
                    watch.start();
                    for (UsedIpVO vo : usedIpVOS) {
                        persist(vo);
                    }
                    flush();
                    watch.stop();
                    logger.debug(String.format("it takes %d microseconds to save %d ip addresses", watch.getLapse(), usedIpVOS.size()));
                }

                tagMgr.createTags(Collections.emptyList(), msg.getUserTags(), reservedIpRangeVO.getUuid(),
                        ReservedIpRangeVO.class.getSimpleName());
                return ReservedIpRangeInventory.valueOf(reload(reservedIpRangeVO));
            }
        }.execute();
    }

    private void handle(APIDeleteReservedIpRangeMsg msg) {
        APIDeleteIpAddressEvent event = new APIDeleteIpAddressEvent(msg.getId());

        SQL.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .eq(UsedIpVO_.metaData, msg.getIpRangeUuid())
                .delete();

        SQL.New(ReservedIpRangeVO.class).eq(ReservedIpRangeVO_.uuid, msg.getIpRangeUuid()).delete();

        bus.publish(event);
    }

    private void handle(APIDeleteIpAddressMsg msg) {
        APIDeleteIpAddressEvent event = new APIDeleteIpAddressEvent(msg.getId());

        SQL.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .in(UsedIpVO_.uuid, msg.getUsedIpUuids())
                .isNull(UsedIpVO_.vmNicUuid)
                .delete();

        bus.publish(event);
    }

    @Override
    public CheckIpAvailabilityReply checkIpAvailability(CheckIpAvailabilityMsg msg) {
        CheckIpAvailabilityReply reply = new CheckIpAvailabilityReply();
        String ip = msg.getIp();
        final int ipversion = IPv6NetworkUtils.isIpv6Address(ip) ? IPv6Constants.IPv6 : IPv6Constants.IPv4;
        if (ipversion == IPv6Constants.IPv6) {
            ip = IPv6NetworkUtils.getIpv6AddressCanonicalString(ip);
        }
        SimpleQuery<IpRangeVO> rq = dbf.createQuery(IpRangeVO.class);
        rq.select(IpRangeVO_.startIp, IpRangeVO_.endIp, IpRangeVO_.gateway);
        rq.add(IpRangeVO_.l3NetworkUuid, Op.EQ, self.getUuid());
        rq.add(IpRangeVO_.ipVersion, Op.EQ, ipversion);
        List<Tuple> ts = rq.listTuple();

        List<String> addressPoolGateways = Q.New(AddressPoolVO.class)
                .select(AddressPoolVO_.gateway)
                .eq(AddressPoolVO_.l3NetworkUuid,self.getUuid()).listValues();

        boolean inRange = false;
        boolean isGateway = false;
        /* disable ip range check */
        if (!msg.getIpRangeCheck()) {
            inRange = true;
        }

        if (!self.enableIpAddressAllocation()) {
            inRange = true;
        }

        if (ts.isEmpty()) {
            inRange = true;
        } else {
            for (Tuple t : ts) {
                String sip = t.get(0, String.class);
                String eip = t.get(1, String.class);
                String gw = t.get(2, String.class);
                if (ipversion == IPv6Constants.IPv6 && gw != null) {
                    gw = IPv6NetworkUtils.getIpv6AddressCanonicalString(gw);
                }
                if (ip.equals(gw) && !addressPoolGateways.contains(gw)) {
                    isGateway = true;
                    break;
                }

                if (NetworkUtils.isInRange(ip, sip, eip)) {
                    inRange = true;
                    break;
                }
            }
        }

        if (!inRange || isGateway) {
            // not an IP of this L3 or is a gateway
            reply.setAvailable(false);
            if (isGateway) {
                reply.setReason(IpNotAvailabilityReason.GATEWAY.toString());
            } else {
                reply.setReason(IpNotAvailabilityReason.NO_IN_RANGE.toString());
            }
        } else {
            SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
            q.add(UsedIpVO_.l3NetworkUuid, Op.EQ, self.getUuid());
            q.add(UsedIpVO_.ip, Op.EQ, ip);
            if (q.isExists()) {
                reply.setAvailable(false);
                reply.setReason(IpNotAvailabilityReason.USED.toString());
            } else {
                reply.setAvailable(true);
            }
        }
        return reply;
    }

    private void handle(APICheckIpAvailabilityMsg msg) {
        APICheckIpAvailabilityReply reply = new APICheckIpAvailabilityReply();
        reply.setAvailable(true);
        reply.setReason("");
        CheckIpAvailabilityMsg imsg = new CheckIpAvailabilityMsg();
        imsg.setL3NetworkUuid(msg.getL3NetworkUuid());
        imsg.setIp(msg.getIp());
        imsg.setArpCheck(msg.getArpCheck());
        imsg.setIpRangeCheck(msg.getIpRangeCheck());

        FlowChain flowChain = new SimpleFlowChain();
        flowChain.setName(String.format("check-ip-address-availability-%s-%s",
                msg.getL3NetworkUuid(), msg.getIp()));
        for (CheckIpAddressAvailabilityExtensionPoint exp : pluginRgty.getExtensionList(CheckIpAddressAvailabilityExtensionPoint.class)) {
            flowChain.then(new NoRollbackFlow() {
                String __name__ = "check-ip-address-availability-" + exp.getClass().getSimpleName();

                @Override
                public boolean skip(Map data) {
                    return !reply.isAvailable();
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    exp.check(imsg, new ReturnValueCompletion<CheckIpAvailabilityReply>(trigger) {
                        @Override
                        public void success(CheckIpAvailabilityReply r) {
                            if (!r.isAvailable()) {
                                reply.setAvailable(r.isAvailable());
                                reply.setReason(r.getReason());
                                trigger.next();
                            } else {
                                trigger.next();
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.next();
                        }
                    });
                }
            });
        }
        flowChain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(final APIGetL3NetworkRouterInterfaceIpMsg msg) {
        APIGetL3NetworkRouterInterfaceIpReply reply = new APIGetL3NetworkRouterInterfaceIpReply();
        if (L3NetworkSystemTags.ROUTER_INTERFACE_IP.hasTag(msg.getL3NetworkUuid())) {
            reply.setRouterInterfaceIp(L3NetworkSystemTags.ROUTER_INTERFACE_IP.getTokenByResourceUuid(msg.getL3NetworkUuid(), L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN));
            bus.reply(msg, reply);
            return;
        }

        /* this api only support ipv4 */
        List<NormalIpRangeVO> ipRangeVOS = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
        if (ipRangeVOS == null || ipRangeVOS.isEmpty()) {
            reply.setRouterInterfaceIp(null);
            bus.reply(msg, reply);
        } else {
            reply.setRouterInterfaceIp(ipRangeVOS.get(0).getGateway());
            bus.reply(msg, reply);
        }
    }

    private void handle(final APISetL3NetworkRouterInterfaceIpMsg msg) {
        APISetL3NetworkRouterInterfaceIpEvent event = new APISetL3NetworkRouterInterfaceIpEvent(msg.getId());
        L3NetworkSystemTags.ROUTER_INTERFACE_IP.delete(msg.getRouterInterfaceIp());

        SystemTagCreator creator = L3NetworkSystemTags.ROUTER_INTERFACE_IP.newSystemTagCreator(msg.getL3NetworkUuid());
        creator.ignoreIfExisting = false;
        creator.inherent = false;
        creator.setTagByTokens(
                map(
                        e(L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN, msg.getRouterInterfaceIp())
                )
        );
        creator.create();

        bus.publish(event);
    }

    private void detachNetworkServiceFromL3NetworkMsg(L3NetworkVO l3VO, List<NetworkServiceL3NetworkRefVO> refs, Completion completion) {
        detachNetworkServiceFromL3NetworkMsg(l3VO, refs, new NetworkOperationContext(), false, completion);
    }

    private void detachNetworkServiceFromL3NetworkMsg(L3NetworkVO l3VO, List<NetworkServiceL3NetworkRefVO> refs,
                                                      NetworkOperationContext operation, boolean failOnError, Completion completion) {
        List<NetworkServiceProviderVO> networkServiceProviderVOS = Q.New(NetworkServiceProviderVO.class).list();
        Map<String, String> providerUuidTypeMap = new HashMap<>();
        for (NetworkServiceProviderVO providerVO : networkServiceProviderVOS) {
            providerUuidTypeMap.put(providerVO.getUuid(), providerVO.getType());
        }

        new While<>(refs).each((ref, wcomp) -> {
            NetworkServiceProviderType pType = NetworkServiceProviderType.valueOf(providerUuidTypeMap.get(ref.getNetworkServiceProviderUuid()));
            NetworkServiceType nsType = NetworkServiceType.valueOf(ref.getNetworkServiceType());

            nsMgr.disableNetworkService(l3VO, pType, nsType, operation, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (failOnError) {
                        wcomp.addError(errorCode);
                        wcomp.allDone();
                    } else {
                        wcomp.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (failOnError && !errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }
    
    private void handle(APIDetachNetworkServiceFromL3NetworkMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIDetachNetworkServiceFromL3NetworkEvent evt = new APIDetachNetworkServiceFromL3NetworkEvent(msg.getId());

                List<NetworkServiceL3NetworkRefVO> refs = new ArrayList<>();
                L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);

                for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
                    SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
                    q.add(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid, Op.EQ, e.getKey());
                    q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, self.getUuid());
                    q.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.IN, e.getValue());
                    refs.addAll(q.list());
                }

                if (refs.isEmpty()) {
                    self = dbf.reload(self);
                    evt.setInventory(L3NetworkInventory.valueOf(self));
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                ReturnValueCompletion<L3NetworkInventory> completion = new ReturnValueCompletion<L3NetworkInventory>(msg, chain) {
                    @Override
                    public void success(L3NetworkInventory inventory) {
                        try {
                            evt.setInventory(inventory);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            evt.setError(errorCode);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }
                };
                detachNetworkServicesLocally(l3VO, refs, msg, completion);
            }

            @Override
            public String getName() {
                return "detach-network-service-from-l3-" + msg.getL3NetworkUuid();
            }
        });
    }

    private void detachNetworkServicesLocally(L3NetworkVO l3VO,
                                               List<NetworkServiceL3NetworkRefVO> refs,
                                               APIDetachNetworkServiceFromL3NetworkMsg msg,
                                               ReturnValueCompletion<L3NetworkInventory> completion) {
        SdnControllerL3 sdnL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());
        Runnable runDetach = () -> detachNetworkServiceFromL3NetworkMsg(l3VO, refs, msg.getNetworkOperation(), true, new Completion(completion) {
            @Override
            public void success() {
                ErrorCode commitError = null;
                try {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            if (sdnL3 != null) {
                                sdnL3.beforeCommitNetworkServiceInDb(getSelfInventory(), msg.getNetworkOperation());
                            }
                            for (NetworkServiceL3NetworkRefVO ref : refs) {
                                NetworkServiceL3NetworkRefVO managed = dbf.getEntityManager().find(
                                        NetworkServiceL3NetworkRefVO.class, ref.getId());
                                if (managed != null) {
                                    remove(managed);
                                }
                            }
                        }
                    }.execute();
                } catch (OperationFailureException e) {
                    commitError = e.getErrorCode();
                } catch (RuntimeException e) {
                    commitError = errf.throwableToInternalError(e);
                } finally {
                    finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                }
                if (commitError != null) {
                    completion.fail(commitError);
                    return;
                }
                self = dbf.reload(self);
                completion.success(L3NetworkInventory.valueOf(self));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                completion.fail(errorCode);
            }
        });
        if (sdnL3 == null) {
            runDetach.run();
            return;
        }
        sdnL3.beforeChangeNetworkService(getSelfInventory(), msg.getNetworkServices(), false, msg.getNetworkOperation(), new Completion(completion) {
            @Override
            public void success() {
                runDetach.run();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                completion.fail(errorCode);
            }
        });
    }

    private void handle(APIUpdateIpRangeMsg msg) {
        IpRangeVO vo = dbf.findByUuid(msg.getUuid(), IpRangeVO.class);
        IpRangeInventory oldIpr = IpRangeInventory.valueOf(vo);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        IpRangeInventory newIpr = IpRangeInventory.valueOf(vo);
        for (AfterUpdateIpRangeExtensionPoint ext : pluginRgty.getExtensionList(AfterUpdateIpRangeExtensionPoint.class)) {
            ext.afterUpdateIpRange(oldIpr, newIpr);
        }

        APIUpdateIpRangeEvent evt = new APIUpdateIpRangeEvent(msg.getId());
        evt.setInventory(newIpr);
        bus.publish(evt);
    }

    private void handle(APIGetFreeIpMsg msg) {
        APIGetFreeIpReply reply = new APIGetFreeIpReply();

        List<IpRangeVO> ipRangeVOs = new ArrayList<>();
        List<Integer> ipVersions = new ArrayList<>();
        if(msg.getIpVersion()!=null){
            if(msg.getIpVersion()== IPv6Constants.DUAL_STACK){
                ipVersions.add(IPv6Constants.IPv4);
                ipVersions.add(IPv6Constants.IPv6);
            }else{
                ipVersions.add(msg.getIpVersion());
            }
        }

        if (msg.getIpRangeUuid() != null) {
            IpRangeVO ipr = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
            ipRangeVOs.add(ipr);
        } else {
            if (msg.getIpRangeType() == null) {
                List<IpRangeVO> tempIpRangeVO = Q.New(IpRangeVO.class)
                        .eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(IpRangeVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);
            } else if (msg.getIpRangeType().equals(IpRangeType.Normal.toString())) {
                List<IpRangeVO> tempIpRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(NormalIpRangeVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);

            } else {
                List<IpRangeVO> tempIpRangeVO = Q.New(AddressPoolVO.class)
                        .eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(AddressPoolVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);
            }
        }

        List<FreeIpInventory> freeIpInventorys = new ArrayList<FreeIpInventory>();
        int limit = msg.getLimit();
        String start = msg.getStart();
        for ( IpRangeVO ipRangeVO : ipRangeVOs) {
            if( msg.getStart() == null){
                if ( ipRangeVO.getIpVersion() == IPv6Constants.IPv6) {
                    start = "::";
                } else {
                    start = "0.0.0.0";
                }
            }
            List<FreeIpInventory> tempFreeIpInventorys = IpRangeHelper.getFreeIp(ipRangeVO, limit,start);
            freeIpInventorys.addAll(tempFreeIpInventorys);
            if (freeIpInventorys.size() >= msg.getLimit()) {
                break;
            }
            limit -= freeIpInventorys.size();
        }

        Set<ReservedIpRangeVO> reservedIpRanges = self.getReservedIpRanges();
        if (reservedIpRanges != null && !reservedIpRanges.isEmpty()) {
            freeIpInventorys.removeIf(freeIp -> reservedIpRanges.stream().anyMatch(
                    r -> NetworkUtils.isInRange(freeIp.getIp(), r.getStartIp(), r.getEndIp())));
        }

        reply.setInventories(freeIpInventorys);

        bus.reply(msg, reply);
    }

    private void handle(APIUpdateL3NetworkMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getSystem() != null && msg.getCategory() != null) {
                self.setSystem(msg.getSystem());
                self.setCategory(L3NetworkCategory.valueOf(msg.getCategory()));
                update = true;
        }
        if (msg.getDnsDomain() != null) {
            self.setDnsDomain(msg.getDnsDomain());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateL3NetworkEvent evt = new APIUpdateL3NetworkEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIAddIpRangeByNetworkCidrMsg msg) {
        List<IpRangeInventory> iprs = IpRangeInventory.fromMessage(msg);
        assignStableIpRangeUuids(iprs, msg.getResourceUuid());
        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        AddIpRangeMsg add = AddIpRangeMsg.fromApi(msg, iprs.get(0));

        addIpRanges(iprs, add, apiTagCreator(msg),
                new ReturnValueCompletion<List<IpRangeInventory>>(msg) {
            @Override
            public void success(List<IpRangeInventory> invs) {
                IpRangeInventory inv = invs.get(0);
                evt.setInventory(inv);
                evt.setInventories(invs);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddIpv6RangeByNetworkCidrMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        if (ipr.getUuid() == null) {
            ipr.setUuid(Platform.getUuid());
        }
        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        AddIpRangeMsg add = AddIpRangeMsg.fromApi(msg, ipr);

        addIpRanges(Collections.singletonList(ipr), add, apiTagCreator(msg),
                new ReturnValueCompletion<List<IpRangeInventory>>(msg) {
            @Override
            public void success(List<IpRangeInventory> invs) {
                IpRangeInventory inv = invs.get(0);
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddIpv6RangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        if (ipr.getUuid() == null) {
            ipr.setUuid(Platform.getUuid());
        }
        APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
        AddIpRangeMsg add = AddIpRangeMsg.fromApi(msg, ipr);

        addIpRanges(Collections.singletonList(ipr), add, apiTagCreator(msg),
                new ReturnValueCompletion<List<IpRangeInventory>>(msg) {
            @Override
            public void success(List<IpRangeInventory> invs) {
                IpRangeInventory inv = invs.get(0);
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private static void assignStableIpRangeUuids(List<IpRangeInventory> iprs,
                                                 String requestedUuid) {
        for (int i = 0; i < iprs.size(); i++) {
            if (iprs.get(i).getUuid() == null) {
                iprs.get(i).setUuid(i == 0 && requestedUuid != null
                        ? requestedUuid : Platform.getUuid());
            }
        }
    }

    private void handle(APIChangeL3NetworkStateMsg msg) {
        if (L3NetworkStateEvent.enable.toString().equals(msg.getStateEvent())) {
            self.setState(L3NetworkState.Enabled);
        } else {
            self.setState(L3NetworkState.Disabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeL3NetworkStateEvent evt = new APIChangeL3NetworkStateEvent(msg.getId());
        evt.setInventory(L3NetworkInventory.valueOf(self));
        bus.publish(evt);
    }

    private static String normalizeDns(String dns) {
        if (dns != null && IPv6NetworkUtils.isIpv6Address(dns)) {
            return IPv6Address.fromString(dns).toString();
        }
        return dns;
    }

    private List<L3NetworkDnsVO> findMatchingDnsVOs(String l3NetworkUuid, String targetDns) {
        String normalizedTarget = normalizeDns(targetDns);
        List<L3NetworkDnsVO> all = Q.New(L3NetworkDnsVO.class)
                .eq(L3NetworkDnsVO_.l3NetworkUuid, l3NetworkUuid)
                .list();
        List<L3NetworkDnsVO> matches = new ArrayList<>();
        for (L3NetworkDnsVO vo : all) {
            if (Objects.equals(normalizeDns(vo.getDns()), normalizedTarget)) {
                matches.add(vo);
            }
        }
        return matches;
    }

    private void handle(final APIRemoveDnsFromL3NetworkMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                msg.setDns(normalizeDns(msg.getDns()));
                final APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());
                ReturnValueCompletion<L3NetworkInventory> completion = new ReturnValueCompletion<L3NetworkInventory>(msg, chain) {
                    @Override
                    public void success(L3NetworkInventory inventory) {
                        try {
                            evt.setInventory(inventory);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            evt.setError(errorCode);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }
                };
                removeDns(msg, completion);
            }

            @Override
            public String getName() {
                return String.format("remove-dns-%s-from-l3-%s", msg.getDns(), msg.getL3NetworkUuid());
            }
        });
    }

    private void removeDns(final APIRemoveDnsFromL3NetworkMsg msg,
                           ReturnValueCompletion<L3NetworkInventory> completion) {

        SdnControllerL3 sdnL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());
        SdnControllerDhcp sdnDhcp = l3NwMgr.getSdnControllerDhcp(msg.getL3NetworkUuid());
        final boolean remoteWriteFirst = sdnL3 != null && sdnL3.isRemoteWriteFirst();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-dns-%s-from-l3-%s", msg.getDns(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                Flow acquireFenceFlow = new NoRollbackFlow() {
                    String __name__ = "acquire-sdn-fence";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnL3 == null) {
                            trigger.next();
                            return;
                        }
                        sdnL3.beforeChangeDns(getSelfInventory(), msg.getDns(), false, msg.getNetworkOperation(), new Completion(trigger) {
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
                };

                Flow removeDnsFromSdnFlow = new NoRollbackFlow() {
                    String __name__ = "remove-dns-from-sdn";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(self.getUuid());
                        if (sdnControllerUuid == null) {
                            trigger.next();
                            return;
                        }

                        SdnControllerUpdateDHCPMsg dmsg = new SdnControllerUpdateDHCPMsg();
                        if (NetworkUtils.isIpv4Address(msg.getDns())) {
                            dmsg.setIpVersion(IPv6Constants.IPv4);
                        } else {
                            dmsg.setIpVersion(IPv6Constants.IPv6);
                        }
                        dmsg.setL3NetworkUuid(self.getUuid());
                        dmsg.setSdnControllerUuid(sdnControllerUuid);
                        dmsg.setNetworkOperation(msg.getNetworkOperation().copyForRequest());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, SdnControllerConstant.SERVICE_ID, sdnControllerUuid);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
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
                };

                Flow removeDnsFromDbFlow = new NoRollbackFlow() {
                    String __name__ = "remove-dns-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (sdnL3 != null) {
                                    sdnL3.beforeCommitDnsInDb(getSelfInventory(), msg.getNetworkOperation());
                                }
                                for (L3NetworkDnsVO dns : findMatchingDnsVOs(msg.getL3NetworkUuid(), msg.getDns())) {
                                    L3NetworkDnsVO managed = dbf.getEntityManager().find(L3NetworkDnsVO.class, dns.getId());
                                    if (managed != null) {
                                        remove(managed);
                                    }
                                }
                            }
                        }.execute();
                        trigger.next();
                    }
                };

                flow(acquireFenceFlow);
                if (remoteWriteFirst) {
                    flow(removeDnsFromSdnFlow);
                    flow(removeDnsFromDbFlow);
                } else {
                    flow(removeDnsFromDbFlow);
                    flow(removeDnsFromSdnFlow);
                }

                if (L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(self.getType()) && !self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "remove-dns-from-backend";

                        @Override
                        public boolean skip(Map data) {
                            return sdnDhcp != null;
                        }

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            RemoveDnsMsg rmsg = new RemoveDnsMsg();
                            rmsg.setDns(msg.getDns());
                            rmsg.setL3NetworkUuid(self.getUuid());
                            bus.makeLocalServiceId(rmsg, NetworkServiceConstants.DNS_SERVICE_ID);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
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
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        finishDnsChange(sdnL3, msg.getNetworkOperation());
                        completion.success(L3NetworkInventory.valueOf(dbf.reload(self)));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        finishDnsChange(sdnL3, msg.getNetworkOperation());
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIAddDnsToL3NetworkMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                msg.setDns(normalizeDns(msg.getDns()));
                final APIAddDnsToL3NetworkEvent evt = new APIAddDnsToL3NetworkEvent(msg.getId());
                if (!findMatchingDnsVOs(msg.getL3NetworkUuid(), msg.getDns()).isEmpty()) {
                    evt.setError(Platform.operr(
                            NetworkUtils.isIpv4Address(msg.getDns()) ? ORG_ZSTACK_NETWORK_L3_10067 : ORG_ZSTACK_NETWORK_L3_10068,
                            "there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), msg.getL3NetworkUuid()));
                    bus.publish(evt);
                    chain.next();
                    return;
                }
                ReturnValueCompletion<L3NetworkInventory> completion = new ReturnValueCompletion<L3NetworkInventory>(msg, chain) {
                    @Override
                    public void success(L3NetworkInventory inventory) {
                        try {
                            evt.setInventory(inventory);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            evt.setError(errorCode);
                            bus.publish(evt);
                        } finally {
                            chain.next();
                        }
                    }
                };
                addDns(msg, completion);
            }

            @Override
            public String getName() {
                return String.format("add-dns-%s-to-l3-%s", msg.getDns(), msg.getL3NetworkUuid());
            }
        });
    }

    private void addDns(final APIAddDnsToL3NetworkMsg msg,
                        ReturnValueCompletion<L3NetworkInventory> completion) {

        SdnControllerL3 sdnL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());
        SdnControllerDhcp sdnDhcp = l3NwMgr.getSdnControllerDhcp(msg.getL3NetworkUuid());
        final boolean remoteWriteFirst = sdnL3 != null && sdnL3.isRemoteWriteFirst();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-dns-%s-to-l3-%s", msg.getDns(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                Flow acquireFenceFlow = new NoRollbackFlow() {
                    String __name__ = "acquire-sdn-fence";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnL3 == null) {
                            trigger.next();
                            return;
                        }
                        sdnL3.beforeChangeDns(getSelfInventory(), msg.getDns(), true, msg.getNetworkOperation(), new Completion(trigger) {
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
                };

                Flow applyToSdnFlow = new NoRollbackFlow() {
                    String __name__ = "apply-to-sdn";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(self.getUuid());
                        if (sdnControllerUuid == null) {
                            trigger.next();
                            return;
                        }

                        SdnControllerUpdateDHCPMsg dmsg = new SdnControllerUpdateDHCPMsg();
                        if (NetworkUtils.isIpv4Address(msg.getDns())) {
                            dmsg.setIpVersion(IPv6Constants.IPv4);
                        } else {
                            dmsg.setIpVersion(IPv6Constants.IPv6);
                        }
                        dmsg.setL3NetworkUuid(self.getUuid());
                        dmsg.setSdnControllerUuid(sdnControllerUuid);
                        dmsg.setNetworkOperation(msg.getNetworkOperation().copyForRequest());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, SdnControllerConstant.SERVICE_ID, sdnControllerUuid);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
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
                };

                Flow writeDnsToDbFlow = new Flow() {
                    String __name__ = "write-dns-to-db";

                    L3NetworkDnsVO dnsvo;
                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (sdnL3 != null) {
                                    sdnL3.beforeCommitDnsInDb(getSelfInventory(), msg.getNetworkOperation());
                                }
                                if (!findMatchingDnsVOs(self.getUuid(), msg.getDns()).isEmpty()) {
                                    throw new OperationFailureException(Platform.operr(
                                            NetworkUtils.isIpv4Address(msg.getDns()) ? ORG_ZSTACK_NETWORK_L3_10067 : ORG_ZSTACK_NETWORK_L3_10068,
                                            "there has been a DNS[%s] on L3 network[uuid:%s]", msg.getDns(), self.getUuid()));
                                }
                                dnsvo = new L3NetworkDnsVO();
                                dnsvo.setDns(msg.getDns());
                                dnsvo.setL3NetworkUuid(self.getUuid());
                                persist(dnsvo);
                            }
                        }.execute();
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            dbf.remove(dnsvo);
                        }
                        trigger.rollback();
                    }
                };

                flow(acquireFenceFlow);
                if (remoteWriteFirst) {
                    flow(applyToSdnFlow);
                    flow(writeDnsToDbFlow);
                } else {
                    flow(writeDnsToDbFlow);
                    flow(applyToSdnFlow);
                }

                if (L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(self.getType()) && !self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "apply-to-backend";

                        @Override
                        public boolean skip(Map data) {
                            return sdnDhcp != null;
                        }

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AddDnsMsg amsg = new AddDnsMsg();
                            amsg.setL3NetworkUuid(self.getUuid());
                            amsg.setDns(msg.getDns());
                            bus.makeLocalServiceId(amsg, NetworkServiceConstants.DNS_SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        finishDnsChange(sdnL3, msg.getNetworkOperation());
                        self = dbf.reload(self);
                        logger.debug(String.format("successfully added dns[%s] to L3Network[uuid:%s, name:%s]", msg.getDns(), self.getUuid(), self.getName()));
                        completion.success(L3NetworkInventory.valueOf(self));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        finishDnsChange(sdnL3, msg.getNetworkOperation());
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final AttachNetworkServiceToL3Msg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doAttachNetworkServiceToL3(msg, chain);
            }

            @Override
            public String getName() {
                return "attach-network-service-to-l3-" + msg.getL3NetworkUuid();
            }
        });
    }

    private void doAttachNetworkServiceToL3(final AttachNetworkServiceToL3Msg msg, final SyncTaskChain chain) {
        MessageReply reply = new MessageReply();
        Runnable replyAndNext = () -> {
            try {
                bus.reply(msg, reply);
            } finally {
                chain.next();
            }
        };
        L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        List<NetworkServiceProviderVO> networkServiceProviderVOS = Q.New(NetworkServiceProviderVO.class).list();
        Map<String, String> providerUuidTypeMap = new HashMap<>();
        for (NetworkServiceProviderVO providerVO : networkServiceProviderVOS) {
            providerUuidTypeMap.put(providerVO.getUuid(), providerVO.getType());
        }

        List<NetworkServiceL3NetworkRefVO> refVOS = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
            for (String nsType : e.getValue()) {
                NetworkServiceL3NetworkRefVO ref = new NetworkServiceL3NetworkRefVO();
                ref.setL3NetworkUuid(self.getUuid());
                ref.setNetworkServiceProviderUuid(e.getKey());
                ref.setNetworkServiceType(nsType);
                refVOS.add(ref);
            }
            logger.debug(String.format("successfully attached network service provider[uuid:%s] to l3network[uuid:%s, name:%s] with services%s", e.getKey(), self.getUuid(), self.getName(), e.getValue()));
        }

        SdnControllerL3 sdnL3 = l3NwMgr.getSdnControllerL3(self.getL2NetworkUuid());
        final boolean remoteWriteFirst = sdnL3 != null && sdnL3.isRemoteWriteFirst();
        if (!remoteWriteFirst) {
            dbf.persistCollection(refVOS);
            new While<>(refVOS).each((ref, wcomp) -> {
                String provideType = providerUuidTypeMap.get(ref.getNetworkServiceProviderUuid());
                NetworkServiceProviderType ptype = NetworkServiceProviderType.valueOf(provideType);
                NetworkServiceType nsType = null;
                try {
                    nsType = NetworkServiceType.valueOf(ref.getNetworkServiceType());
                } catch (Exception e) {
                    logger.debug(e.toString());
                    wcomp.done();
                    return;
                }

                nsMgr.enableNetworkService(l3VO, ptype, nsType, msg.getApiSystemTags(), msg.getNetworkOperation(), new Completion(wcomp) {
                    @Override
                    public void success() {
                        wcomp.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        wcomp.addError(errorCode);
                        wcomp.allDone();
                    }
                });
            }).run(new WhileDoneCompletion(msg) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (errorCodeList.getCauses().isEmpty()) {
                        replyAndNext.run();
                    } else {
                        detachNetworkServiceFromL3NetworkMsg(l3VO, refVOS, msg.getNetworkOperation(), false, new Completion(msg) {
                            @Override
                            public void success() {
                                dbf.removeCollection(refVOS, NetworkServiceL3NetworkRefVO.class);
                                reply.setError(errorCodeList.getCauses().get(0));
                                replyAndNext.run();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                dbf.removeCollection(refVOS, NetworkServiceL3NetworkRefVO.class);
                                reply.setError(errorCodeList.getCauses().get(0));
                                replyAndNext.run();
                            }
                        });
                    }
                }
            });
            return;
        }

        Runnable enableServices = () -> new While<>(refVOS).each((ref, wcomp) -> {
            String provideType = providerUuidTypeMap.get(ref.getNetworkServiceProviderUuid());
            NetworkServiceProviderType ptype = NetworkServiceProviderType.valueOf(provideType);
            NetworkServiceType nsType = null;
            try {
                /* some network service are not in NetworkServiceType
                * if it need enable/disable, then add it later  */
                nsType = NetworkServiceType.valueOf(ref.getNetworkServiceType());
            } catch (Exception e) {
                logger.debug(e.toString());
                wcomp.done();
                return;
            }

            nsMgr.enableNetworkService(l3VO, ptype, nsType, msg.getApiSystemTags(), msg.getNetworkOperation(), new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    try {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (sdnL3 != null) {
                                    sdnL3.beforeCommitNetworkServiceInDb(getSelfInventory(), msg.getNetworkOperation());
                                }
                                for (NetworkServiceL3NetworkRefVO ref : refVOS) {
                                    persist(ref);
                                }
                            }
                        }.execute();
                    } catch (OperationFailureException e) {
                        reply.setError(e.getErrorCode());
                    } catch (RuntimeException e) {
                        reply.setError(errf.throwableToInternalError(e));
                    } finally {
                        finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                        replyAndNext.run();
                    }
                } else {
                    detachNetworkServiceFromL3NetworkMsg(l3VO, refVOS, msg.getNetworkOperation(), false, new Completion(msg) {
                        @Override
                        public void success() {
                            if (sdnL3 != null) {
                                finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                            }
                            reply.setError(errorCodeList.getCauses().get(0));
                            replyAndNext.run();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (sdnL3 != null) {
                                finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                            }
                            reply.setError(errorCodeList.getCauses().get(0));
                            replyAndNext.run();
                        }
                    });
                }
            }
        });

        sdnL3.beforeChangeNetworkService(getSelfInventory(), msg.getNetworkServices(), true, msg.getNetworkOperation(), new Completion(msg) {
            @Override
            public void success() {
                enableServices.run();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                finishNetworkServiceChange(sdnL3, msg.getNetworkOperation());
                reply.setError(errorCode);
                replyAndNext.run();
            }
        });
    }

	private void handle(APIAttachNetworkServiceToL3NetworkMsg msg) {
        APIAttachNetworkServiceToL3NetworkEvent evt = new APIAttachNetworkServiceToL3NetworkEvent(msg.getId());
        if (msg.isSkipAttach()) {
            self = dbf.reload(self);
            evt.setInventory(L3NetworkInventory.valueOf(self));
            bus.publish(evt);
            return;
        }

        ReturnValueCompletion<L3NetworkInventory> completion = new ReturnValueCompletion<L3NetworkInventory>(msg) {
            @Override
            public void success(L3NetworkInventory inventory) {
                evt.setInventory(inventory);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        };
        attachNetworkServices(msg, completion);
	}

    private void finishIpRangeDeletion(SdnControllerL3 controller, IpRangeInventory range, NetworkOperationContext operation) {
        if (controller == null) {
            return;
        }
        try {
            controller.afterIpRangeFinally(range, operation);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to finish IP range deletion[uuid:%s]", range.getUuid()), e);
        }
    }

    private void finishDnsChange(SdnControllerL3 controller, NetworkOperationContext operation) {
        if (controller == null) {
            return;
        }
        try {
            controller.afterChangeDnsFinally(getSelfInventory(), operation);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to finish DNS change for L3Network[uuid:%s]",
                    self.getUuid()), e);
        }
    }

    private void finishNetworkServiceChange(SdnControllerL3 controller, NetworkOperationContext operation) {
        if (controller == null) {
            return;
        }
        try {
            controller.afterChangeNetworkServiceFinally(getSelfInventory(), operation);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to finish network service change for L3Network[uuid:%s]",
                    self.getUuid()), e);
        }
    }

    private void attachNetworkServices(APIAttachNetworkServiceToL3NetworkMsg msg,
                                       ReturnValueCompletion<L3NetworkInventory> completion) {
        AttachNetworkServiceToL3Msg attach = new AttachNetworkServiceToL3Msg();
        attach.setTimeout(msg.getTimeout());
        attach.setL3NetworkUuid(msg.getL3NetworkUuid());
        attach.setNetworkServices(msg.getNetworkServices());
        if (msg.getSystemTags() != null) {
            attach.setApiSystemTags(msg.getSystemTags());
        }
        attach.setNetworkOperation(msg.getNetworkOperation().copyForRequest());
        bus.makeTargetServiceIdByResourceUuid(attach, L3NetworkConstant.SERVICE_ID,
                msg.getL3NetworkUuid());
        bus.send(attach, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                self = dbf.reload(self);
                completion.success(L3NetworkInventory.valueOf(self));
            }
        });
    }

    private void doDeleteIpRange(APIDeleteIpRangeMsg msg, Completion completion) {
        IpRangeVO vo = dbf.findByUuid(msg.getUuid(), IpRangeVO.class);
        if (vo == null) {
            completion.success();
            return;
        }
        NetworkOperationContext deleteOperation = msg.getNetworkOperation();
        deleteOperation.setStandaloneIpRangeDelete(true);
        pendingDeleteIpRangeOperations.put(vo.getUuid(), deleteOperation);
        pendingDeleteIpRangeErrors.remove(vo.getUuid());
        final String issuer = IpRangeVO.class.getSimpleName();
        final List<IpRangeInventory> ctx = IpRangeInventory.valueOf(Arrays.asList(vo));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-ip-range-%s", vo.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                pendingDeleteIpRangeOperations.remove(vo.getUuid());
                ErrorCode err = pendingDeleteIpRangeErrors.remove(vo.getUuid());
                if (err != null && msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    completion.fail(err);
                    return;
                }
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                pendingDeleteIpRangeOperations.remove(vo.getUuid());
                pendingDeleteIpRangeErrors.remove(vo.getUuid());
                completion.fail(errCode);
            }
        }).start();
    }

    private void syncManagementServiceTypeWhileDelete(ServiceTypeExtensionPoint ext, L2NetworkVO l2NetworkVO, List<String> hostUuids) {
        String l2NetworkType = l2NetworkVO.getType();
        switch (l2NetworkType) {
            case L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE:
            case L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE:
            case L2NetworkConstant.L2_VLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), true);
                break;

            default:
                break;
        }
    }

    private void handle(APIDeleteIpRangeMsg msg) {
        final APIDeleteIpRangeEvent evt = new APIDeleteIpRangeEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
                    @Override
                    public String getSyncSignature() {
                        return getSyncId();
                    }

                    @Override
                    public String getName() {
                        return "delete-ip-range-" + msg.getIpRangeUuid();
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        doDeleteIpRange(msg, new Completion(chain) {
                            @Override
                            public void success() {
                                bus.publish(evt);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                evt.setError(errorCode);
                                bus.publish(evt);
                                chain.next();
                            }
                        });
                    }
                }
        );
    }

    private void doDeleteL3Network(APIDeleteL3NetworkMsg msg, Completion completion) {
        final String issuer = L3NetworkVO.class.getSimpleName();
        final List<L3NetworkInventory> ctx = L3NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3NetworkVO.getL2NetworkUuid(), L2NetworkVO.class);
        chain.setName(String.format("delete-l3-network-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                        .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
                List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.clusterUuid)
                        .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkVO.getUuid()).listValues();
                if (!isExistSystemL3) {
                    if (clusterUuids != null && !clusterUuids.isEmpty()) {
                        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                                .in(HostVO_.clusterUuid, clusterUuids).listValues();
                        for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                            syncManagementServiceTypeWhileDelete(ext, l2NetworkVO, hostUuids);
                        }
                    }
                }
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
    private void handle(APIDeleteL3NetworkMsg msg) {
        final APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());

        doDeleteL3Network(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAddHostRouteToL3NetworkMsg msg) {
        final APIAddHostRouteToL3NetworkEvent evt = new APIAddHostRouteToL3NetworkEvent(msg.getId());

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-hostroute-prefix-%s-to-l3-%s", msg.getPrefix(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-hostroute-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        L3NetworkHostRouteVO vo = new L3NetworkHostRouteVO();
                        vo.setL3NetworkUuid(msg.getL3NetworkUuid());
                        vo.setPrefix(msg.getPrefix());
                        vo.setNexthop(msg.getNexthop());
                        vo = dbf.persist(vo);
                        data.put(L3NetworkConstant.Param.L3_HOSTROUTE_VO, vo);
                        data.put(L3NetworkConstant.Param.L3_HOSTROUTE_SUCCESS, true);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        boolean flag = (boolean) data.get(L3NetworkConstant.Param.L3_HOSTROUTE_SUCCESS);
                        if (flag) {
                            L3NetworkHostRouteVO vo = (L3NetworkHostRouteVO) data.get(L3NetworkConstant.Param.L3_HOSTROUTE_VO);
                            dbf.remove(vo);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AddHostRouteMsg amsg = new AddHostRouteMsg();
                        amsg.setL3NetworkUuid(self.getUuid());
                        amsg.setPrefix(msg.getPrefix());
                        amsg.setNexthop(msg.getNexthop());
                        bus.makeLocalServiceId(amsg, NetworkServiceConstants.HOSTROUTE_SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        evt.setInventory(L3NetworkInventory.valueOf(self));
                        logger.debug(String.format("successfully added hostRoute prefix [%s] nexthop [%s] to L3Network[uuid:%s, name:%s]",
                                msg.getPrefix(), msg.getNexthop(), self.getUuid(), self.getName()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIRemoveHostRouteFromL3NetworkMsg msg) {
        final APIRemoveHostRouteFromL3NetworkEvent evt = new APIRemoveHostRouteFromL3NetworkEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-hostroute-prefix-%s-from-l3-%s", msg.getPrefix(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "remove-hostroute-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
                        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
                        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
                        L3NetworkHostRouteVO hostRouteVO = q.find();
                        if (hostRouteVO != null) {
                            dbf.remove(hostRouteVO);
                        }
                        trigger.next();
                    }
                });

                if (!self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "remove-hostroute-from-backend";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            RemoveHostRouteMsg rmsg = new RemoveHostRouteMsg();
                            rmsg.setL3NetworkUuid(self.getUuid());
                            rmsg.setPrefix(msg.getPrefix());
                            bus.makeLocalServiceId(rmsg, NetworkServiceConstants.HOSTROUTE_SERVICE_ID);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
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
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(L3NetworkInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }
}
