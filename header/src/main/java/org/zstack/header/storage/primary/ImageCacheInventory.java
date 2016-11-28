package org.zstack.header.storage.primary;

import java.sql.Timestamp;

public class ImageCacheInventory {
    private long id;
    private String primaryStorageUuid;
    private String imageUuid;
    private String installUrl;
    private long size;
    private String md5sum;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ImageCacheInventory valueOf(ImageCacheVO vo) {
        ImageCacheInventory inv = new ImageCacheInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setId(vo.getId());
        inv.setImageUuid(vo.getImageUuid());
        inv.setInstallUrl(vo.getInstallUrl());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setMd5sum(vo.getMd5sum());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setSize(vo.getSize());
        inv.setState(vo.getState().toString());
        return inv;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
