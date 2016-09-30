package org.zstack.appliancevm;

import org.zstack.header.agent.AgentCommand;
import org.zstack.header.agent.AgentResponse;

import java.util.List;

/**
 */
public class ApplianceVmCommands {
    public static class RefreshFirewallCmd extends AgentCommand {
        private List<ApplianceVmFirewallRuleTO> rules;

        public List<ApplianceVmFirewallRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<ApplianceVmFirewallRuleTO> rules) {
            this.rules = rules;
        }
    }

    public static class RefreshFirewallRsp extends AgentResponse {
    }

    public static class InitCmd extends AgentCommand {
    }

    public static class InitRsp extends AgentResponse {
    }
}
