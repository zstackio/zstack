package org.zstack.test.deployer;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.EipConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 */
public class DefaultEipDeployer implements EipDeployer<EipConfig> {
    @Override
    public void deploy(List<EipConfig> eips, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (EipConfig eip : eips) {
            L3NetworkInventory publ3 = deployer.l3Networks.get(eip.getPublicL3NetworkRef());
            assert publ3 != null;
            final L3NetworkInventory privl3 = deployer.l3Networks.get(eip.getPrivateL3NetworkRef());
            assert privl3 != null;

            String nicUuid = null;
            if (eip.getVmRef() != null) {
                VmInstanceInventory vm = deployer.vms.get(eip.getVmRef());
                assert vm != null;
                VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        if (arg.getL3NetworkUuid().equals(arg.getL3NetworkUuid())) {
                            return arg;
                        }
                        return null;
                    }
                });
                assert nic != null;
                nicUuid = nic.getUuid();
            }

            SessionInventory session = eip.getAccountRef() == null ? null : deployer.loginByAccountRef(eip.getAccountRef(), config);

            VipInventory vip = deployer.getApi().acquireIp(publ3.getUuid());
            EipInventory inv = deployer.getApi().createEip(eip.getName(), vip.getUuid(), nicUuid, session);
            deployer.eips.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<EipConfig> getSupportedDeployerClassType() {
        return EipConfig.class;
    }
}
