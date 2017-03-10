package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniReply;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

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
        APICreateL2VxlanNetworkMsg amsg = (APICreateL2VxlanNetworkMsg) msg;
        VxlanNetworkVO vo = new VxlanNetworkVO(ovo);

        AllocateVniMsg vniMsg = new AllocateVniMsg();
        vniMsg.setL2NetworkUuid(amsg.getPoolUuid());
        vniMsg.setRequiredVni(amsg.getVni());
        bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getPoolUuid());
        MessageReply reply = bus.call(amsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        AllocateVniReply r = reply.castReply();
        vo.setVni(r.getVni());
        vo.setPoolUuid((amsg.getPoolUuid()));
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
