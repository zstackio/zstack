package org.zstack.kvm.xmlhook;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = XmlHookVmInstanceRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "xmlHook", inventoryClass = XmlHookInventory.class,
                foreignKey = "xmlHookUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid")
})
public class XmlHookVmInstanceRefInventory {
    private Long id;
    private String xmlHookUuid;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static XmlHookVmInstanceRefInventory valueOf(XmlHookVmInstanceRefVO vo) {
        XmlHookVmInstanceRefInventory inv = new XmlHookVmInstanceRefInventory();
        inv.setId(vo.getId());
        inv.setXmlHookUuid(vo.getXmlHookUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<XmlHookVmInstanceRefInventory> valueOf(Collection<XmlHookVmInstanceRefVO> vos) {
        List<XmlHookVmInstanceRefInventory> invs = new ArrayList<>();
        for (XmlHookVmInstanceRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getXmlHookUuid() {
        return xmlHookUuid;
    }

    public void setXmlHookUuid(String xmlHookUuid) {
        this.xmlHookUuid = xmlHookUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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
