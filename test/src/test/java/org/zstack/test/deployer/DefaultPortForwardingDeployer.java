package org.zstack.test.deployer;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.PortForwardingConfig;

import java.util.List;

public class DefaultPortForwardingDeployer implements PortForwardingDeployer<PortForwardingConfig> {

    @Override
    public Class<PortForwardingConfig> getSupportedDeployerClassType() {
        return PortForwardingConfig.class;
    }

    private VmNicInventory getVmNicUuidForL3OfVm(String l3Uuid, VmInstanceInventory vm) {
        for (VmNicInventory nic : vm.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                return nic;
            }
        }
        return null;
    }

    @Override
    public void deploy(List<PortForwardingConfig> portForwardingRules, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (PortForwardingConfig pf : portForwardingRules) {
            VmInstanceInventory vm = deployer.vms.get(pf.getVmRef());
            L3NetworkInventory publicL3 = deployer.l3Networks.get(pf.getPublicL3NetworkRef());
            String vmNicUuid = null;
            if (vm != null) {
                L3NetworkInventory l3 = deployer.l3Networks.get(pf.getPrivateL3NetworkRef());
                assert l3 != null;
                VmNicInventory nic = getVmNicUuidForL3OfVm(l3.getUuid(), vm);
                assert nic != null;
                vmNicUuid = nic.getUuid();
            }

            VipInventory pubIp = deployer.getApi().acquireIp(publicL3.getUuid());
            PortForwardingRuleInventory pfinv = new PortForwardingRuleInventory();
            pfinv.setAllowedCidr(pf.getAllowedCidr());
            pfinv.setDescription(pf.getDescription());
            pfinv.setName(pf.getName());
            pfinv.setPrivatePortEnd((int) pf.getPrivatePortEnd());
            pfinv.setPrivatePortStart(pf.getPrivatePortStart().intValue());
            pfinv.setProtocolType(pf.getProtocolType());
            pfinv.setVipUuid(pubIp.getUuid());
            pfinv.setVipPortEnd((int) pf.getPublicPortEnd());
            pfinv.setVipPortStart(pf.getPublicPortStart().intValue());
            pfinv.setVmNicUuid(vmNicUuid);

            SessionInventory session = pf.getAccountRef() == null ? null : deployer.loginByAccountRef(pf.getAccountRef(), config);

            pfinv = deployer.getApi().createPortForwardingRuleByFullConfig(pfinv, session);
            deployer.portForwardingRules.put(pfinv.getName(), pfinv);
        }
    }

}
