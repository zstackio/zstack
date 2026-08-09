package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

public class L2NoVlanL2NetworkFactory implements L2NetworkFactory, Component, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint {
    private static L2NetworkType type = new L2NetworkType(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    private static CLogger logger = Utils.getLogger(L2NoVlanL2NetworkFactory.class);
    private static FieldPrinter printer = Utils.getFieldPrinter();
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {

        FlowChain chain = new SimpleFlowChain();
        chain.setName("create-no-vlan-network");
        chain.then(new Flow() {
            String __name__ = "write-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                dbf.persist(vo);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                dbf.removeByPrimaryKey(vo.getUuid(), L2NetworkVO.class);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-l2-network-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(L2NetworkCreateExtensionPoint.class))
                        .each((exp, wcompl) -> exp.postCreateL2Network(L2NetworkInventory.valueOf(vo), msg, new Completion(trigger) {
                            @Override
                            public void success() {
                                wcompl.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                wcompl.addError(errorCode);
                                wcompl.allDone();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                L2NetworkInventory inv = L2NetworkInventory.valueOf(
                        dbf.findByUuid(vo.getUuid(), L2NetworkVO.class));
                logger.debug("Successfully created NoVlanL2Network: " + printer.print(inv));
                completion.success(inv);
            }
        }).start();
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2NoVlanNetwork(vo);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        return 0;
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }
}
