package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VipManagerImpl extends AbstractService implements VipManager, ReportQuotaExtensionPoint, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VipManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected EventFacade evtf;

    private Map<String, VipReleaseExtensionPoint> vipReleaseExts = new HashMap<String, VipReleaseExtensionPoint>();
    private Map<String, VipBackend> vipBackends = new HashMap<String, VipBackend>();
    private Map<String, VipFactory> factories = new HashMap<>();

    private List<String> releaseVipByApiFlowNames;
    private FlowChainBuilder releaseVipByApiFlowChainBuilder;

    private void populateExtensions() {
        List<PluginExtension> exts = pluginRgty.getExtensionByInterfaceName(VipReleaseExtensionPoint.class.getName());
        for (PluginExtension ext : exts) {
            VipReleaseExtensionPoint extp = (VipReleaseExtensionPoint) ext.getInstance();
            VipReleaseExtensionPoint old = vipReleaseExts.get(extp.getVipUse());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VirtualRouterVipReleaseExtensionPoint for %s, old[%s], new[%s]", old.getClass().getName(), extp.getClass().getName(), old.getVipUse()));
            }
            vipReleaseExts.put(extp.getVipUse(), extp);
        }

        exts = pluginRgty.getExtensionByInterfaceName(VipBackend.class.getName());
        for (PluginExtension ext : exts) {
            VipBackend extp = (VipBackend) ext.getInstance();
            VipBackend old = vipBackends.get(extp.getServiceProviderTypeForVip());
            if (old != null) {
                throw new CloudRuntimeException(
                        String.format("duplicate VipBackend[%s, %s] for provider type[%s]", old.getClass().getName(), extp.getClass().getName(), extp.getServiceProviderTypeForVip())
                );
            }
            vipBackends.put(extp.getServiceProviderTypeForVip(), extp);
        }
        for (VipFactory ext : pluginRgty.getExtensionList(VipFactory.class)) {
            VipFactory old = factories.get(ext.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VipFactory[%s, %s] for the network service provider type[%s]",
                        old.getClass(), ext.getClass(), ext.getNetworkServiceProviderType()));
            }

            factories.put(ext.getNetworkServiceProviderType(), ext);
        }

    }

    public VipReleaseExtensionPoint getVipReleaseExtensionPoint(String use) {
        VipReleaseExtensionPoint extp = vipReleaseExts.get(use);
        if (extp == null) {
            throw new CloudRuntimeException(String.format("cannot get VipReleaseExtensionPoint for use[%s]", use));
        }

        return extp;
    }

    @Override
    public FlowChain getReleaseVipChain() {
        return releaseVipByApiFlowChainBuilder.build();
    }

    @Override
    public VipFactory getVipFactory(String networkServiceProviderType) {
        VipFactory f = factories.get(networkServiceProviderType);
        DebugUtils.Assert(f != null, String.format("cannot find the VipFactory for the network service provider type[%s]", networkServiceProviderType));
        return f;
    }


    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof CreateVipMsg) {
            handle((CreateVipMsg)msg);
        } else if (msg instanceof VipMessage) {
            passThrough((VipMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(VipMessage msg) {
        VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        if (vip == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find the vip[uuid:%s], it may have been deleted", msg.getVipUuid()
            ));
        }

        VipBase v = new VipBase(vip);
        v.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVipMsg) {
            handle((APICreateVipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateVipMsg msg) {
        CreateVipReply reply = new CreateVipReply();

        APICreateVipMsg amsg = new APICreateVipMsg();
        amsg.setName(msg.getName());
        amsg.setDescription(msg.getDescription());
        amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
        amsg.setAllocatorStrategy(msg.getAllocatorStrategy());
        amsg.setRequiredIp(msg.getRequiredIp());
        amsg.setSystem(msg.isSystem());
        amsg.setSession(msg.getSession());
        if (msg.getRequiredIp() != null) {
            if (NetworkUtils.isIpv4Address(msg.getRequiredIp())) {
                amsg.setIpVersion(IPv6Constants.IPv4);
            } else if (IPv6NetworkUtils.isIpv6Address(msg.getRequiredIp())) {
                amsg.setIpVersion(IPv6Constants.IPv6);
            }
        } else if (msg.getIpVersion() != null) {
            amsg.setIpVersion(msg.getIpVersion());
        } else {
            L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
            if (l3NetworkVO.getIpVersions().contains(IPv6Constants.IPv4)) {
                amsg.setIpVersion(IPv6Constants.IPv4);
            } else {
                amsg.setIpVersion(IPv6Constants.IPv6);
            }
        }
        docreateVip(amsg, new ReturnValueCompletion<VipInventory>(msg) {
            @Override
            public void success(VipInventory returnValue) {
                reply.setVip(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void docreateVip(final APICreateVipMsg msg, ReturnValueCompletion<VipInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-vip-%s-from-l3-%s", msg.getName(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            UsedIpInventory ip;
            VipInventory vip;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = String.format("allocate-ip-for-vip");

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        /* if ip address is allocated, vip is virtual router system vip */
                        if (msg.getRequiredIp() != null) {
                            UsedIpVO usedIpVO = Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getRequiredIp())
                                    .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).find();
                            if (usedIpVO != null) {
                                ip = UsedIpInventory.valueOf(usedIpVO);
                                trigger.next();
                                return;
                            }
                        }

                        String strategyType = msg.getAllocatorStrategy();
                        if (strategyType == null) {
                            if (msg.getIpVersion() == IPv6Constants.IPv4) {
                                strategyType = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY;
                            } else {
                                strategyType = L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY;
                            }
                        }
                        AllocateIpMsg amsg = new AllocateIpMsg();
                        amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                        amsg.setAllocateStrategy(strategyType);
                        amsg.setRequiredIp(msg.getRequiredIp());
                        amsg.setUseAddressPoolIfNotRequiredIpRange(true);
                        if (msg.getIpRangeUuid() != null) {
                            amsg.setIpRangeUuid(msg.getIpRangeUuid());
                        }
                        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateIpReply re = reply.castReply();
                                    ip = re.getIpInventory();
                                    data.put("newCreate", ip);
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        UsedIpInventory newVip = (UsedIpInventory)data.get("newCreate");
                        if (newVip == null) {
                            trigger.rollback();
                            return;
                        }

                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, rmsg.getL3NetworkUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to return ip[uuid:%s, ip:%s] to l3Network[uuid:%s], %s",
                                            ip.getUuid(), ip.getIp(), ip.getL3NetworkUuid(), reply.getError()));
                                }

                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("create-vip-in-db");

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VipVO vipvo = new VipVO();
                        if (msg.getResourceUuid() != null) {
                            vipvo.setUuid(msg.getResourceUuid());
                        } else {
                            vipvo.setUuid(Platform.getUuid());
                        }
                        vipvo.setName(msg.getName());
                        vipvo.setDescription(msg.getDescription());
                        vipvo.setState(VipState.Enabled);
                        vipvo.setGateway(ip.getGateway());
                        vipvo.setIp(ip.getIp());
                        vipvo.setIpRangeUuid(ip.getIpRangeUuid());
                        vipvo.setL3NetworkUuid(ip.getL3NetworkUuid());
                        vipvo.setNetmask(ip.getNetmask());
                        vipvo.setUsedIpUuid(ip.getUuid());
                        vipvo.setAccountUuid(msg.getSession().getAccountUuid());
                        vipvo.setSystem(msg.isSystem());

                        IpRangeVO ipr = dbf.findByUuid(ip.getIpRangeUuid(), IpRangeVO.class);
                        vipvo.setPrefixLen(ipr.getPrefixLen());

                        VipVO finalVipvo = vipvo;
                        vipvo = new SQLBatchWithReturn<VipVO>() {
                            @Override
                            protected VipVO scripts() {
                                persist(finalVipvo);
                                reload(finalVipvo);
                                tagMgr.createTagsFromAPICreateMessage(msg, finalVipvo.getUuid(), VipVO.class.getSimpleName());
                                return finalVipvo;
                            }
                        }.execute();

                        vip = VipInventory.valueOf(vipvo);

                        VipCanonicalEvents.VipEventData vipEventData = new VipCanonicalEvents.VipEventData();
                        vipEventData.setVipUuid(vipvo.getUuid());
                        vipEventData.setCurrentStatus(VipCanonicalEvents.VIP_STATUS_CREATED);
                        vipEventData.setInventory(vip);
                        vipEventData.setDate(new Date());
                        evtf.fire(VipCanonicalEvents.VIP_CREATED_PATH, vipEventData);

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully acquired vip[uuid:%s, address:%s] on l3NetworkUuid[uuid:%s]", vip.getUuid(), ip.getIp(), ip.getL3NetworkUuid()));
                        completion.success(vip);
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

    private void handle(final APICreateVipMsg msg) {
        final APICreateVipEvent evt = new APICreateVipEvent(msg.getId());

        docreateVip(msg, new ReturnValueCompletion<VipInventory>(msg) {
            @Override
            public void success(VipInventory returnValue) {
                evt.setInventory(returnValue);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VipConstant.SERVICE_ID);
    }

    private void prepareFlows() {
        releaseVipByApiFlowChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(releaseVipByApiFlowNames).construct();
    }

    @Override
    public boolean start() {
        populateExtensions();
        prepareFlows();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void setReleaseVipByApiFlowNames(List<String> releaseVipByApiFlowNames) {
        this.releaseVipByApiFlowNames = releaseVipByApiFlowNames;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateVipMsg) {
                        check((APICreateVipMsg) msg, pairs);
                    }
                }

                if (msg instanceof APIChangeResourceOwnerMsg) {
                    check((APIChangeResourceOwnerMsg) msg, pairs);
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setUsed(getUsedVip(accountUuid));
                usage.setName(VipQuotaConstant.VIP_NUM);
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedVip(String accountUuid) {
                String sql = "select count(vip) from VipVO vip, AccountResourceRefVO ref where ref.resourceUuid = vip.uuid" +
                        " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", VipVO.class.getSimpleName());
                Long vn = q.getSingleResult();
                vn = vn == null ? 0 : vn;
                return vn;
            }

            private void check(APIChangeResourceOwnerMsg msg, Map<String, QuotaPair> pairs) {
                long vipNum = pairs.get(VipQuotaConstant.VIP_NUM).getValue();
                long vn = getUsedVip(msg.getAccountUuid());

                if (vn + 1 > vipNum) {
                    throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                            msg.getAccountUuid(), VipQuotaConstant.VIP_NUM, vipNum));
                }
            }

            private void check(APICreateVipMsg msg, Map<String, QuotaPair> pairs) {
                long vipNum = pairs.get(VipQuotaConstant.VIP_NUM).getValue();
                long vn = getUsedVip(msg.getSession().getAccountUuid());

                if (vn + 1 > vipNum) {
                    throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                                    msg.getSession().getAccountUuid(), VipQuotaConstant.VIP_NUM, vipNum));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateVipMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(VipQuotaConstant.VIP_NUM);
        p.setValue(VipQuotaGlobalConfig.VIP_NUM.defaultValue(Long.class));
        quota.addPair(p);

        return list(quota);
    }

    @Override
    public void prepareDbInitialValue() {
        List<VipVO> vipVOS = Q.New(VipVO.class).isNull(VipVO_.prefixLen).list();
        for (VipVO vip : vipVOS) {
            vip.setPrefixLen(NetworkUtils.getPrefixLengthFromNetwork(vip.getNetmask()));
        }
        if (!vipVOS.isEmpty()) {
            dbf.updateCollection(vipVOS);
        }

        vipVOS = Q.New(VipVO.class).eq(VipVO_.system, false).list();
        for (VipVO vip : vipVOS) {
            if (vip.getServicesTypes().contains(NetworkServiceType.SNAT.toString())) {
                vip.setSystem(true);
            }
        }
        if (!vipVOS.isEmpty()) {
            dbf.updateCollection(vipVOS);
        }
    }

    @Override
    public boolean isSystemVip(VipVO vip) {
        return vip.isSystem();
    }
}
