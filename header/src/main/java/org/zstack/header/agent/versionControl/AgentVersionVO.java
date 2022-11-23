package org.zstack.header.agent.versionControl;

import javax.persistence.*;
import java.sql.Timestamp;

@Table
@Entity
public class AgentVersionVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String agentType;

    @Column
    private String currentVersion;

    @Column
    private String exceptVersion;

    @Column
    private Timestamp createOpDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getExceptVersion() {
        return exceptVersion;
    }

    public void setExceptVersion(String exceptVersion) {
        this.exceptVersion = exceptVersion;
    }

    public Timestamp getCreateOpDate() {
        return createOpDate;
    }

    public void setCreateOpDate(Timestamp createOpDate) {
        this.createOpDate = createOpDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
