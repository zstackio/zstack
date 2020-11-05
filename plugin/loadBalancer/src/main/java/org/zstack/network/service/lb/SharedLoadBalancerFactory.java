package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.utils.DebugUtils;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SharedLoadBalancerFactory implements LoadBalancerFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    LoadBalancerManager lbMgr;

    @Override
    public String getType() {
        return LoadBalancerType.Shared.toString();
    }

    @Override
    public LoadBalancerVO persistLoadBalancer(APICreateLoadBalancerMsg msg) {
        LoadBalancerVO vo = new LoadBalancerVO();
        vo.setName(msg.getName());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setVipUuid(msg.getVipUuid());
        vo.setState(LoadBalancerState.Enabled);
        vo.setType(LoadBalancerType.Shared);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    @Override
    public void deleteLoadBalancer(LoadBalancerVO vo) {
        dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerVO.class);
    }

    @Override
    public String getNetworkServiceType() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING;
    }

    @Override
    public LoadBalancerBackend getLoadBalancerBackend(LoadBalancerVO vo) {
        if (vo.getProviderType() == null) {
            return null;
        }

        return lbMgr.getBackend(vo.getProviderType());
    }
}
