package org.zstack.header.securitymachine;

import org.zstack.header.errorcode.ErrorCode;

public interface SecurityMachineClientFactory {
    /**
     * create a security machine client, not null
     */
    SecurityMachineClient create();

    /**
     * Get the type of the current security machine client and factory
     * 
     * @return the current type of the current cipher machine client
     */
    String getType();

    ErrorCode testConnection(AddSecurityMachineMessage message);
}
