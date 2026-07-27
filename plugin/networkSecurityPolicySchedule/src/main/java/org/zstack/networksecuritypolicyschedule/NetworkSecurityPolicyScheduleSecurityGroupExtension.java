package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.securitygroup.ChangeSecurityGroupScheduleMsg;
import org.zstack.network.securitygroup.SecurityGroupConstant;
import org.zstack.network.securitygroup.SecurityGroupRuleFilterExtensionPoint;
import org.zstack.network.securitygroup.SecurityGroupVO;
import org.zstack.network.securitygroup.SecurityGroupVO_;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkSecurityPolicyScheduleSecurityGroupExtension implements
        SecurityGroupRuleFilterExtensionPoint, NetworkSecurityPolicyScheduleResourceBackend {
    @Autowired
    private NetworkSecurityPolicyScheduleFacade scheduleFacade;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public Set<String> getInactiveSecurityGroupUuids(Collection<String> securityGroupUuids) {
        if (securityGroupUuids == null || securityGroupUuids.isEmpty()) {
            return Collections.emptySet();
        }

        List<Tuple> refs = SQL.New(
                        "select sg.uuid, sg.scheduleUuid from SecurityGroupVO sg" +
                                " where sg.uuid in (:securityGroupUuids)" +
                                " and sg.scheduleUuid is not null",
                        Tuple.class)
                .param("securityGroupUuids", securityGroupUuids)
                .list();
        if (refs.isEmpty()) {
            return Collections.emptySet();
        }

        Map<String, String> scheduleUuidByGroup = new HashMap<>();
        for (Tuple ref : refs) {
            scheduleUuidByGroup.put(
                    ref.get(0, String.class), ref.get(1, String.class));
        }
        return scheduleFacade.findInactiveResourceUuids(scheduleUuidByGroup);
    }

    @Override
    public String getResourceType() {
        return NetworkSecurityPolicyScheduleConstant.SECURITY_GROUP_RESOURCE_TYPE;
    }

    @Override
    public boolean resourceExists(String resourceUuid) {
        return dbf.isExist(resourceUuid, SecurityGroupVO.class);
    }

    @Override
    public String getScheduleUuid(String resourceUuid) {
        return Q.New(SecurityGroupVO.class)
                .select(SecurityGroupVO_.scheduleUuid)
                .eq(SecurityGroupVO_.uuid, resourceUuid)
                .findValue();
    }

    @Override
    public Map<String, String> getBoundResources() {
        List<Tuple> refs = SQL.New(
                        "select sg.uuid, sg.scheduleUuid from SecurityGroupVO sg" +
                                " where sg.scheduleUuid is not null",
                        Tuple.class)
                .list();
        Map<String, String> result = new HashMap<>();
        for (Tuple ref : refs) {
            result.put(ref.get(0, String.class), ref.get(1, String.class));
        }
        return result;
    }

    @Override
    public NeedReplyMessage makeChangeScheduleMessage(String resourceUuid,
                                                      String scheduleUuid,
                                                      Operation operation,
                                                      boolean ignoreRefreshFailure) {
        ChangeSecurityGroupScheduleMsg msg = new ChangeSecurityGroupScheduleMsg();
        msg.setSecurityGroupUuid(resourceUuid);
        msg.setScheduleUuid(scheduleUuid);
        msg.setOperation(ChangeSecurityGroupScheduleMsg.Operation.valueOf(operation.name()));
        msg.setIgnoreRefreshFailure(ignoreRefreshFailure);
        bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, resourceUuid);
        return msg;
    }
}
