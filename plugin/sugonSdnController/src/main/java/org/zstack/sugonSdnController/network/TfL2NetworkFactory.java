package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l2.*;

public class TfL2NetworkFactory implements L2NetworkFactory {
    private static final L2NetworkType type = new L2NetworkType(SugonSdnControllerConstant.L2_TF_NETWORK_TYPE);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        TfL2Network tfL2Network = new TfL2Network(vo);
        tfL2Network.createTfL2NetworkOnSdnController(vo, msg, new Completion(msg) {
            @Override
            public void success() {
                dbf.persist(vo);
                completion.success(L2NetworkInventory.valueOf(dbf.findByUuid(vo.getUuid(), L2NetworkVO.class)));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new TfL2Network(vo);
    }

}
