package org.zstack.test.deployer;

import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.L2VlanNetworkConfig;

import java.util.List;

public class L2VlanNetworkDeployer implements L2NetworkDeployer<L2VlanNetworkConfig> {

    @Override
    public Class<L2VlanNetworkConfig> getSupportedDeployerClassType() {
        return L2VlanNetworkConfig.class;
    }

    @Override
    public void deploy(List<L2VlanNetworkConfig> l2Networks, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (L2VlanNetworkConfig l2c : l2Networks) {
            L2VlanNetworkInventory l2inv = new L2VlanNetworkInventory();
            l2inv.setName(l2c.getName());
            l2inv.setType(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
            l2inv.setDescription(l2c.getDescription());
            l2inv.setPhysicalInterface(l2c.getPhysicalInterface());
            l2inv.setZoneUuid(zone.getUuid());
            l2inv.setVlan((int) l2c.getVlan());
            l2inv = deployer.getApi().createL2VlanNetworkByFullConfig(l2inv);
            deployer.l2Networks.put(l2inv.getName(), l2inv);
            deployer.deployL3Network(l2c.getL3Networks(), l2inv);
        }
    }

}
