package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQuerySecurityGroupRuleReply extends APIQueryReply {
    private List<SecurityGroupRuleInventory> inventories;

    public List<SecurityGroupRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupRuleInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySecurityGroupRuleReply __example__() {
        APIQuerySecurityGroupRuleReply reply = new APIQuerySecurityGroupRuleReply();
        SecurityGroupRuleInventory rule = new SecurityGroupRuleInventory();
        rule.setUuid(uuid());
        rule.setAllowedCidr("0.0.0.0/0");
        rule.setEndPort(22);
        rule.setStartPort(22);
        rule.setProtocol("TCP");
        rule.setSecurityGroupUuid(uuid());
        rule.setState(SecurityGroupRuleState.Enabled.toString());
        rule.setType("Ingress");
        rule.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        rule.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(rule));
        reply.setSuccess(true);
        return reply;
    }

}
