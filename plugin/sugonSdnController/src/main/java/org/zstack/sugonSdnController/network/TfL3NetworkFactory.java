package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.*;

/**
 * @description:
 * @author: liupt@sugon.com
 * @create: 2022-10-11
 **/
public class TfL3NetworkFactory implements L3NetworkFactory {
    private static final L3NetworkType type = new L3NetworkType(SugonSdnControllerConstant.L3_TF_NETWORK_TYPE);
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public L3Network getL3Network(L3NetworkVO vo) {
        return new TfL3Network(vo);
    }

    @Override
    public L3NetworkType getType() {
        return type;
    }

    @Override
    public L3NetworkInventory createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg) {
        l3vo.setType(type.toString());
        dbf.getEntityManager().persist(l3vo);
        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(l3vo);
        return L3NetworkInventory.valueOf(l3vo);
    }

    @Override
    public boolean applyNetworkServiceWhenVmStateChange() {
        return false;
    }
}
