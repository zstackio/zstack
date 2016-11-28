package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmFirewallRuleTO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.RangeSet.Range;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingRuleTestValidator {
    private static final CLogger logger = Utils.getLogger(PortForwardingRuleTestValidator.class);

    @Autowired
    private DatabaseFacade dbf;

    public boolean compare(PortForwardingRuleTO to, PortForwardingRuleInventory inv) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.ip);
        q.add(VmNicVO_.uuid, Op.EQ, inv.getVmNicUuid());
        String privateIp = q.findValue();

        boolean ret;
        if (to.getAllowedCidr() == null && inv.getAllowedCidr() == null) {
            ret = (to.getPrivateIp().equals(privateIp)
                    && to.getPrivatePortEnd() == inv.getPrivatePortEnd() && to.getPrivatePortStart() == inv.getPrivatePortStart()
                    && to.getProtocolType().equals(inv.getProtocolType())
                    && to.getVipPortStart() == inv.getVipPortStart() && to.getVipPortStart() == inv.getVipPortStart());
        } else {
            ret = (to.getAllowedCidr().equals(inv.getAllowedCidr()) && to.getPrivateIp().equals(privateIp)
                    && to.getPrivatePortEnd() == inv.getPrivatePortEnd() && to.getPrivatePortStart() == inv.getPrivatePortStart()
                    && to.getProtocolType().equals(inv.getProtocolType())
                    && to.getVipPortStart() == inv.getVipPortStart() && to.getVipPortStart() == inv.getVipPortStart());
        }

        if (!ret) {
            logger.warn(String.format("port forwarding rule mismatching:\n" +
                    "expected: %s\n" +
                    "actual: %s\n", JSONObjectUtil.toJsonString(to), JSONObjectUtil.toJsonString(inv)));
        }

        return ret;
    }

    public void validate(List<PortForwardingRuleTO> actual, Collection<PortForwardingRuleInventory> expected) {
        for (PortForwardingRuleInventory e : expected) {
            boolean has = false;
            for (PortForwardingRuleTO a : actual) {
                if (compare(a, e)) {
                    has = true;
                }
            }

            if (!has) {
                logger.warn(String.format("can not find expected rule:\n%s, actual dump:\n%s", JSONObjectUtil.toJsonString(e), JSONObjectUtil.toJsonString(actual)));
            }

            Assert.assertTrue(has);
        }
    }

    private ApplianceVmFirewallRuleTO findFirewall(final ApplianceVmSimulatorConfig config, final PortForwardingRuleInventory rule) {
        final VipVO vip = dbf.findByUuid(rule.getVipUuid(), VipVO.class);
        return CollectionUtils.find(config.firewallRules, new Function<ApplianceVmFirewallRuleTO, ApplianceVmFirewallRuleTO>() {
            @Override
            public ApplianceVmFirewallRuleTO call(ApplianceVmFirewallRuleTO arg) {
                String toIdentity = String.format("%s-%s-%s-%s-%s", arg.getStartPort(), arg.getEndPort(), arg.getProtocol().toLowerCase(), arg.getAllowCidr(), arg.getDestIp());
                String ruleIdentity = String.format("%s-%s-%s-%s-%s", rule.getVipPortStart(), rule.getVipPortEnd(), rule.getProtocolType().toLowerCase(), rule.getAllowedCidr(), vip == null ? null : vip.getIp());
                if (!toIdentity.equals(ruleIdentity)) {
                    return null;
                }

                Range r1 = new Range(Long.valueOf(arg.getStartPort()), Long.valueOf(arg.getEndPort()));
                Range r2 = new Range(Long.valueOf(rule.getVipPortStart()), Long.valueOf(rule.getVipPortEnd()));
                return r1.isOverlap(r2) ? arg : null;
            }
        });
    }

    public void noFirewall(ApplianceVmSimulatorConfig config, final PortForwardingRuleInventory rule) {
        ApplianceVmFirewallRuleTO to = findFirewall(config, rule);
        Assert.assertNull(to);
    }

    public void hasFirewall(ApplianceVmSimulatorConfig config, final PortForwardingRuleInventory rule) {
        ApplianceVmFirewallRuleTO to = findFirewall(config, rule);
        Assert.assertNotNull(to);
    }
}
