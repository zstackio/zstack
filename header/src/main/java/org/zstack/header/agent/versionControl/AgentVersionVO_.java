package org.zstack.header.agent.versionControl;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AgentVersionVO.class)
public class AgentVersionVO_ {

    public static volatile SingularAttribute<AgentVersionVO, String> uuid;
    public static volatile SingularAttribute<AgentVersionVO, String> agentType;
    public static volatile SingularAttribute<AgentVersionVO, String> currentVersion;
    public static volatile SingularAttribute<AgentVersionVO, String> exceptVersion;
    public static volatile SingularAttribute<AgentVersionVO, Timestamp> createOpDate;
    public static volatile SingularAttribute<AgentVersionVO, Timestamp> lastOpDate;

}
