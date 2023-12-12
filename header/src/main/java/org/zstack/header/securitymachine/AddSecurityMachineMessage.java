package org.zstack.header.securitymachine;

/**
 * Created by LiangHanYu on 2021/11/14 16:20
 */
public interface AddSecurityMachineMessage {
    String getName();

    String getDescription();

    String getManagementIp();

    /**
     * @desc the model of security machine.eg:InfoSec
     */
    String getModel();

    String getType();

    String getSecretResourcePoolUuid();

    String getResourceUuid();

    String getZoneUuid();
}