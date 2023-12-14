package org.zstack.header.securitymachine.secretresourcepool;

/**
 * Created by LiangHanYu on 2021/11/8 10:18
 */
public interface CreateSecretResourcePoolMessage {
    String getName();

    String getDescription();

    String getZoneUuid();

    String getResourceUuid();

    /**
     * @desc define the model of resource pool, what type of resource pool can only add what model of security machine.
     * generate different factories according to different models to process different APIs.
     */
    String getModel();

    /**
     * @desc used to set the time to periodically check the connection status of the security machine
     */
    Integer getHeartbeatInterval();

    String getType();
}
