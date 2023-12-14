package org.zstack.header.securitymachine;

import java.util.Collection;

/**
 * Created by LiangHanYu on 2021/11/15 18:13
 */
public interface SecurityMachineTracker {
    void trackSecurityMachine(String SecurityMachineUuid);

    void untrackSecurityMachine(String SecurityMachineUuid);

    void trackSecurityMachine(Collection<String> SecurityMachineUuids);
}
