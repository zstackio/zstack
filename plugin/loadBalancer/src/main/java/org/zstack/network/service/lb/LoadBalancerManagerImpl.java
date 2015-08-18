package org.zstack.network.service.lb;

import org.apache.commons.lang.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.tag.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipManager;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.TagManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerManagerImpl extends AbstractService implements LoadBalancerManager  {
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
            throw new OperationFailureException(errf.stringToOperationError(String.format("cannot find the load balancer[uuid:%s]", msg.getLoadBalancerUuid())));
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

    private void handle(APICreateLoadBalancerMsg msg) {
        APICreateLoadBalancerEvent evt = new APICreateLoadBalancerEvent(msg.getId());

        LoadBalancerVO vo = new LoadBalancerVO();
        vo.setName(msg.getName());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setVipUuid(msg.getVipUuid());
        vo.setState(LoadBalancerState.Enabled);
        vo = dbf.persistAndRefresh(vo);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), LoadBalancerVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), LoadBalancerVO.class.getSimpleName());

        evt.setInventory(LoadBalancerInventory.valueOf(dbf.reload(vo)));
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
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("cannot delete the system tag[%s]. The load balancer plugin relies on it, you can only update it", tag.getTag())
                ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid balance algorithm[%s], valid algorithms are %s", algorithm, LoadBalancerConstants.BALANCE_ALGORITHMS)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid unhealthy threshold[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid healthy threshold[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid healthy timeout[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid connection idle timeout[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid health check interval[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid max connection[%s], %s is not a number", systemTag, s)
                    ));
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
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", systemTag)
                    ));
                }

                String protocol = ts[0];
                if (!LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS.contains(protocol)) {
                    throw new OperationFailureException(errf.stringToInvalidArgumentError(
                            String.format("invalid health target[%s], the target checking protocol[%s] is invalid, valid protocols are %s",
                                    systemTag, protocol, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCOLS)
                    ));
                }

                String port = ts[1];
                if (!"default".equals(port)) {
                    try {
                        int p = Integer.valueOf(port);
                        if (p < 1 || p > 65535) {
                            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                                    String.format("invalid invalid health target[%s], port[%s] is not in the range of [1, 65535]", systemTag, port)
                            ));
                        }
                    } catch (NumberFormatException e) {
                        throw new OperationFailureException(errf.stringToInvalidArgumentError(
                                String.format("invalid invalid health target[%s], port[%s] is not a number", systemTag, port)
                        ));
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
}
