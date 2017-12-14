package org.zstack.storage.surfs;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * Created by zhouhaiping 2017-11-14
 */
@Entity
@Table
public class SurfsPoolClassVO{
    @Id
    @Column
    private String uuid;

    @Column
    private String fsid;
    
    @Column
    private String clsname;

    @Column
    private String clsdisplayname;
    
    @Column
    private boolean isrootcls=false;
    
    @Column
    private boolean isactive=false;
    
    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;
    
    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;
    
    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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