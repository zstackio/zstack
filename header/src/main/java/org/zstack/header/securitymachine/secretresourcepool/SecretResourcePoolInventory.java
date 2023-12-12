package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by LiangHanYu on 2021/11/3 15:42
 */
@Inventory(mappingVOClass = SecretResourcePoolVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
})
public class SecretResourcePoolInventory implements Serializable {
    private String uuid;

    private String zoneUuid;

    private String name;

    private String type;

    private String description;

    private String state;

    private String model;

    private Integer heartbeatInterval;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    protected SecretResourcePoolInventory(SecretResourcePoolVO vo) {
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setUuid(vo.getUuid());
        this.setState(vo.getState().toString());
        this.setModel(vo.getModel());
        this.setZoneUuid(vo.getZoneUuid());
        this.setType(vo.getType().toString());
        this.setHeartbeatInterval(vo.getHeartbeatInterval());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public SecretResourcePoolInventory() {
    }

    public static SecretResourcePoolInventory valueOf(SecretResourcePoolVO vo) {
        SecretResourcePoolInventory inv = new SecretResourcePoolInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setUuid(vo.getUuid());
        inv.setState(vo.getState().toString());
        inv.setModel(vo.getModel());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setType(vo.getType().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SecretResourcePoolInventory> valueOf(Collection<SecretResourcePoolVO> vos) {
        List<SecretResourcePoolInventory> invs = new ArrayList<SecretResourcePoolInventory>(vos.size());
        for (SecretResourcePoolVO vo : vos) {
            invs.add(SecretResourcePoolInventory.valueOf(vo));
        }
        return invs;
    }

    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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
