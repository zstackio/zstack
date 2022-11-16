package org.zstack.sugonSdnController.userdata;

import org.zstack.header.network.service.NetworkServiceProviderType;

/**
 * Created by fuwei on 11/15/2022.
 */
public class TfNetworkServiceConstant {
    public static final String TF_NETWORK_SERVICE_TYPE_STRING = "TF";
    public static final NetworkServiceProviderType TF_NETWORK_SERVICE_TYPE = new NetworkServiceProviderType(TfNetworkServiceConstant.TF_NETWORK_SERVICE_TYPE_STRING);
}
