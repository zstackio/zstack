package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 * @api
 * add rule to a security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"rules": [
{
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0"
},
{
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0"
}
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"rules": [
{
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0"
},
{
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0"
}
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
},
"timeout": 1800000,
"id": "c644a6d41e614ffeaa9e2112bf339b6b",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddSecurityGroupRuleEvent`
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/security-groups/{securityGroupUuid}/rules",
        method = HttpMethod.POST,
        responseClass = APIAddSecurityGroupRuleEvent.class,
        parameterName = "params"
)
public class APIAddSecurityGroupRuleMsg extends APIMessage {
    /**
     * @inventory
     * inventory for SecurityGroupRuleAO
     *
     * @category security group
     *
     * @example
     *
     *{
    "type": "Ingress",
    "startPort": 10,
    "endPort": 10,
    "protocol": "UDP",
    "allowedCidr": "192.168.0.1/0"
    }
     * @since 0.1.0
     */
    @PythonClassInventory
    public static class SecurityGroupRuleAO {
        /**
         * @desc
         * rule type
         *
         * - Ingress: for inbound traffic
         * - Egress: for outbound traffic
         * @choices
         * - Ingress
         * - Egress
         */
        private String type;
        /**
         * @desc
         * start port
         * @choices 0 - 65535
         */
        private Integer startPort;
        /**
         * @desc
         * end port. If omitted, endPort is set to startPort
         * @choices 0 - 65535
         * @nullable
         */
        private Integer endPort;
        /**
         * @desc network protocol type
         * @choices
         * - TCP
         * - UDP
         * - ICMP
         */
        private String protocol;
        /**
         * @desc source CIDR the rule applies to. If set, the rule only applies to traffic from this CIDR. If omitted, the rule
         * applies to all traffic
         * @nullable
         */
        private String allowedCidr;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getStartPort() {
            return startPort;
        }

        public void setStartPort(Integer startPort) {
            this.startPort = startPort;
        }

        public Integer getEndPort() {
            return endPort;
        }

        public void setEndPort(Integer endPort) {
            this.endPort = endPort;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getAllowedCidr() {
            return allowedCidr;
        }

        public void setAllowedCidr(String allowedCidr) {
            this.allowedCidr = allowedCidr;
        }
    }

    /**
     * @desc security group uuid
     */
    @APIParam(resourceType = SecurityGroupVO.class, checkAccount = true, operationTarget = true)
    private String securityGroupUuid;
    /**
     * @desc a list of :ref:`SecurityGroupRuleAO` that describe rules
     */
    @APIParam(nonempty = true)
    private List<SecurityGroupRuleAO> rules;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public List<SecurityGroupRuleAO> getRules() {
        return rules;
    }

    public void setRules(List<SecurityGroupRuleAO> rules) {
        this.rules = rules;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
    
}
