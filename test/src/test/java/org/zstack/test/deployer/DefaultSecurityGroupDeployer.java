package org.zstack.test.deployer;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SecurityGroupConfig;
import org.zstack.test.deployer.schema.SecurityGroupRuleConfig;

import java.util.ArrayList;
import java.util.List;

public class DefaultSecurityGroupDeployer implements SecurityGroupDeployer<SecurityGroupConfig> {

    @Override
    public Class<SecurityGroupConfig> getSupportedDeployerClassType() {
        return SecurityGroupConfig.class;
    }

    @Override
    public void deploy(List<SecurityGroupConfig> securityGroups, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (SecurityGroupConfig sc : securityGroups) {
            SecurityGroupInventory scinv = new SecurityGroupInventory();
            scinv.setName(sc.getName());
            scinv.setDescription(sc.getDescription());
            SessionInventory session = null;
            if (sc.getAccountRef() != null) {
                session = deployer.loginByAccountRef(sc.getAccountRef(), config);
            }
            if (session != null) {
                scinv = deployer.getApi().createSecurityGroupByFullConfig(scinv, session);
            } else {
                scinv = deployer.getApi().createSecurityGroupByFullConfig(scinv);
            }
            List<SecurityGroupRuleAO> aos = new ArrayList<SecurityGroupRuleAO>();
            for (SecurityGroupRuleConfig rc : sc.getRule()) {
                SecurityGroupRuleAO ao = new SecurityGroupRuleAO();
                ao.setAllowedCidr(rc.getAllowedCidr());
                ao.setEndPort(rc.getEndPort().intValue());
                ao.setStartPort(rc.getStartPort().intValue());
                ao.setProtocol(rc.getProtocol());
                ao.setType(rc.getType());
                aos.add(ao);
            }

            if (!aos.isEmpty()) {
                if (session != null) {
                    scinv = deployer.getApi().addSecurityGroupRuleByFullConfig(scinv.getUuid(), aos, session);
                } else {
                    scinv = deployer.getApi().addSecurityGroupRuleByFullConfig(scinv.getUuid(), aos);
                }
            }

            for (String l3nwName : sc.getL3NetworkRef()) {
                L3NetworkInventory l3nw = deployer.l3Networks.get(l3nwName);
                assert l3nw != null;
                scinv = deployer.getApi().attachSecurityGroupToL3Network(scinv.getUuid(), l3nw.getUuid());
            }
            deployer.securityGroups.put(scinv.getName(), scinv);
        }
    }

}
