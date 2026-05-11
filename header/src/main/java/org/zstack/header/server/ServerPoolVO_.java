package org.zstack.header.server;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ServerPoolVO.class)
public class ServerPoolVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ServerPoolVO, String> zoneUuid;
    public static volatile SingularAttribute<ServerPoolVO, String> name;
    public static volatile SingularAttribute<ServerPoolVO, String> description;
    public static volatile SingularAttribute<ServerPoolVO, String> physicalLocation;
    public static volatile SingularAttribute<ServerPoolVO, String> networkTopology;
    public static volatile SingularAttribute<ServerPoolVO, ServerPoolState> state;
    public static volatile SingularAttribute<ServerPoolVO, Boolean> isDefault;
    public static volatile SingularAttribute<ServerPoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<ServerPoolVO, Timestamp> lastOpDate;
}
