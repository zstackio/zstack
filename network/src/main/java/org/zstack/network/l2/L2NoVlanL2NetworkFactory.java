package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;

public class L2NoVlanL2NetworkFactory implements L2NetworkFactory, Component, L2NetworkDefaultMtu{
    private static L2NetworkType type = new L2NetworkType(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    private static CLogger logger = Utils.getLogger(L2NoVlanL2NetworkFactory.class);
    private static FieldPrinter printer = Utils.getFieldPrinter();
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public L2NetworkInventory createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg) {
        vo = dbf.persistAndRefresh(vo);
        L2NetworkInventory inv = L2NetworkInventory.valueOf(vo);
        logger.debug("Successfully created NoVlanL2Network: " + printer.print(inv));
        return L2NetworkInventory.valueOf(vo);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2NoVlanNetwork(vo);
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
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu() {
        return Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN.getDefaultValue());
    }
}
