package org.zstack.network.service.lb;

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
public class LoadBalancerManagerImpl extends AbstractService implements LoadBalancerManager {
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

        return true;
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
