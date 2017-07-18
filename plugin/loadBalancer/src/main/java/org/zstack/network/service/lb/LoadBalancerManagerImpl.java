package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.quota.QuotaConstant;
import org.zstack.header.tag.AbstractSystemTagOperationJudger;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerManagerImpl extends AbstractService implements LoadBalancerManager,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint {
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
        bus.dealWithUnknownMessage(msg);
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateLoadBalancerMsg) {
            handle((APICreateLoadBalancerMsg) msg);
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
                    String __name__ = "acquire-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Vip v = new Vip(vip.getUuid());
                        v.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        v.acquire(false, new Completion(trigger) {
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
                        if (!s) {
                            trigger.rollback();
                            return;
                        }

                        new Vip(vip.getUuid()).release(false, new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "write-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vo = new LoadBalancerVO();
                        vo.setName(msg.getName());
                        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                        vo.setDescription(msg.getDescription());
                        vo.setVipUuid(msg.getVipUuid());
                        vo.setState(LoadBalancerState.Enabled);
                        vo = dbf.persistAndRefresh(vo);

                        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), LoadBalancerVO.class);
                        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), LoadBalancerVO.class.getSimpleName());
                        trigger.next();
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
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM);
                usage.setUsed(getUsedLb(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedLb(String accountUuid) {
                String sql = "select count(lb) from LoadBalancerVO lb, AccountResourceRefVO ref where ref.resourceUuid = lb.uuid and " +
                        "ref.accountUuid = :auuid and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", LoadBalancerVO.class.getSimpleName());
                Long en = q.getSingleResult();
                en = en == null ? 0 : en;
                return en;
            }

            private void check(APICreateLoadBalancerMsg msg, Map<String, QuotaPair> pairs) {
                long lbNum = pairs.get(LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM).getValue();
                long en = getUsedLb(msg.getSession().getAccountUuid());

                if (en + 1 > lbNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM, lbNum)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateLoadBalancerMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM);
        p.setValue(QuotaConstant.QUOTA_LOAD_BALANCER_NUM);
        quota.addPair(p);

        return list(quota);
    }
}
