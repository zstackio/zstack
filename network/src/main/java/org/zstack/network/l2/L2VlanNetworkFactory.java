package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class L2VlanNetworkFactory extends AbstractService implements L2NetworkFactory, L2NetworkDefaultMtu {
    private static CLogger logger = Utils.getLogger(L2VlanNetworkFactory.class);
    static L2NetworkType type = new L2NetworkType(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    
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
        APICreateL2VlanNetworkMsg amsg = (APICreateL2VlanNetworkMsg) msg;
        L2VlanNetworkVO vo = new L2VlanNetworkVO(ovo);
        vo.setVlan(amsg.getVlan());
        vo = dbf.persistAndRefresh(vo);
        L2VlanNetworkInventory inv = L2VlanNetworkInventory.valueOf(vo);
        String info = String.format("successfully create L2VlanNetwork, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        return inv;
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2VlanNetwork(vo);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIListL2VlanNetworkMsg) {
            handle((APIListL2VlanNetworkMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }
    
    private void handle(APIListL2VlanNetworkMsg msg) {
        List<L2VlanNetworkVO> vos = dbf.listByApiMessage(msg, L2VlanNetworkVO.class);
        List<L2VlanNetworkInventory> invs = L2VlanNetworkInventory.valueOf1(vos);
        APIListL2VlanNetworkReply reply = new APIListL2VlanNetworkReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(L2NetworkConstant.L2_VLAN_NETWORK_FACTORY_SERVICE_ID);
    }

    @Override
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu() {
        return Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_VLAN.getDefaultValue());
    }
}
