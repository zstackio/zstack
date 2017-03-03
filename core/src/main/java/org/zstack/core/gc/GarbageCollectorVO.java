package org.zstack.core.gc;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/5/2015.
 */
@Entity
@Table
public class GarbageCollectorVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String runnerClass;

    @Column
    private String context;

    @Column
    @Enumerated(EnumType.STRING)
    private GCStatus status;

    @Column
    private String managementNodeUuid;

    @Column
    private String type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRunnerClass() {
        return runnerClass;
    }

    public void setRunnerClass(String runnerClass) {
        this.runnerClass = runnerClass;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public GCStatus getStatus() {
        return status;
    }

    public void setStatus(GCStatus status) {
        this.status = status;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
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
