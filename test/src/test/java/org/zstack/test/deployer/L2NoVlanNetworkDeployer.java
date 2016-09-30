package org.zstack.test.deployer;

import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.L2NoVlanNetworkConfig;

import java.util.List;

public class L2NoVlanNetworkDeployer implements L2NetworkDeployer<L2NoVlanNetworkConfig> {
    @Override
    public Class<L2NoVlanNetworkConfig> getSupportedDeployerClassType() {
        return L2NoVlanNetworkConfig.class;
    }

    @Override
    public void deploy(List<L2NoVlanNetworkConfig> l2Networks, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (L2NoVlanNetworkConfig l2c : l2Networks) {
            L2NetworkInventory l2inv = new L2NetworkInventory();
            l2inv.setName(l2c.getName());
            l2inv.setDescription(l2c.getDescription());
            l2inv.setPhysicalInterface(l2c.getPhysicalInterface());
            l2inv.setZoneUuid(zone.getUuid());
            l2inv.setType(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
            l2inv = deployer.getApi().createL2NetworkByFullConfig(l2inv);
            deployer.l2Networks.put(l2inv.getName(), l2inv);
            deployer.deployL3Network(l2c.getL3Networks(), l2inv);
        }
    }
}
