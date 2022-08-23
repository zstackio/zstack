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
    public SdnController getSdnController(SdnControllerVO vo) {
        return new H3cVcfcSdnController(vo);
    }

    @Override
    public void createSdnController(final SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion) {
        SdnControllerVO sdnControllerVO = dbf.persistAndRefresh(vo);
        SdnControllerInventory sdn = SdnControllerInventory.valueOf(sdnControllerVO);
        H3cVcfcSdnController controller = new H3cVcfcSdnController(sdnControllerVO);

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("create-sdn-controller-%s", msg.getName()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("pre-process-for-create-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.preInitSdnController(msg, sdn, new Completion(trigger) {
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
                        controller.initSdnController(msg, sdn, new Completion(completion) {
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
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("post-process-for-create-sdn-controller--%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.postInitSdnController(msg, sdn, new Completion(trigger) {
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
    public void deleteSdnController(SdnControllerVO vo, SdnControllerDeletionMsg msg, Completion completion) {
        SdnControllerVO sdnControllerVO = dbf.persistAndRefresh(vo);
        SdnControllerInventory sdn = SdnControllerInventory.valueOf(sdnControllerVO);
        H3cVcfcSdnController controller = new H3cVcfcSdnController(sdnControllerVO);

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName(String.format("delete-sdn-controller-%s", sdnControllerVO.getName()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("delete-sdn-controller-%s", sdnControllerVO.getName());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                controller.deleteSdnController(msg, sdn, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully delete sdn controller"));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
