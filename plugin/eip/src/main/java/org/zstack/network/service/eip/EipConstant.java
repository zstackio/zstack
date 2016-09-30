package org.zstack.network.service.eip;

import org.zstack.header.network.service.NetworkServiceType;

/**
 */
public interface EipConstant {
    public static final String SERVICE_ID = "eip";
    public static String EIP_NETWORK_SERVICE_TYPE = "Eip";

    public static final String ACTION_CATEGORY = "eip";

    String QUOTA_EIP_NUM = "eip.num";

    public static final NetworkServiceType EIP_TYPE = new NetworkServiceType(EIP_NETWORK_SERVICE_TYPE);

    public static enum Params {
        NETWORK_SERVICE_PROVIDER_TYPE,
        EIP_STRUCT,
        NEED_LOCK_VIP,
        NEED_UNLOCK_VIP
    }
}
