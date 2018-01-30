package org.zstack.storage.surfs.primary;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zstack.header.search.Inventory;
import org.zstack.storage.surfs.*;

@Inventory(mappingVOClass =SurfsPoolClassVO.class)
public class SurfsPoolClassInventory{
    private String uuid;
    private String fsid;
    private String clsname;
    private String clsdisplayname;
    private boolean isrootcls;
    private boolean isactive;
    private long totalCapacity;
    private long availableCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    public static SurfsPoolClassInventory valueOf(SurfsPoolClassVO vo){
    	SurfsPoolClassInventory inv= new SurfsPoolClassInventory();
    	inv.setUuid(vo.getUuid());
    	inv.setFsid(vo.getFsid());
    	inv.setClsname(vo.getClsname());
    	inv.setDisplayName(vo.getDisplayName());
    	inv.setIsRootCls(vo.getIsRootCls());
    	inv.setIsActive(vo.getIsActive());
    	inv.setTotalCapacity(vo.getTotalCapacity());
    	inv.setAvailableCapacity(vo.getAvailableCapacity());
    	inv.setCreateDate(vo.getCreateDate());
    	inv.setLastOpDate(vo.getLastOpDate());
    	return inv;
    }
    public static List<SurfsPoolClassInventory> valueOf(Collection<SurfsPoolClassVO> vos){
    	List<SurfsPoolClassInventory> invs= new ArrayList<SurfsPoolClassInventory>();
    	for (SurfsPoolClassVO vo : vos){
    		invs.add(valueOf(vo));
    	}
    	return invs;
    }
    public void setUuid(String cuuid){
    	this.uuid=cuuid;
    }
    
    public String getUuid(){
    	return this.uuid;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
    
    public void setClsname(String cname){
    	this.clsname=cname;
    }
    
    public String getClsname(){
    	return this.clsname;
    }
    
    public void setDisplayName(String dpname){
    	this.clsdisplayname=dpname;
    }
    
    public String getDisplayName(){
    	return this.clsdisplayname;
    }
    
    public void setIsRootCls(boolean isrc){
    	this.isrootcls=isrc;
    }
    
    public boolean getIsRootCls(){
    	return this.isrootcls;
    }
    
    public void setIsActive(boolean isac){
    	this.isactive=isac;
    }
    
    public boolean getIsActive(){
    	return this.isactive;
    }
    
    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
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