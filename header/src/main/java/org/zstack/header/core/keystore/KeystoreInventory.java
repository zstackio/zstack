package org.zstack.header.core.keystore;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by miao on 16-8-15.
 */
@Inventory(mappingVOClass = KeystoreVO.class, collectionValueOfMethod = "valueOfList")
public class KeystoreInventory implements Serializable {
    private String uuid;
    private String resourceUuid;
    private String resourceType;
    private String type;
    private String content;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static KeystoreInventory valueOf(KeystoreVO vo) {
        KeystoreInventory inv = new KeystoreInventory();
        inv.setUuid(vo.getUuid());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setType(vo.getType());
        inv.setContent(vo.getContent());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<KeystoreInventory> valueOfList(Collection<KeystoreVO> vos) {
        List<KeystoreInventory> invs = new ArrayList<KeystoreInventory>(vos.size());
        for (KeystoreVO vo : vos) {
            invs.add(KeystoreInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
