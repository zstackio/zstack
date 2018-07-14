package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.tag.AbstractSystemTagOperationJudger;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerManagerImpl extends AbstractService implements LoadBalancerManager,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VipGetUsedPortRangeExtensionPoint, VipGetServiceReferencePoint {
    private static final CLogger logger = Utils.getLogger(LoadBalancerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    private Map<String, LoadBalancerBackend> backends = new HashMap<String, LoadBalancerBackend>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof LoadBalancerMessage) {
            passThrough((LoadBalancerMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(LoadBalancerMessage msg) {
        LoadBalancerVO vo = dbf.findByUuid(msg.getLoadBalancerUuid(), LoadBalancerVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the load balancer[uuid:%s]", msg.getLoadBalancerUuid()));
        }

        LoadBalancerBase base = new LoadBalancerBase(vo);
        base.handleMessage((Message) msg);
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CertificateDeletionMsg) {
            handle((CertificateDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CertificateDeletionMsg msg) {
        CertificateDeletionReply reply = new CertificateDeletionReply();
        dbf.removeByPrimaryKey(msg.getUuid(), CertificateVO.class);
        bus.reply(msg, reply);
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateLoadBalancerMsg) {
            handle((APICreateLoadBalancerMsg) msg);
        } else if (msg instanceof APICreateCertificateMsg) {
            handle((APICreateCertificateMsg) msg);
        } else if (msg instanceof APIDeleteCertificateMsg) {
            handle((APIDeleteCertificateMsg) msg);
        } else if (msg instanceof APIUpdateCertificateMsg) {
            handle((APIUpdateCertificateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APICreateLoadBalancerMsg msg) {
        final APICreateLoadBalancerEvent evt = new APICreateLoadBalancerEvent(msg.getId());

        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(msg.getVipUuid(), VipVO.class));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-lb-%s", msg.getName()));
        chain.then(new ShareFlow() {
            LoadBalancerVO vo;

            @Override
            public void setup() {

                flow(new Flow() {
                    String __name__ = "write-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vo = new LoadBalancerVO();
                        vo.setName(msg.getName());
                        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                        vo.setDescription(msg.getDescription());
                        vo.setVipUuid(msg.getVipUuid());
                        vo.setState(LoadBalancerState.Enabled);
                        vo.setAccountUuid(msg.getSession().getAccountUuid());
                        vo = dbf.persistAndRefresh(vo);

                        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), LoadBalancerVO.class.getSimpleName());
                        /* put vo to data for rollback */
                        data.put(LoadBalancerConstants.Param.LOAD_BALANCER_VO, vo);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        LoadBalancerVO vo = (LoadBalancerVO)data.get(LoadBalancerConstants.Param.LOAD_BALANCER_VO);
                        dbf.remove(vo);
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "acquire-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(struct);
                        v.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
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
                        LoadBalancerVO vo = (LoadBalancerVO)data.get(LoadBalancerConstants.Param.LOAD_BALANCER_VO);
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(struct);
                        v.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO add GC
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(LoadBalancerInventory.valueOf(dbf.reload(vo)));
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

    private void handle(final APICreateCertificateMsg msg) {
        final APICreateCertificateEvent evt = new APICreateCertificateEvent(msg.getId());

        CertificateVO vo = new CertificateVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        vo.setCertificate(msg.getCertificate());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), CertificateVO.class.getSimpleName());

        evt.setInventory(CertificateInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(final APIDeleteCertificateMsg msg) {
        final APIDeleteCertificateEvent evt = new APIDeleteCertificateEvent(msg.getId());

        CertificateInventory inv = CertificateInventory.valueOf(dbf.findByUuid(msg.getUuid(), CertificateVO.class));
        SQL.New(CertificateVO.class).eq(CertificateVO_.uuid, msg.getUuid()).delete();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-certificate-%s", msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Set<String> lbUuids = CollectionUtils.transformToSet(inv.getListeners(), new Function<String, LoadBalancerListenerCertificateRefInventory>() {
                            @Override
                            public String call(LoadBalancerListenerCertificateRefInventory arg) {
                                return Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, arg.getListenerUuid())
                                        .select(LoadBalancerListenerVO_.loadBalancerUuid).findValue();
                            }
                        });

                        if (lbUuids == null || lbUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<ErrorCode> errs = new ArrayList<>();
                        new While<>(lbUuids).each((lbUuid, wcompl) -> {
                            LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
                            cmsg.setLoadBalancerUuid(lbUuid);
                            cmsg.setCertificateUuid(null);
                            bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
                            bus.send(cmsg, new CloudBusCallBack(wcompl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        errs.add(reply.getError());
                                    }
                                    wcompl.done();
                                }
                            });
                        }).run(new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (errs.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errs.get(0));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
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

    private void handle(final APIUpdateCertificateMsg msg) {
        APIUpdateCertificateEvent evt = new APIUpdateCertificateEvent(msg.getId());

        CertificateVO vo = dbf.findByUuid(msg.getUuid(), CertificateVO.class);
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

        evt.setInventory(CertificateInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LoadBalancerConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        for (LoadBalancerBackend bkd : pluginRgty.getExtensionList(LoadBalancerBackend.class)) {
            LoadBalancerBackend old = backends.get(bkd.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LoadBalancerBackend[%s, %s]", old.getClass(), bkd.getNetworkServiceProviderType()));
            }

            backends.put(bkd.getNetworkServiceProviderType(), bkd);
        }

        prepareSystemTags();

        return true;
    }

    private void prepareSystemTags() {
        AbstractSystemTagOperationJudger judger = new AbstractSystemTagOperationJudger() {
            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                throw new OperationFailureException(operr("cannot delete the system tag[%s]. The load balancer plugin relies on it, you can only update it", tag.getTag()));
            }
        };
        LoadBalancerSystemTags.BALANCER_ALGORITHM.installJudger(judger);
        LoadBalancerSystemTags.HEALTHY_THRESHOLD.installJudger(judger);
        LoadBalancerSystemTags.MAX_CONNECTION.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_INTERVAL.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_TARGET.installJudger(judger);
        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.installJudger(judger);
        LoadBalancerSystemTags.HEALTH_TIMEOUT.installJudger(judger);
        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.installJudger(judger);

        LoadBalancerSystemTags.BALANCER_ALGORITHM.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String algorithm = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);

                if (!LoadBalancerConstants.BALANCE_ALGORITHMS.contains(algorithm)) {
                    throw new OperationFailureException(argerr("invalid balance algorithm[%s], valid algorithms are %s", algorithm, LoadBalancerConstants.BALANCE_ALGORITHMS));
                }
            }
        });

        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid unhealthy threshold[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTHY_THRESHOLD.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid healthy threshold[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_TIMEOUT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTH_TIMEOUT.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid healthy timeout[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid connection idle timeout[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_INTERVAL.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.HEALTH_INTERVAL.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid health check interval[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.MAX_CONNECTION.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String s = LoadBalancerSystemTags.MAX_CONNECTION.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.MAX_CONNECTION_TOKEN);

                try {
                    Long.valueOf(s);
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("invalid max connection[%s], %s is not a number", systemTag, s));
                }
            }
        });

        LoadBalancerSystemTags.HEALTH_TARGET.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String target = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByTag(systemTag,
                        LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);

                String[] ts = target.split(":");
                if (ts.length != 2) {
                    throw new OperationFailureException(argerr("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", systemTag));
                }

                String protocol = ts[0];
                if (!LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS.contains(protocol)) {
                    throw new OperationFailureException(argerr("invalid health target[%s], the target checking protocol[%s] is invalid, valid protocols are %s",
                            systemTag, protocol, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS));
                }

                String port = ts[1];
                if (!"default".equals(port)) {
                    try {
                        int p = Integer.valueOf(port);
                        if (p < 1 || p > 65535) {
                            throw new OperationFailureException(argerr("invalid invalid health target[%s], port[%s] is not in the range of [1, 65535]", systemTag, port));
                        }
                    } catch (NumberFormatException e) {
                        throw new OperationFailureException(argerr("invalid invalid health target[%s], port[%s] is not a number", systemTag, port));
                    }
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public LoadBalancerBackend getBackend(String providerType) {
        LoadBalancerBackend bkd = backends.get(providerType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find LoadBalancerBackend[provider type:%s]", providerType));
        }
        return bkd;
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct s = new ExpandedQueryStruct();
        s.setExpandedField("loadBalancerListenerRef");
        s.setHidden(true);
        s.setForeignKey("uuid");
        s.setExpandedInventoryKey("vmNicUuid");
        s.setInventoryClassToExpand(VmNicInventory.class);
        s.setInventoryClass(LoadBalancerListenerVmNicRefInventory.class);
        structs.add(s);

        s = new ExpandedQueryStruct();
        s.setInventoryClassToExpand(VipInventory.class);
        s.setExpandedField("loadBalancer");
        s.setInventoryClass(LoadBalancerInventory.class);
        s.setForeignKey("uuid");
        s.setExpandedInventoryKey("vipUuid");
        structs.add(s);
        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        List<ExpandedQueryAliasStruct> structs = new ArrayList<ExpandedQueryAliasStruct>();

        ExpandedQueryAliasStruct s = new ExpandedQueryAliasStruct();
        s.setInventoryClass(VmNicInventory.class);
        s.setAlias("loadBalancerListener");
        s.setExpandedField("loadBalancerListenerRef.listener");
        structs.add(s);
        return structs;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateLoadBalancerMsg) {
                        check((APICreateLoadBalancerMsg) msg, pairs);
                    } else if (msg instanceof APICreateLoadBalancerListenerMsg) {
                        check((APICreateLoadBalancerListenerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(LoadBalanceQuotaConstant.LOAD_BALANCER_NUM);
                usage.setUsed(getUsedLb(accountUuid));
                usages.add(usage);

                Quota.QuotaUsage listenerUsage = new Quota.QuotaUsage();
                listenerUsage.setName(LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM);
                listenerUsage.setUsed(getUsedLbListener(accountUuid));
                usages.add(listenerUsage);

                return usages;
            }

            @Transactional(readOnly = true)
            private long getUsedLb(String accountUuid) {
                String sql = "select count(lb.uuid) from LoadBalancerVO lb, AccountResourceRefVO ref where ref.resourceUuid = lb.uuid and " +
                        "ref.accountUuid = :auuid and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", LoadBalancerVO.class.getSimpleName());
                Long en = q.getSingleResult();
                en = en == null ? 0 : en;
                return en;
            }

            @Transactional(readOnly = true)
            private long getUsedLbListener(String accountUuid) {
                String sql = "select count(lb.uuid) from LoadBalancerListenerVO lb, AccountResourceRefVO ref where ref.resourceUuid = lb.uuid and " +
                        "ref.accountUuid = :auuid and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", LoadBalancerListenerVO.class.getSimpleName());
                Long en = q.getSingleResult();
                en = en == null ? 0 : en;
                return en;
            }

            private void check(APICreateLoadBalancerListenerMsg msg, Map<String, QuotaPair> pairs) {
                long lbNum = pairs.get(LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM).getValue();
                long en = getUsedLbListener(msg.getSession().getAccountUuid());

                if (en + 1 > lbNum) {
                    throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                                    msg.getSession().getAccountUuid(), LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM, lbNum));
                }
            }

            private void check(APICreateLoadBalancerMsg msg, Map<String, QuotaPair> pairs) {
                long lbNum = pairs.get(LoadBalanceQuotaConstant.LOAD_BALANCER_NUM).getValue();
                long en = getUsedLb(msg.getSession().getAccountUuid());

                if (en + 1 > lbNum) {
                    throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                                    msg.getSession().getAccountUuid(), LoadBalanceQuotaConstant.LOAD_BALANCER_NUM, lbNum));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateLoadBalancerMsg.class);
        quota.addMessageNeedValidation(APICreateLoadBalancerListenerMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(LoadBalanceQuotaConstant.LOAD_BALANCER_NUM);
        p.setValue(LoadBalanceQuotaGlobalConfig.LOAD_BALANCER_NUM.defaultValue(Long.class));
        quota.addPair(p);

        QuotaPair listener = new QuotaPair();
        listener.setName(LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM);
        listener.setValue(LoadBalanceQuotaGlobalConfig.LOAD_BALANCER_LISTENER_NUM.defaultValue(Long.class));
        quota.addPair(listener);

        return list(quota);
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList){

        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();

        /* no matter TCP or HTTP, retrieve all the ports of TCP and HTTP */
        if (protocol.toLowerCase().equals(LoadBalancerConstants.LB_PROTOCOL_TCP) ||
                protocol.toLowerCase().equals(LoadBalancerConstants.LB_PROTOCOL_HTTP)) {
            List<Tuple> lbPortList = SQL.New("select lbl.loadBalancerPort, lbl.loadBalancerPort from LoadBalancerListenerVO lbl, LoadBalancerVO lb "
                    + "where lbl.loadBalancerUuid=lb.uuid and lb.vipUuid = :vipUuid", Tuple.class).
                    param("vipUuid", vipUuid).list();

            Iterator<Tuple> it = lbPortList.iterator();
            while (it.hasNext()) {
                Tuple strRange = it.next();
                int start = strRange.get(0, Integer.class);
                int end = strRange.get(1, Integer.class);

                RangeSet.Range range = new RangeSet.Range(start, end);
                portRanges.add(range);
            }
            portRangeList.setRanges(portRanges);
        }

        return portRangeList;
    }

    @Override
    public ServiceReference getServiceReference(String vipUuid) {
        long count = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.vipUuid, vipUuid).count();
        return new VipGetServiceReferencePoint.ServiceReference(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING, count);
    }
}
