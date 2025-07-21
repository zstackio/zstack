package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.sdnController.h3cVcfc.H3cVcfcSdnControllerSystemTags;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Inventory(mappingVOClass = SdnControllerVO.class)
@PythonClassInventory
public class SdnControllerInventory implements Serializable {
    private String uuid;
    private String vendorType;
    private String vendorVersion;
    private String name;
    private String description;
    private String ip;
    private String username;
    @NoLogging
    private String password;
    private SdnControllerStatus status;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<SdnVniRange> vniRanges;
    private List<HardwareL2VxlanNetworkPoolInventory> vxlanPools;
    private List<SdnControllerHostRefInventory> hostRefs;

    public static SdnControllerInventory valueOf(SdnControllerVO vo) {
        SdnControllerInventory inv = new SdnControllerInventory();
        inv.setUuid(vo.getUuid());
        inv.setVendorType(vo.getVendorType());
        inv.setVendorVersion(vo.getVendorVersion());
        inv.setDescription(vo.getDescription());
        inv.setName(vo.getName());
        inv.setIp(vo.getIp());
        inv.setUsername(vo.getUsername());
        inv.setPassword(vo.getPassword());
        inv.setStatus(vo.getStatus());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.vniRanges = new ArrayList<>();
        if (vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            List<Map<String, String>> tokenList = H3cVcfcSdnControllerSystemTags.H3C_VNI_RANGE.getTokensOfTagsByResourceUuid(vo.getUuid());
            for (Map<String, String> tokens : tokenList) {
                SdnVniRange range = new SdnVniRange();
                range.startVni = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_START_VNI_TOKEN));
                range.endVni = Integer.valueOf(tokens.get(H3cVcfcSdnControllerSystemTags.H3C_END_VNI_TOKEN));
                inv.vniRanges.add(range);
            }
        }
        inv.setVxlanPools(HardwareL2VxlanNetworkPoolInventory.valueOf2(vo.getVxlanPools()));
        inv.setHostRefs(SdnControllerHostRefInventory.valueOf(vo.getHostRefVOS()));
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

    public String getVendorVersion() {
        return vendorVersion;
    }

    public void setVendorVersion(String vendorVersion) {
        this.vendorVersion = vendorVersion;
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

    public SdnControllerStatus getStatus() {
        return status;
    }

    public void setStatus(SdnControllerStatus status) {
        this.status = status;
    }

    public List<SdnControllerHostRefInventory> getHostRefs() {
        return hostRefs;
    }

    public void setHostRefs(List<SdnControllerHostRefInventory> hostRefs) {
        this.hostRefs = hostRefs;
    }
}
