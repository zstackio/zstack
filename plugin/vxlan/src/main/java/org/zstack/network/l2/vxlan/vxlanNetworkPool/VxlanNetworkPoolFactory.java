package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.network.l2.*;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by weiwang on 03/03/2017.
 */
public class VxlanNetworkPoolFactory implements L2NetworkFactory, Component {
    private static CLogger logger = Utils.getLogger(VxlanNetworkPoolFactory.class);
    static L2NetworkType type = new L2NetworkType(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public L2NetworkInventory createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg) {
        VxlanNetworkPoolVO vo = new VxlanNetworkPoolVO(ovo);
        if (vo.getPhysicalInterface() == null) {
            vo.setPhysicalInterface("No use");
        }
        vo = dbf.persistAndRefresh(vo);
        L2VxlanNetworkPoolInventory inv = L2VxlanNetworkPoolInventory.valueOf(vo);
        String info = String.format("successfully create L2VxlanNetworkPool, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        return inv;
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new VxlanNetworkPool(vo);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
