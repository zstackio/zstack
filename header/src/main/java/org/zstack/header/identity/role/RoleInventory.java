package org.zstack.header.identity.role;

import org.hibernate.Hibernate;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.resource.ResourceSourceConstant;
import org.zstack.header.resource.ResourceSourceRefInventory;
import org.zstack.header.resource.ResourceSourceRefVO;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = RoleVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sourceRef", inventoryClass = ResourceSourceRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "resourceUuid", hidden = true)
})
public class RoleInventory {
    private String uuid;
    private String name;
    private String description;
    private String identity;
    private RoleType type;
    private RoleState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<RolePolicyStatementInventory> statements;
    private List<RolePolicyRefInventory> policies;
    private String sourceCategory;
    private String sourceType;
    private String sourceName;
    private String syncType;
    private String externalUuid;
    private String externalType;

    protected RoleInventory(RoleVO vo) {
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setLastOpDate(vo.getLastOpDate());
        this.setType(vo.getType());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setUuid(vo.getUuid());
        this.setIdentity(vo.getIdentity());
        this.setStatements(RolePolicyStatementInventory.valueOf(vo.getStatements()));
        this.setPolicies(RolePolicyRefInventory.valueOf(vo.getPolicies()));
        applySourceInfo(this, initializedSourceRefs(vo));
    }

    public RoleInventory() {
    }

    public static RoleInventory valueOf(RoleVO vo) {
        RoleInventory inv = new RoleInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.type = vo.getType();
        inv.state = vo.getState();
        inv.description = vo.getDescription();
        inv.identity = vo.getIdentity();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.statements = RolePolicyStatementInventory.valueOf(vo.getStatements());
        inv.policies = RolePolicyRefInventory.valueOf(vo.getPolicies());
        applySourceInfo(inv, initializedSourceRefs(vo));
        return inv;
    }

    public static List<RoleInventory> valueOf(Collection<RoleVO> vos) {
        return vos.stream().map(RoleInventory::valueOf).collect(Collectors.toList());
    }

    public RoleState getState() {
        return state;
    }

    public void setState(RoleState state) {
        this.state = state;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public List<RolePolicyStatementInventory> getStatements() {
        return statements;
    }

    public void setStatements(List<RolePolicyStatementInventory> statements) {
        this.statements = statements;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<RolePolicyRefInventory> getPolicies() {
        return policies;
    }

    public void setPolicies(List<RolePolicyRefInventory> policies) {
        this.policies = policies;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getSourceCategory() {
        return sourceCategory;
    }

    public void setSourceCategory(String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSyncType() {
        return syncType;
    }

    public void setSyncType(String syncType) {
        this.syncType = syncType;
    }

    public String getExternalUuid() {
        return externalUuid;
    }

    public void setExternalUuid(String externalUuid) {
        this.externalUuid = externalUuid;
    }

    public String getExternalType() {
        return externalType;
    }

    public void setExternalType(String externalType) {
        this.externalType = externalType;
    }

    private static void applySourceInfo(RoleInventory inv, Collection<ResourceSourceRefVO> refs) {
        if (refs != null) {
            for (ResourceSourceRefVO ref : refs) {
                if (ResourceSourceConstant.SOURCE_TYPE_ZIAM.equals(ref.getSourceType()) &&
                        ResourceSourceConstant.SYNC_TYPE_SCIM.equals(ref.getSyncType())) {
                    inv.sourceCategory = ResourceSourceConstant.SOURCE_CATEGORY_ZIAM_SCIM;
                    inv.sourceType = ref.getSourceType();
                    inv.sourceName = ref.getSourceName();
                    inv.syncType = ref.getSyncType();
                    inv.externalUuid = ref.getExternalUuid();
                    inv.externalType = ref.getExternalType();
                    return;
                }
            }
        }
        inv.sourceCategory = ResourceSourceConstant.SOURCE_CATEGORY_LOCAL;
    }

    private static Collection<ResourceSourceRefVO> initializedSourceRefs(RoleVO vo) {
        return vo.getSourceRefs() != null && Hibernate.isInitialized(vo.getSourceRefs()) ? vo.getSourceRefs() : null;
    }
}
