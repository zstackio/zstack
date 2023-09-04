package org.zstack.header.vo;

import org.zstack.header.search.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SecurityLevelResourceRefVO.class)
public class SecurityLevelResourceRefInventory {
    private String resourceUuid;
    private String securityLevel;

    public static SecurityLevelResourceRefInventory valueOf(SecurityLevelResourceRefVO vo) {
        SecurityLevelResourceRefInventory inv = new SecurityLevelResourceRefInventory();
        inv.resourceUuid = vo.getResourceUuid();
        inv.securityLevel = vo.getSecurityLevel();
        return inv;
    }

    public static List<SecurityLevelResourceRefInventory> valueOf(Collection<SecurityLevelResourceRefVO> vos) {
        return vos.stream().map(SecurityLevelResourceRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }
}
