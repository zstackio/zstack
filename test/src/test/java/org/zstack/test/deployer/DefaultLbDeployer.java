package org.zstack.test.deployer;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.sdk.AddVmNicToLoadBalancerAction;
import org.zstack.sdk.CreateLoadBalancerAction;
import org.zstack.sdk.CreateLoadBalancerListenerAction;
import org.zstack.sdk.CreateVipAction;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.LbConfig;
import org.zstack.test.deployer.schema.LbListenerConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by frank on 8/10/2015.
 */
public class DefaultLbDeployer implements LbDeployer<LbConfig> {
    @Override
    public void deploy(List<LbConfig> lbs, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (LbConfig lb : lbs) {
            L3NetworkInventory pl3 = deployer.l3Networks.get(lb.getPublicL3NetworkRef());
            assert pl3 != null;

            SessionInventory session = lb.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(lb.getAccountRef(), config);

            CreateVipAction vaction = new CreateVipAction();
            vaction.name = "vip";
            vaction.sessionId = session.getUuid();
            vaction.l3NetworkUuid = pl3.getUuid();
            CreateVipAction.Result res = vaction.call().throwExceptionIfError();
            VipInventory vip = JSONObjectUtil.rehashObject(res.value.getInventory(), VipInventory.class);

            CreateLoadBalancerAction laction = new CreateLoadBalancerAction();
            laction.name = lb.getName();
            laction.vipUuid = vip.getUuid();
            laction.systemTags = lb.getTag();
            laction.sessionId = session.getUuid();
            CreateLoadBalancerAction.Result lres = laction.call().throwExceptionIfError();

            LoadBalancerInventory lbinv = JSONObjectUtil.rehashObject(lres.value.getInventory(), LoadBalancerInventory.class);
            deployer.loadBalancers.put(lbinv.getName(), lbinv);

            for (LbListenerConfig lcfg : lb.getListener()) {
                CreateLoadBalancerListenerAction a = new CreateLoadBalancerListenerAction();
                a.name = lcfg.getName();
                a.description = lcfg.getDescription();
                a.loadBalancerUuid = lbinv.getUuid();
                a.protocol = lcfg.getProtocol();
                a.instancePort = lcfg.getInstancePort().intValue();
                a.loadBalancerPort = lcfg.getLoadBalancerPort().intValue();
                CreateLoadBalancerListenerAction.Result llres = a.call().throwExceptionIfError();
                LoadBalancerListenerInventory inv = JSONObjectUtil.rehashObject(llres.value.getInventory(), LoadBalancerListenerInventory.class);
                deployer.loadBalancerListeners.put(inv.getName(), inv);

                for (String nicRef : lcfg.getVmNicRef()) {
                    if (!nicRef.contains(":")) {
                        throw new CloudRuntimeException(String.format("nicRef[%s] must be in format vmName:L3Ref", nicRef));
                    }

                    String[] refs = nicRef.split(":");
                    String vmName = refs[0];
                    String l3Name = refs[1];

                    L3NetworkInventory l3 = deployer.l3Networks.get(l3Name);
                    assert l3 != null : String.format("cannot find l3Network[name:%s]", l3Name);
                    VmInstanceInventory vm = deployer.vms.get(vmName);
                    assert vm != null : String.format("cannot find vm[name:%s]", vmName);
                    VmNicInventory nic = vm.findNic(l3.getUuid());
                    assert nic != null : String.format("cannot find nic[l3name: %s] of vm[name:%s]", l3Name, vmName);

                    AddVmNicToLoadBalancerAction av = new AddVmNicToLoadBalancerAction();
                    av.listenerUuid = inv.getUuid();
                    av.vmNicUuids = asList(nic.getUuid());
                    av.sessionId = session.getUuid();
                    AddVmNicToLoadBalancerAction.Result avres = av.call().throwExceptionIfError();

                    inv = JSONObjectUtil.rehashObject(avres.value.getInventory(), LoadBalancerListenerInventory.class);
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
