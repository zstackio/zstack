package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.NeedReplyMessage;

import java.util.Map;

public interface NetworkSecurityPolicyScheduleResourceBackend {
    enum Operation {
        SET,
        REFRESH
    }

    String getResourceType();

    boolean resourceExists(String resourceUuid);

    String getScheduleUuid(String resourceUuid);

    Map<String, String> getBoundResources();

    NeedReplyMessage makeChangeScheduleMessage(String resourceUuid,
                                               String scheduleUuid,
                                               Operation operation,
                                               boolean ignoreRefreshFailure);
}
