package org.zstack.sugonSdnController.network;

import org.zstack.network.l3.L3BasicNetworkFactory;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.header.network.l3.*;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-11
 **/
public class TfL3NetworkFactory extends L3BasicNetworkFactory {
    private static final L3NetworkType type = new L3NetworkType(SugonSdnControllerConstant.L3_TF_NETWORK_TYPE);

    @Override
    public L3Network getL3Network(L3NetworkVO vo) {
        return new TfL3Network(vo);
    }

    @Override
    public L3NetworkType getType() {
        return type;
    }

    @Override
    public boolean applyNetworkServiceWhenVmStateChange() {
        return false;
    }
}
