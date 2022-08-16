package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Inventory(mappingVOClass = SdnControllerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = HardwareL2VxlanNetworkPoolInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "sdnControllerUuid"),
})
@PythonClassInventory
public class SdnControllerInventory implements Serializable {
    private String uuid;
    private String vendorType;
    private String name;
    private String description;
    private String ip;
    private String username;
    @NoLogging
    private String password;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<SdnVniRange> vniRanges;
    private List<HardwareL2VxlanNetworkPoolInventory> vxlanPools;

    public static SdnControllerInventory valueOf(SdnControllerVO vo) {
        SdnControllerInventory inv = new SdnControllerInventory();
        inv.setUuid(vo.getUuid());
        inv.setVendorType(vo.getVendorType());
        inv.setDescription(vo.getDescription());
        inv.setName(vo.getName());
        inv.setIp(vo.getIp());
        inv.setUsername(vo.getUsername());
        inv.setPassword(vo.getPassword());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.vniRanges = new ArrayList<>();
        if (vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            List<Map<String, String>> tokenList = SdnControllerSystemTags.H3C_VNI_RANGE.getTokensOfTagsByResourceUuid(vo.getUuid());
            for (Map<String, String> tokens : tokenList) {
                SdnVniRange range = new SdnVniRange();
                range.startVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.H3C_START_VNI_TOKEN));
                range.endVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.H3C_END_VNI_TOKEN));
                inv.vniRanges.add(range);
            }
        }
        inv.setVxlanPools(HardwareL2VxlanNetworkPoolInventory.valueOf2(vo.getVxlanPools()));
        return inv;
    }

    public static List<SdnControllerInventory> valueOf(Collection<SdnControllerVO> vos) {
        List<SdnControllerInventory> lst = new ArrayList<SdnControllerInventory>(vos.size());
        for (SdnControllerVO vo : vos) {
            lst.add(SdnControllerInventory.valueOf(vo));
        }
        return lst;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVendorType() {
        return vendorType;
    }

    public void setVendorType(String vendorType) {
        this.vendorType = vendorType;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<SdnVniRange> getVniRanges() {
        return vniRanges;
    }

    public void setVniRanges(List<SdnVniRange> vniRanges) {
        this.vniRanges = vniRanges;
    }

    public List<HardwareL2VxlanNetworkPoolInventory> getVxlanPools() {
        return vxlanPools;
    }

    public void setVxlanPools(List<HardwareL2VxlanNetworkPoolInventory> vxlanPools) {
        this.vxlanPools = vxlanPools;
    }
}
