package org.zstack.network.hostNetwork.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.hostNetwork.*;
import org.zstack.network.hostNetwork.lldp.api.*;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpInventory;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefVO;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class LldpManagerImpl extends AbstractService {
    private static final CLogger logger = Utils.getLogger(LldpManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public int getSyncLevel() {
        return super.getSyncLevel();
    }

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeHostNetworkInterfaceLldpModeMsg) {
            handle((APIChangeHostNetworkInterfaceLldpModeMsg) msg);
        } else if (msg instanceof APIGetHostNetworkInterfaceLldpMsg) {
            handle((APIGetHostNetworkInterfaceLldpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeHostNetworkInterfaceLldpModeMsg msg) {
        APIChangeHostNetworkInterfaceLldpModeEvent event = new APIChangeHostNetworkInterfaceLldpModeEvent(msg.getId());

        final LldpKvmAgentCommands.ChangeLldpModeCmd cmd = new LldpKvmAgentCommands.ChangeLldpModeCmd();
        List <HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class)
                .in(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuids())
                .list();
        List<String> interfaceNames = interfaceVOS.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toList());
        String hostUuid = interfaceVOS.get(0).getHostUuid();
        cmd.setPhysicalInterfaceNames(interfaceNames);
        cmd.setMode(msg.getMode());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.CHANGE_LLDP_MODE_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                } else {
                    List<HostNetworkInterfaceLldpVO> lldpVOS = new ArrayList<>();
                    for (HostNetworkInterfaceVO interfaceVO : interfaceVOS) {
                        HostNetworkInterfaceLldpVO vo = dbf.findByUuid(interfaceVO.getUuid(), HostNetworkInterfaceLldpVO.class);
                        if (vo == null) {
                            vo = new HostNetworkInterfaceLldpVO();
                        }
                        vo.setInterfaceUuid(vo.getInterfaceUuid());
                        vo.setMode(msg.getMode());
                        lldpVOS.add(vo);
                    }
                    dbf.updateCollection(lldpVOS);
                    event.setInventories(HostNetworkInterfaceLldpInventory.valueOf(lldpVOS));
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetHostNetworkInterfaceLldpMsg msg) {
        APIGetHostNetworkInterfaceLldpReply greply = new APIGetHostNetworkInterfaceLldpReply();

        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("get-interface-%s-lldp-info-from-host-%s", msg.getInterfaceUuid(), interfaceVO.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-lldp-info";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final LldpKvmAgentCommands.GetLldpInfoCmd cmd = new LldpKvmAgentCommands.GetLldpInfoCmd();
                        cmd.setPhysicalInterfaceName(interfaceVO.getInterfaceName());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setPath(LldpConstant.GET_LLDP_INFO_PATH);
                        kmsg.setHostUuid(interfaceVO.getHostUuid());
                        kmsg.setCommand(cmd);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, interfaceVO.getHostUuid());
                        bus.send(kmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    greply.setError(reply.getError());
                                    bus.reply(msg, greply);
                                    return;
                                } else {
                                    KVMHostAsyncHttpCallReply r = reply.castReply();
                                    LldpKvmAgentCommands.GetLldpInfoResponse rsp = r.toResponse( LldpKvmAgentCommands.GetLldpInfoResponse.class);
                                    if (!rsp.isSuccess()) {
                                        greply.setError(operr("operation error, because %s", rsp.getError()));
                                    } else {
                                        if (msg.getInterfaceUuid() != null) {
                                            greply.setLldp(rsp.getLldpInventory());
                                        }
                                    }
                                }
                            }
                        });
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostNetworkInterfaceLldpRefInventory inv = greply.getLldp();
                        HostNetworkInterfaceLldpRefVO vo = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceLldpRefVO.class);
                        if (vo == null) {
                            vo = new HostNetworkInterfaceLldpRefVO();
                        }
                        vo.setInterfaceUuid(inv.getInterfaceUuid());
                        vo.setChassisId(inv.getChassisId());
                        vo.setTimeToLive(inv.getTimeToLive());
                        vo.setManagementAddress(inv.getManagementAddress());
                        vo.setSystemName(inv.getSystemName());
                        vo.setSystemDescription(inv.getSystemDescription());
                        vo.setSystemCapabilities(inv.getSystemCapabilities());
                        vo.setPortId(inv.getPortId());
                        vo.setPortDescription(inv.getPortDescription());
                        vo.setVlanId(inv.getVlanId());
                        vo.setAggregationPortId(inv.getAggregationPortId());
                        vo.setMtu(inv.getMtu());

                        dbf.updateAndRefresh(vo);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, greply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        greply.setError(errCode);
                        bus.reply(msg, greply);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LldpConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
