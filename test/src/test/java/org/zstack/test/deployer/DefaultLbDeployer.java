package org.zstack.test.deployer;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.LbConfig;
import org.zstack.test.deployer.schema.LbListenerConfig;

import java.util.List;

/**
 * Created by frank on 8/10/2015.
 */
public class DefaultLbDeployer implements LbDeployer<LbConfig> {
    @Override
    public void deploy(List<LbConfig> lbs, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api  = deployer.getApi();
        for (LbConfig lb : lbs) {
            L3NetworkInventory pl3 = deployer.l3Networks.get(lb.getPublicL3NetworkRef());
            assert pl3 != null;

            SessionInventory session = lb.getAccountRef() == null ? null : deployer.loginByAccountRef(lb.getAccountRef(), config);
            VipInventory vip = api.acquireIp(pl3.getUuid());
            LoadBalancerInventory lbinv = api.createLoadBalancer(lb.getName(), vip.getUuid(), lb.getTag(), session);
            deployer.loadBalancers.put(lbinv.getName(), lbinv);

            for (LbListenerConfig lcfg : lb.getListener()) {
                LoadBalancerListenerInventory inv = new LoadBalancerListenerInventory();
                inv.setName(lcfg.getName());
                inv.setDescription(lcfg.getDescription());
                inv.setLoadBalancerUuid(lbinv.getUuid());
                inv.setProtocol(lcfg.getProtocol());
                inv.setInstancePort(lcfg.getInstancePort().intValue());
                inv.setLoadBalancerPort(lcfg.getLoadBalancerPort().intValue());
                inv = api.createLoadBalancerListener(inv, session);
                deployer.loadBalancerListeners.put(inv.getName(), inv);

                for (String nicRef : lcfg.getVmNicRef()) {
                    if (!nicRef.contains(":")) {
                        throw new CloudRuntimeException(String.format("nicRef[%s] must be in format vmName:L3Ref", nicRef));
                    }

                    String[] refs = nicRef.split(":");
                    String vmName = refs[0];
                    String l3Name = refs[1];

                    L3NetworkInventory l3 = deployer.l3Networks.get(l3Name);
                    assert l3 != null: String.format("cannot find l3Network[name:%s]", l3Name);
                    VmInstanceInventory vm = deployer.vms.get(vmName);
                    assert vm != null: String.format("cannot find vm[name:%s]", vmName);
                    VmNicInventory nic = vm.findNic(l3.getUuid());
                    assert nic != null: String.format("cannot find nic[l3name: %s] of vm[name:%s]", l3Name, vmName);

                    inv = api.addVmNicToLoadBalancerListener(inv.getUuid(), nic.getUuid(), session);
                    deployer.loadBalancerListeners.put(inv.getName(), inv);
                }

                deployer.loadBalancers.put(lbinv.getName(), lbinv);
            }
        }
    }

    @Override
    public Class getSupportedDeployerClassType() {
        return LbConfig.class;
    }
}
