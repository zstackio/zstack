package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterCreateExtensionPoint;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.server.ServerPoolVO;

public class DefaultServerPoolClusterCreateExtension implements ClusterCreateExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DefaultServerPoolFactory defaultServerPoolFactory;

    @Override
    public void afterCreateCluster(ClusterVO cluster) {
        DefaultServerPoolCreationPolicy policy = DefaultServerPoolCreationPolicy.valueOf(
                PhysicalServerGlobalConfig.DEFAULT_SERVER_POOL_CREATION_POLICY.value(String.class));
        if (policy != DefaultServerPoolCreationPolicy.OnClusterCreate || cluster.getServerPoolUuid() != null) {
            return;
        }

        ServerPoolVO defaultPool = defaultServerPoolFactory.findDefaultPool(cluster.getZoneUuid());
        if (defaultPool == null && defaultServerPoolFactory.hasAnyPool(cluster.getZoneUuid())) {
            return;
        }

        if (defaultPool == null) {
            defaultPool = defaultServerPoolFactory.ensureDefaultPool(cluster.getZoneUuid());
        }

        cluster.setServerPoolUuid(defaultPool.getUuid());
        dbf.updateAndRefresh(cluster);
    }
}
