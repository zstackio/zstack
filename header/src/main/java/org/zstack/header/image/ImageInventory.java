package org.zstack.header.image;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.volume.VolumeInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ImageVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = ImageBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "imageUuid", hidden = true),
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "rootImageUuid"),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage")
})
public class ImageInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private String status;
    private Long size;
    private Long actualSize;
    private String md5Sum;
    private String url;
    private String mediaType;
    private String guestOsType;
    private String type;
    private String platform;
    private String architecture;
    private String format;
    private Boolean system;
    private Boolean virtio;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = ImageBackupStorageRefInventory.class,
            joinColumn = @JoinColumn(name = "imageUuid"))
    private List<ImageBackupStorageRefInventory> backupStorageRefs;
    private List<SystemTagInventory> systemTags;

    public ImageInventory() {

    }
    public ImageInventory(ImageVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        if (vo.getState() != null) {
            this.state = vo.getState().toString();
        } else {
            this.state = null;
        }
        if (vo.getStatus() != null) {
            this.status = vo.getStatus().toString();
        } else {
            this.status = null;
        }
        this.size = vo.getSize();
        this.actualSize = vo.getActualSize();
        this.md5Sum = vo.getMd5Sum();
        this.url = vo.getUrl();
        if (vo.getMediaType() != null) {
            this.mediaType = vo.getMediaType().toString();
        } else {
            this.mediaType = null;
        }
        this.guestOsType = vo.getGuestOsType();
        this.type = vo.getType();
        if (vo.getPlatform() != null) {
            this.platform = vo.getPlatform().toString();
        } else {
            this.platform = null;
        }
        this.architecture = vo.getArchitecture();
        this.format = vo.getFormat();
        this.system = vo.isSystem();
        this.virtio = vo.getVirtio();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.backupStorageRefs = ImageBackupStorageRefInventory.valueOf(vo.getBackupStorageRefs());
    }

    public static ImageInventory valueOf(ImageVO vo) {
        return new ImageInventory(vo);
    }

    public static ImageInventory valueOf(ImageEO vo) {
        ImageInventory inv = new ImageInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setMediaType(vo.getMediaType().toString());
        inv.setPlatform(vo.getPlatform() == null ? null : vo.getPlatform().toString());
        inv.setArchitecture(vo.getArchitecture());
        inv.setFormat(vo.getFormat());
        inv.setGuestOsType(vo.getGuestOsType());
        inv.setMd5Sum(vo.getMd5Sum());
        inv.setName(vo.getName());
        inv.setSize(vo.getSize());
        inv.setActualSize(vo.getActualSize());
        inv.setStatus(vo.getStatus().toString());
        inv.setState(vo.getState().toString());
        inv.setUrl(vo.getUrl());
        inv.setUuid(vo.getUuid());
        inv.setType(vo.getType());
        inv.setSystem(vo.isSystem());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setVirtio(vo.getVirtio());
        return inv;
    }

    public static List<ImageInventory> valueOfEO(Collection<ImageEO> vos) {
        List<ImageInventory> invs = new ArrayList<ImageInventory>(vos.size());
        for (ImageEO vo : vos) {
            invs.add(ImageInventory.valueOf(vo));
        }
        return invs;
    }

    public static List<ImageInventory> valueOf(Collection<ImageVO> vos) {
        List<ImageInventory> invs = new ArrayList<ImageInventory>(vos.size());
        for (ImageVO vo : vos) {
            invs.add(ImageInventory.valueOf(vo));
        }
        return invs;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public List<ImageBackupStorageRefInventory> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<ImageBackupStorageRefInventory> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPlatform() {
        return platform;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5Sum() {
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String type) {
        this.mediaType = type;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<SystemTagInventory> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<SystemTagInventory> systemTags) {
        this.systemTags = systemTags;
    }


    public Boolean getVirtio() {
        return virtio;
    }

    public void setVirtio(Boolean virtio) {
        this.virtio = virtio;
    }

}
