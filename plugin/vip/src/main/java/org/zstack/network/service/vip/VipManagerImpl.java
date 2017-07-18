package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.quota.QuotaConstant;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VipManagerImpl extends AbstractService implements VipManager, ReportQuotaExtensionPoint {
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
            throw new CloudRuntimeException(String.format("cannot VipReleaseExtensionPoint for use[%s]", use));
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
        if (msg instanceof VipMessage) {
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
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find the vip[uuid:%s], it may have been deleted", msg.getVipUuid())
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

    private void handle(final APICreateVipMsg msg) {
        final APICreateVipEvent evt = new APICreateVipEvent(msg.getId());

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
                        String strategyType = msg.getAllocatorStrategy() == null ? L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY : msg.getAllocatorStrategy();
                        AllocateIpMsg amsg = new AllocateIpMsg();
                        amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                        amsg.setAllocateStrategy(strategyType);
                        amsg.setRequiredIp(msg.getRequiredIp());
                        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateIpReply re = reply.castReply();
                                    ip = re.getIpInventory();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (ip == null) {
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

                        VipVO finalVipvo = vipvo;
                        vipvo = new SQLBatchWithReturn<VipVO>() {
                            @Override
                            protected VipVO scripts() {
                                persist(finalVipvo);
                                reload(finalVipvo);
                                acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), finalVipvo.getUuid(), VipVO.class);
                                tagMgr.createTagsFromAPICreateMessage(msg, finalVipvo.getUuid(), VipVO.class.getSimpleName());
                                return finalVipvo;
                            }
                        }.execute();

                        vip = VipInventory.valueOf(vipvo);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully acquired vip[uuid:%s, address:%s] on l3NetworkUuid[uuid:%s]", vip.getUuid(), ip.getIp(), ip.getL3NetworkUuid()));
                        evt.setInventory(vip);
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
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setUsed(getUsedVip(accountUuid));
                usage.setName(VipConstant.QUOTA_VIP_NUM);
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

            private void check(APICreateVipMsg msg, Map<String, QuotaPair> pairs) {
                long vipNum = pairs.get(VipConstant.QUOTA_VIP_NUM).getValue();
                long vn = getUsedVip(msg.getSession().getAccountUuid());

                if (vn + 1 > vipNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VipConstant.QUOTA_VIP_NUM, vipNum)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateVipMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(VipConstant.QUOTA_VIP_NUM);
        p.setValue(QuotaConstant.QUOTA_VIP_NUM);
        quota.addPair(p);

        return list(quota);
    }
}
