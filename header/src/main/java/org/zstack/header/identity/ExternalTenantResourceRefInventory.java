package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Inventory for ExternalTenantResourceRefVO
 */
@PythonClassInventory
@Inventory(mappingVOClass = ExternalTenantResourceRefVO.class)
public class ExternalTenantResourceRefInventory {
    private long id;
    private String source;
    private String tenantId;
    private String userId;
    private String resourceUuid;
    private String accountUuid;
    private String resourceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public ExternalTenantResourceRefInventory() {
    }

    public static List<ExternalTenantResourceRefInventory> valueOf(Collection<ExternalTenantResourceRefVO> vos) {
        return vos.stream().map(ExternalTenantResourceRefInventory::valueOf)
                .collect(Collectors.toList());
    }

    public static ExternalTenantResourceRefInventory valueOf(ExternalTenantResourceRefVO vo) {
        return new ExternalTenantResourceRefInventory(vo);
    }

    public ExternalTenantResourceRefInventory(ExternalTenantResourceRefVO vo) {
        this.id = vo.getId();
        this.source = vo.getSource();
        this.tenantId = vo.getTenantId();
        this.userId = vo.getUserId();
        this.resourceUuid = vo.getResourceUuid();
        this.accountUuid = vo.getAccountUuid();
        this.resourceType = vo.getResourceType();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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
}
