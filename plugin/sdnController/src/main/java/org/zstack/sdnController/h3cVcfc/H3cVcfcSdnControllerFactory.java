package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.*;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.utils.ObjectUtils.logger;

public class H3cVcfcSdnControllerFactory implements SdnControllerFactory {
    SdnControllerType sdnControllerType = new SdnControllerType(SdnControllerConstant.H3C_VCFC_CONTROLLER);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public void createSdnController(final SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion) {
        SdnControllerVO sdnControllerVO = dbf.persistAndRefresh(vo);
        H3cVcfcSdnController controller = new H3cVcfcSdnController(sdnControllerVO);

        Map data = new HashMap();
        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setData(data);
        fchain.setName(String.format("create-sdn-controller-%s", msg.getName()));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("pre-check-for-create-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.preInitSdnController(vo.toInventory(), msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("init-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.postInitSdnController(msg, new Completion(completion) {
                            @Override
                            public void success() {
                                completion.success();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                dbf.removeByPrimaryKey(vo.getUuid(), SdnControllerVO.class);
                                completion.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });
                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully create sdn controller"));
                        completion.success();
                    }
                });
                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new H3cVcfcSdnController(vo);
    }

    @Override
    public int getMappingVlanIdFromHardwareVxlanNetwork(L2VxlanNetworkInventory vxlan, String controllerUuid) {
        return 0;
    }
}
