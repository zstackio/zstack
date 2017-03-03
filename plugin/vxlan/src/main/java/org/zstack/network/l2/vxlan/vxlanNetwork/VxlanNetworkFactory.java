package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by weiwang on 02/03/2017.
 */
public class VxlanNetworkFactory implements L2NetworkFactory {
    private static CLogger logger = Utils.getLogger(VxlanNetwork.class);
    static L2NetworkType type = new L2NetworkType(VxlanNetworkConstant.VXLAN_NETWORK_TYPE);

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
        APICreateVxlanNetworkMsg amsg = (APICreateVxlanNetworkMsg) msg;
        VxlanNetworkVO vo = new VxlanNetworkVO(ovo);
        // todo ...
        vo = dbf.persistAndRefresh(vo);
        L2VxlanNetworkInventory inv = L2VxlanNetworkInventory.valueOf(vo);
        String info = String.format("successfully create L2VxlanNetwork, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        return inv;
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new VxlanNetwork(vo);
    }

}
