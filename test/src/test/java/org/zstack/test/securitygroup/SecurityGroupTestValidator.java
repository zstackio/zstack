package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.securitygroup.RuleTO;
import org.zstack.network.securitygroup.SecurityGroupRuleInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class SecurityGroupTestValidator {
    private static CLogger logger = Utils.getLogger(SecurityGroupTestValidator.class);

    public static void validate(SecurityGroupRuleTO actual, List<SecurityGroupRuleInventory> expected) {
        List<String> rules = new ArrayList<String>();

        StringBuilder sb = new StringBuilder("\n*************************** security group validator ******************************");
        sb.append(String.format("\nexpected rules:\n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual rules:\n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append("\n*************************************************************************");
        logger.debug(sb.toString());

        for (RuleTO r : actual.getRules()) {
            rules.add(r.toString());
        }

        Assert.assertEquals(expected.size(), rules.size());

        for (SecurityGroupRuleInventory r : expected) {
            Assert.assertTrue(rules.contains(r.toString()));
        }
    }

    public static void validateInternalIpIn(SecurityGroupRuleTO actual, String internalIp, List<SecurityGroupRuleInventory> expected) {
        List<String> rules = new ArrayList<String>();

        StringBuilder sb = new StringBuilder("\n*************************** security group validator ******************************");
        sb.append(String.format("\nexpected rules:\n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual rules:\n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append(String.format("\ninclusive internal ip: %s", internalIp));
        sb.append("\n*************************************************************************");
        logger.debug(sb.toString());

        Assert.assertEquals(expected.size(), rules.size());

        for (SecurityGroupRuleInventory r : expected) {
            Assert.assertTrue(rules.contains(r.toString()));
        }
    }

    public static void validateInternalIpNotIn(SecurityGroupRuleTO actual, String internalIp, List<SecurityGroupRuleInventory> expected) {
        List<String> rules = new ArrayList<String>();

        StringBuilder sb = new StringBuilder("\n*************************** security group validator ******************************");
        sb.append(String.format("\nexpected rules:\n%s", JSONObjectUtil.toJsonString(expected)));
        sb.append(String.format("\nactual rules:\n%s", JSONObjectUtil.toJsonString(actual)));
        sb.append(String.format("\nexclusive internal ip: %s", internalIp));
        sb.append("\n*************************************************************************");
        logger.debug(sb.toString());

        Assert.assertEquals(expected.size(), rules.size());

        for (SecurityGroupRuleInventory r : expected) {
            Assert.assertTrue(rules.contains(r.toString()));
        }
    }

    public static VmNicInventory getVmNicOnSpecificL3Network(List<VmNicInventory> nics, String l3Uuid) {
        for (VmNicInventory nic : nics) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                VmNicVO vo = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid()).find();
                // this will add internalName
                return VmNicInventory.valueOf(vo);
            }
        }
        return null;
    }
}
