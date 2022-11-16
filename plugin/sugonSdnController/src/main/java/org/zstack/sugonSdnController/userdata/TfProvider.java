package org.zstack.sugonSdnController.userdata;

import org.zstack.header.message.Message;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.APIAttachNetworkServiceProviderToL2NetworkMsg;
import org.zstack.header.network.service.APIDetachNetworkServiceProviderFromL2NetworkMsg;
import org.zstack.header.network.service.NetworkServiceProvider;
import org.zstack.header.network.service.NetworkServiceProviderVO;

/**
 * Created by fuwei on 11/15/2022.
 */
public class TfProvider implements NetworkServiceProvider {
    private NetworkServiceProviderVO self;

    public TfProvider(NetworkServiceProviderVO self) {
        this.self = self;
    }

    public TfProvider() {
    }

    @Override
    public void handleMessage(Message msg) {
    }

    @Override
    public void attachToL2Network(L2NetworkInventory l2Network, APIAttachNetworkServiceProviderToL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void detachFromL2Network(L2NetworkInventory l2Network, APIDetachNetworkServiceProviderFromL2NetworkMsg msg) throws NetworkException {

    }
}
