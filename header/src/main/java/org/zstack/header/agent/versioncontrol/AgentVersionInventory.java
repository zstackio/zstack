package org.zstack.header.agent.versioncontrol;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AgentVersionVO.class)
public class AgentVersionInventory {
    private String uuid;
    private String agentType;
    private String currentVersion;
    private String expectVersion;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AgentVersionInventory valueOf(AgentVersionVO vo) {
        AgentVersionInventory inv = new AgentVersionInventory();
        inv.setUuid(vo.getUuid());
        inv.setAgentType(vo.getAgentType());
        inv.setCurrentVersion(vo.getCurrentVersion());
        inv.setExpectVersion(vo.getExpectVersion());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AgentVersionInventory> valueOf(Collection<AgentVersionVO> vos) {
        List<AgentVersionInventory> invs = new ArrayList<AgentVersionInventory>();
        for (AgentVersionVO vo : vos) {
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

    public String getExpectVersion() {
        return expectVersion;
    }

    public void setExpectVersion(String expectVersion) {
        this.expectVersion = expectVersion;
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
