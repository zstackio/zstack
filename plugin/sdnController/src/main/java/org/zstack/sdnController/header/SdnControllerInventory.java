package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.sdnController.SdnControllerSystemTags;
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

    public SdnControllerInventory() {
    }

    public SdnControllerInventory(SdnControllerVO vo) {
        this.setUuid(vo.getUuid());
        this.setVendorType(vo.getVendorType());
        this.setVendorVersion(vo.getVendorVersion());
        this.setDescription(vo.getDescription());
        this.setName(vo.getName());
        this.setIp(vo.getIp());
        this.setUsername(vo.getUsername());
        this.setPassword(vo.getPassword());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.vniRanges = new ArrayList<>();
        if (vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            List<Map<String, String>> tokenList = SdnControllerSystemTags.VNI_RANGE.getTokensOfTagsByResourceUuid(vo.getUuid());
            for (Map<String, String> tokens : tokenList) {
                SdnVniRange range = new SdnVniRange();
                range.startVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.START_VNI_TOKEN));
                range.endVni = Integer.valueOf(tokens.get(SdnControllerSystemTags.END_VNI_TOKEN));
                this.vniRanges.add(range);
            }
        }
        this.setVxlanPools(HardwareL2VxlanNetworkPoolInventory.valueOf2(vo.getVxlanPools()));
        this.setHostRefs(SdnControllerHostRefInventory.valueOf(vo.getHostRefVOS()));
    }

    public static SdnControllerInventory valueOf(SdnControllerVO vo) {
        return new SdnControllerInventory(vo);
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
