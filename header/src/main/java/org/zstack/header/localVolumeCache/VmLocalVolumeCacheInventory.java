package org.zstack.header.localVolumeCache;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VmLocalVolumeCacheVO.class)
@PythonClassInventory
public class VmLocalVolumeCacheInventory implements Serializable {
    private String uuid;
    private String volumeUuid;
    private String poolUuid;
    private String installPath;
    private VmLocalVolumeCacheMode cacheMode;
    private VmLocalVolumeCacheState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmLocalVolumeCacheInventory valueOf(VmLocalVolumeCacheVO vo) {
        VmLocalVolumeCacheInventory inv = new VmLocalVolumeCacheInventory();
        inv.setUuid(vo.getUuid());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setPoolUuid(vo.getPoolUuid());
        inv.setInstallPath(vo.getInstallPath());
        inv.setCacheMode(vo.getCacheMode());
        inv.setState(vo.getState());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VmLocalVolumeCacheInventory> valueOf(Collection<VmLocalVolumeCacheVO> vos) {
        List<VmLocalVolumeCacheInventory> invs = new ArrayList<>();
        for (VmLocalVolumeCacheVO vo : vos) {
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

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public VmLocalVolumeCacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(VmLocalVolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public VmLocalVolumeCacheState getState() {
        return state;
    }

    public void setState(VmLocalVolumeCacheState state) {
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
