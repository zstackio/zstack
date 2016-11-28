package org.zstack.test.deployer;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.L3BasicNetworkConfig;

import java.util.ArrayList;
import java.util.List;

public class L3BasicNetworkDeployer implements L3NetworkDeployer<L3BasicNetworkConfig> {
    @Override
    public Class<L3BasicNetworkConfig> getSupportedDeployerClassType() {
        return L3BasicNetworkConfig.class;
    }

    @Override
    public void deploy(List<L3BasicNetworkConfig> l3Networks, L2NetworkInventory l2Network, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (L3BasicNetworkConfig l3c : l3Networks) {
            L3NetworkInventory l3inv = new L3NetworkInventory();
            l3inv.setDescription(l3c.getDescription());
            l3inv.setL2NetworkUuid(l2Network.getUuid());
            l3inv.setName(l3c.getName());
            l3inv.setType(L3NetworkConstant.L3_BASIC_NETWORK_TYPE);
            l3inv.setDnsDomain(l3c.getDnsDomain());
            SessionInventory session = null;
            if (l3c.getAccountRef() != null) {
                session = deployer.loginByAccountRef(l3c.getAccountRef(), config);
            }

            if (session != null) {
                l3inv = deployer.getApi().createL3NetworkByFullConfig(l3inv, session);
            } else {
                l3inv = deployer.getApi().createL3NetworkByFullConfig(l3inv);
            }

            deployer.addIpRange(l3c.getIpRange(), l3inv, session);

            for (String dns : l3c.getDns()) {
                deployer.getApi().addDns(l3inv.getUuid(), dns);
            }
            deployer.attachNetworkServiceToL3Network(l3inv, l3c.getNetworkService());
            List<String> uuids = new ArrayList<String>();
            uuids.add(l3inv.getUuid());
            l3inv = deployer.getApi().listL3Network(uuids).get(0);
            deployer.l3Networks.put(l3inv.getName(), l3inv);
        }
    }

}
