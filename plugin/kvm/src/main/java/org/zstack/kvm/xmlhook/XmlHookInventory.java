package org.zstack.kvm.xmlhook;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = XmlHookVO.class)
@PythonClassInventory
public class XmlHookInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private XmlHookType type;
    private String hookScript;
    private String libvirtVersion;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static XmlHookInventory valueOf(XmlHookVO vo) {
        XmlHookInventory inv = new XmlHookInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType());
        inv.setHookScript(vo.getHookScript());
        inv.setLibvirtVersion(vo.getLibvirtVersion());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<XmlHookInventory> valueOf(Collection<XmlHookVO> vos) {
        List<XmlHookInventory> invs = new ArrayList<>();
        for (XmlHookVO vo : vos) {
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
