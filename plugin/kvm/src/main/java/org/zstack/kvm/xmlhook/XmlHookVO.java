package org.zstack.kvm.xmlhook;

import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class XmlHookVO extends ResourceVO implements ToInventory {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    @Enumerated(EnumType.STRING)
    private XmlHookType type;
    @Column
    private String hookScript;
    @Column
    private String libvirtVersion;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public XmlHookType getType() {
        return type;
    }

    public void setType(XmlHookType type) {
        this.type = type;
    }

    public String getHookScript() {
        return hookScript;
    }

    public void setHookScript(String hookScript) {
        this.hookScript = hookScript;
    }

    public String getLibvirtVersion() {
        return libvirtVersion;
    }

    public void setLibvirtVersion(String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
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
