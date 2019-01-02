package org.zstack.header.errorcode;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@Inventory(mappingVOClass = ElaborationVO.class)
@PythonClassInventory
public class ElaborationInventory {
    private long id;
    private String errorInfo;
    private String md5sum;
    private double distance;
    private boolean matched;
    private long repeats;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getRepeats() {
        return repeats;
    }

    public void setRepeats(long repeats) {
        this.repeats = repeats;
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

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public ElaborationInventory() {
    }

    public static ElaborationInventory valueOf(ElaborationVO vo) {
        ElaborationInventory inventory = new ElaborationInventory();
        inventory.setId(vo.getId());
        inventory.setDistance(vo.getDistance());
        inventory.setErrorInfo(vo.getErrorInfo());
        inventory.setRepeats(vo.getRepeats());
        inventory.setMd5sum(vo.getMd5sum());
        inventory.setMatched(vo.isMatched());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<ElaborationInventory> valueOf(Collection<ElaborationVO> vos) {
        List<ElaborationInventory> inventories = new ArrayList<>();
        for (ElaborationVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }
}
