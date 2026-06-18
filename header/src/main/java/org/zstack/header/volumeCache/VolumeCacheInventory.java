package org.zstack.header.volumeCache;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VolumeCacheVO.class)
@PythonClassInventory
public class VolumeCacheInventory implements Serializable {
    private static final long serialVersionUID = 1L;
    private String uuid;
    private String volumeUuid;
    private String poolUuid;
    private String installPath;
    private VolumeCacheMode cacheMode;
    private VolumeCacheStatus status;
    private Long virtualSize;
    private Long actualSize;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VolumeCacheInventory valueOf(VolumeCacheVO vo) {
        VolumeCacheInventory inv = new VolumeCacheInventory();
        inv.setUuid(vo.getUuid());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setPoolUuid(vo.getPoolUuid());
        inv.setInstallPath(vo.getInstallPath());
        inv.setCacheMode(vo.getCacheMode());
        inv.setStatus(vo.getStatus());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VolumeCacheInventory> valueOf(Collection<VolumeCacheVO> vos) {
        List<VolumeCacheInventory> invs = new ArrayList<>();
        for (VolumeCacheVO vo : vos) {
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

    public VolumeCacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(VolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public VolumeCacheStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeCacheStatus status) {
        this.status = status;
    }

    public Long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
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
