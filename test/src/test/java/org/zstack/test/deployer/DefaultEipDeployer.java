package org.zstack.test.deployer;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.sdk.CreateEipAction;
import org.zstack.sdk.CreateVipAction;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.EipConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

public class DefaultEipDeployer implements EipDeployer<EipConfig> {
    @Override
    public void deploy(List<EipConfig> eips, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (EipConfig eip : eips) {
            L3NetworkInventory publ3 = deployer.l3Networks.get(eip.getPublicL3NetworkRef());
            assert publ3 != null;

            String nicUuid = null;
            if (eip.getVmRef() != null) {
                final L3NetworkInventory privl3 = deployer.l3Networks.get(eip.getPrivateL3NetworkRef());
                assert privl3 != null;

                VmInstanceInventory vm = deployer.vms.get(eip.getVmRef());
                assert vm != null;
                VmNicInventory nic = vm.findNic(privl3.getUuid());
                assert nic != null;
                nicUuid = nic.getUuid();
            }

            SessionInventory session = eip.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(eip.getAccountRef(), config);

            CreateVipAction vipAction = new CreateVipAction();
            vipAction.name = "vip";
            vipAction.l3NetworkUuid = publ3.getUuid();
            vipAction.sessionId = session.getUuid();
            CreateVipAction.Result res = vipAction.call().throwExceptionIfError();
            VipInventory vip = JSONObjectUtil.rehashObject(res.value.getInventory(), VipInventory.class);

            CreateEipAction eipAction = new CreateEipAction();
            eipAction.name = eip.getName();
            eipAction.vipUuid = vip.getUuid();
            eipAction.sessionId = session.getUuid();
            eipAction.vmNicUuid = nicUuid;
            CreateEipAction.Result eres = eipAction.call().throwExceptionIfError();

            EipInventory inv = JSONObjectUtil.rehashObject(eres.value.getInventory(), EipInventory.class);
            deployer.eips.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<EipConfig> getSupportedDeployerClassType() {
        return EipConfig.class;
    }
}
