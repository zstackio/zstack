package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 2/23/2016.
 */
@Inventory(mappingVOClass = SharedResourceVO.class)
@PythonClassInventory
public class SharedResourceInventory {
    private String ownerAccountUuid;
    private String receiverAccountUuid;
    private Boolean toPublic;
    private String resourceType;
    private String resourceUuid;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public static SharedResourceInventory valueOf(SharedResourceVO vo) {
        SharedResourceInventory inv = new SharedResourceInventory();
        inv.setReceiverAccountUuid(vo.getReceiverAccountUuid());
        inv.setOwnerAccountUuid(vo.getOwnerAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setToPublic(vo.isToPublic());
        return inv;
    }

    public static List<SharedResourceInventory> valueOf(Collection<SharedResourceVO> vos) {
        List<SharedResourceInventory> invs = new ArrayList<SharedResourceInventory>();
        for (SharedResourceVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
    }

    public String getReceiverAccountUuid() {
        return receiverAccountUuid;
    }

    public void setReceiverAccountUuid(String receiverAccountUuid) {
        this.receiverAccountUuid = receiverAccountUuid;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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
