package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

public class L2VlanNetworkFactory extends AbstractService implements L2NetworkFactory, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint {
    private static CLogger logger = Utils.getLogger(L2VlanNetworkFactory.class);
    static L2NetworkType type = new L2NetworkType(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.setName("create-no-vlan-network");
        chain.then(new Flow() {
            String __name__ = "write-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                APICreateL2VlanNetworkMsg amsg = (APICreateL2VlanNetworkMsg) msg;
                L2VlanNetworkVO vo = new L2VlanNetworkVO(ovo);
                vo.setVlan(amsg.getVlan());
                vo.setVirtualNetworkId(vo.getVlan());
                dbf.persist(vo);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                dbf.removeByPrimaryKey(ovo.getUuid(), L2VlanNetworkVO.class);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-l2-network-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2VlanNetworkInventory inv = L2VlanNetworkInventory.valueOf(
                        dbf.findByUuid(ovo.getUuid(), L2VlanNetworkVO.class));
                new While<>(pluginRgty.getExtensionList(L2NetworkCreateExtensionPoint.class))
                        .each((exp, wcompl) -> exp.postCreateL2Network(inv, msg, new Completion(trigger) {
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
                L2VlanNetworkInventory inv = L2VlanNetworkInventory.valueOf(
                        dbf.findByUuid(ovo.getUuid(), L2VlanNetworkVO.class));
                logger.debug(String.format("Successfully created VlanL2Network[uuid:%s, name:%s]",
                        inv.getUuid(), inv.getName()));
                completion.success(inv);
            }
        }).start();

    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2VlanNetwork(vo);
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
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        bus.dealWithUnknownMessage(msg);
    }


    @Override
    public String getId() {
        return bus.makeLocalServiceId(L2NetworkConstant.L2_VLAN_NETWORK_FACTORY_SERVICE_ID);
    }

    @Override
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_VLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        L2VlanNetworkVO l2VlanNetworkVO = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, l2NetworkUuid).find();
        return l2VlanNetworkVO.getVlan();
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }
}
