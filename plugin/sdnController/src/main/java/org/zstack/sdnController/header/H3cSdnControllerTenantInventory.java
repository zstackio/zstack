package org.zstack.sdnController.header;

import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by boce.wang on 06/16/2025.
 */
@Inventory(mappingVOClass = H3cSdnControllerTenantVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sdnController", inventoryClass = SdnControllerInventory.class,
                foreignKey = "sdnControllerUuid", expandedInventoryKey = "uuid")
})
public class H3cSdnControllerTenantInventory {
    private String uuid;
    private String sdnControllerUuid;
    private String tenantUuid;
    private String vdsUuid;
    private String tenantName;
    private String vdsName;
    private String cloudDomainName;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static H3cSdnControllerTenantInventory valueOf(H3cSdnControllerTenantVO vo) {
        H3cSdnControllerTenantInventory inv = new H3cSdnControllerTenantInventory();
        inv.setUuid(vo.getUuid());
        inv.setSdnControllerUuid(vo.getSdnControllerUuid());
        inv.setTenantUuid(vo.getTenantUuid());
        inv.setVdsUuid(vo.getVdsUuid());
        inv.setTenantName(vo.getTenantName());
        inv.setVdsName(vo.getVdsName());
        inv.setCloudDomainName(vo.getCloudDomainName());
        inv.setState(vo.getState());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<H3cSdnControllerTenantInventory> valueOf(Collection<H3cSdnControllerTenantVO> vos) {
        List<H3cSdnControllerTenantInventory> invs = new ArrayList<H3cSdnControllerTenantInventory>();
        for (H3cSdnControllerTenantVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getTenantUuid() {
        return tenantUuid;
    }

    public void setTenantUuid(String tenantUuid) {
        this.tenantUuid = tenantUuid;
    }

    public String getVdsUuid() {
        return vdsUuid;
    }

    public void setVdsUuid(String vdsUuid) {
        this.vdsUuid = vdsUuid;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getVdsName() {
        return vdsName;
    }

    public void setVdsName(String vdsName) {
        this.vdsName = vdsName;
    }

    public String getCloudDomainName() {
        return cloudDomainName;
    }

    public void setCloudDomainName(String cloudDomainName) {
        this.cloudDomainName = cloudDomainName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
