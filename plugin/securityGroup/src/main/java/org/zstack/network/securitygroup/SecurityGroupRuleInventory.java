package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory
 * inventory for security group rule
 *
 * @category security group
 *
 * @example
 * {
"uuid": "02bc62abee88444ca3e2c434a1b8fdea",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
}
 *
 * @since 0.1.0
 */
@Inventory(mappingVOClass = SecurityGroupRuleVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "securityGroup", inventoryClass = SecurityGroupInventory.class,
                foreignKey = "securityGroupUuid", expandedInventoryKey = "uuid")
})
public class SecurityGroupRuleInventory {
    /**
     * @desc rule uuid
     */
    private String uuid;
    /**
     * @desc security group uuid
     */
    private String securityGroupUuid;
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
     * end port
     * @choices 0 - 65535
     */
    private Integer endPort;
    /**
     * @desc
     * network protocol type
     * @choices
     * - TCP
     * - UDP
     * - ICMP
     */
    private String protocol;

    private String state;
    /**
     * @desc source CIDR the rule applies to. If set, the rule only applies to traffic from this CIDR. If omitted, the rule
     * applies to all traffic
     * @nullable
     */
    private String allowedCidr;
    /**
     * @desc remote security group uuids for rules between groups
     */
    private String remoteSecurityGroupUuid;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public SecurityGroupRuleInventory() {
    }
    
    protected SecurityGroupRuleInventory(SecurityGroupRuleVO vo) {
        this.setState(vo.getState().toString());
        this.setUuid(vo.getUuid());
        this.setSecurityGroupUuid(vo.getSecurityGroupUuid());
        this.setType(vo.getType().toString());
        this.setStartPort(vo.getStartPort());
        this.setEndPort(vo.getEndPort());
        this.setProtocol(vo.getProtocol().toString());
        this.setAllowedCidr(vo.getAllowedCidr());
        this.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }
    
    public static SecurityGroupRuleInventory valueOf(SecurityGroupRuleVO vo) {
        SecurityGroupRuleInventory inv = new SecurityGroupRuleInventory(vo);
        return inv;
    }
    
    public static List<SecurityGroupRuleInventory> valueOf(Collection<SecurityGroupRuleVO> vos) {
        List<SecurityGroupRuleInventory> invs = new ArrayList<SecurityGroupRuleInventory>(vos.size());
        for (SecurityGroupRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
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

    public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }

    public String getRemoteSecurityGroupUuid() {
        return remoteSecurityGroupUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("startPort: %s,", this.startPort));
        sb.append(String.format("endPort: %s,", this.endPort));
        sb.append(String.format("allowedCidr: %s", this.allowedCidr));
        return sb.toString();
    }
}
