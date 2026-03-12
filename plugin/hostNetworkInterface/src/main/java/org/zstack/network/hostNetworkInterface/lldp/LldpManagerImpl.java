package org.zstack.network.hostNetworkInterface.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMPingAgentNoFailureExtensionPoint;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceCanonicalEvents;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.hostNetworkInterface.PhysicalSwitchPortInventory;
import org.zstack.network.hostNetworkInterface.PhysicalSwitchPortVO;
import org.zstack.network.hostNetworkInterface.PhysicalSwitchPortVO_;
import org.zstack.network.hostNetworkInterface.PhysicalSwitchVO;
import org.zstack.network.hostNetworkInterface.PhysicalSwitchVO_;
import org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent;
import org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeMsg;
import org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpMsg;
import org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpReply;
import org.zstack.network.hostNetworkInterface.lldp.api.GetHostNetworkInterfaceLldpMsg;
import org.zstack.network.hostNetworkInterface.lldp.api.GetHostNetworkInterfaceLldpReply;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefInventory;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefVO;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefVO_;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class LldpManagerImpl extends AbstractService implements HostAfterConnectedExtensionPoint, HostDeleteExtensionPoint,
        KVMPingAgentNoFailureExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LldpManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;

    private enum LLDPGetNeighbourState {
        None,
        STARTING,
        Done,
    }

    private final Map<String, LLDPGetNeighbourState> getNeighbourStateMap = new ConcurrentHashMap<>();
    private final Map<String, Object> interfaceLocks = new ConcurrentHashMap<>();

    private Object getInterfaceLock(String interfaceUuid) {
        return interfaceLocks.computeIfAbsent(interfaceUuid, k -> new Object());
    }

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
        if (msg instanceof GetHostNetworkInterfaceLldpMsg) {
            handle((GetHostNetworkInterfaceLldpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
        List<String> interfaceNames = interfaceVOS.stream().map(HostNetworkInterfaceVO::getInterfaceName).collect(Collectors.toList());
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
                    bus.publish(event);
                    return;
                } else {
                    List<HostNetworkInterfaceLldpVO> toCreate = new ArrayList<>();
                    List<HostNetworkInterfaceLldpVO> toUpdate = new ArrayList<>();

                    for (HostNetworkInterfaceVO interfaceVO : interfaceVOS) {
                        HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceVO.getUuid()).find();
                        if (vo == null) {
                            vo = new HostNetworkInterfaceLldpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setInterfaceUuid(interfaceVO.getUuid());
                            vo.setMode(msg.getMode());
                            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                            toCreate.add(vo);
                        } else {
                            vo.setMode(msg.getMode());
                            toUpdate.add(vo);
                        }
                    }

                    if (!toCreate.isEmpty()) {
                        dbf.persistCollection(toCreate);
                    }
                    if (!toUpdate.isEmpty()) {
                        dbf.updateCollection(toUpdate);
                    }
                    List<HostNetworkInterfaceLldpVO> combinedList = new ArrayList<>(toCreate);
                    combinedList.addAll(toUpdate);
                    event.setInventories(HostNetworkInterfaceLldpInventory.valueOf(combinedList));
                }
                bus.publish(event);
            }
        });
    }

    private void copyInventoryToVO(HostNetworkInterfaceLldpRefVO vo, LldpInfoStruct inv) {
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
    }

    private synchronized void syncHostNetworkInterfaceLldpInDb(String interfaceUuid, LldpInfoStruct lldpInfo) {
        if (lldpInfo == null) {
            return;
        }

        Object interfaceLock = getInterfaceLock(interfaceUuid);
        synchronized (interfaceLock) {
            HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuid).find();
            HostNetworkInterfaceLldpRefVO refVO = Q.New(HostNetworkInterfaceLldpRefVO.class).eq(HostNetworkInterfaceLldpRefVO_.lldpUuid, vo.getUuid()).find();
            if (refVO == null) {
                refVO = new HostNetworkInterfaceLldpRefVO();
                refVO.setLldpUuid(vo.getUuid());
                copyInventoryToVO(refVO, lldpInfo);
                dbf.persistAndRefresh(refVO);
            } else {
                copyInventoryToVO(refVO, lldpInfo);
                // explicitly update the data to indicate the last refresh time
                refVO.setLastOpDate(new Timestamp(System.currentTimeMillis()));
                dbf.updateAndRefresh(refVO);
            }

            /* link host network interface to physical interface based on:
            HostNetworkInterfaceLldpRefVO.systemName == PhysicalSwitchVO.name
                && HostNetworkInterfaceLldpRefVO.portId == PhysicalSwitchPortVO.name
            > select lldpUuid,chassisId,systemName,portId,aggregationPortId from HostNetworkInterfaceLldpRefVO;     +----------------------------------+-------------------+------------+------------+-------------------+
| lldpUuid                         | chassisId         | systemName | portId     | aggregationPortId |
+----------------------------------+-------------------+------------+------------+-------------------+
| 0a89ae9894274b608b3e5cd29b20e216 | c0:e3:fb:65:ab:d1 | huawei_152 | 10GE1/0/13 |              NULL |
| d3905992b4c043099c86785b0ee3186d | c0:e3:fb:65:ab:d1 | huawei_152 | 10GE1/0/11 |                 1 |
+----------------------------------+-------------------+------------+------------+-------------------+
 select uuid,name,mac from PhysicalSwitchVO;
+----------------------------------+------------+-------------------+
| uuid                             | name       | mac               |
+----------------------------------+------------+-------------------+
| b9d708c427c630b1b9fffe2d989c3a48 | huawei_152 | C0:E3:FB:65:AB:D1 |
+----------------------------------+------------+-------------------+
1 row in set (0.000 sec)
select name,ethTrunkName,switchUuid from PhysicalSwitchPortVO limit 1;
+------------+--------------+----------------------------------+
| name       | ethTrunkName | switchUuid                       |
+------------+--------------+----------------------------------+
| 10GE1/0/30 | NULL         | b9d708c427c630b1b9fffe2d989c3a48 |
+------------+--------------+----------------------------------+
            */
            PhysicalSwitchVO switchVO = Q.New(PhysicalSwitchVO.class)
                    .eq(PhysicalSwitchVO_.name, refVO.getSystemName()).limit(1).find();
            if (switchVO != null) {
                PhysicalSwitchPortVO oldPhysicalSwitchPortVO = Q.New(PhysicalSwitchPortVO.class)
                        .eq(PhysicalSwitchPortVO_.peerInterfaceUuid, interfaceUuid).find();
                PhysicalSwitchPortVO physicalSwitchPortVO = Q.New(PhysicalSwitchPortVO.class)
                        .eq(PhysicalSwitchPortVO_.switchUuid, switchVO.getUuid())
                        .eq(PhysicalSwitchPortVO_.name, refVO.getPortId()).limit(1).find();
                if (physicalSwitchPortVO != null) {
                    logger.debug(String.format("link host network interface[uuid:%s] to physical switch port[uuid:%s,name:%s]",
                            interfaceUuid, physicalSwitchPortVO.getUuid(), physicalSwitchPortVO.getName()));
                    if (physicalSwitchPortVO.getPeerInterfaceUuid() != null && physicalSwitchPortVO.getPeerInterfaceUuid().equals(interfaceUuid)) {
                        logger.debug(String.format("physical switch port[uuid:%s,name:%s] is already linked to host network interface[uuid:%s], skip",
                                physicalSwitchPortVO.getUuid(), physicalSwitchPortVO.getName(), physicalSwitchPortVO.getPeerInterfaceUuid()));
                        return;
                    }
                    physicalSwitchPortVO.setPeerInterfaceUuid(interfaceUuid);
                    dbf.update(physicalSwitchPortVO);

                    if (oldPhysicalSwitchPortVO != null) {
                        oldPhysicalSwitchPortVO.setPeerInterfaceUuid(null);
                        dbf.update(oldPhysicalSwitchPortVO);
                    }

                    HostNetworkInterfaceCanonicalEvents.PeerPortChangedData data = new HostNetworkInterfaceCanonicalEvents.PeerPortChangedData();
                    data.setInterfaceUuid(interfaceUuid);
                    data.setOldPhysicalSwitchPort(PhysicalSwitchPortInventory.valueOf(oldPhysicalSwitchPortVO));
                    data.setNewPhysiaclSwitchPort(PhysicalSwitchPortInventory.valueOf(physicalSwitchPortVO));
                    evtf.fire(HostNetworkInterfaceCanonicalEvents.PEER_PORT_CHANGED, data);
                }
            }
        }
    }

    void doGetHostNetworkInterfaceLLdpInfo(String interfaceUuid, ReturnValueCompletion<HostNetworkInterfaceLldpRefInventory> completion) {
        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(interfaceUuid, HostNetworkInterfaceVO.class);
        final LldpKvmAgentCommands.GetLldpInfoCmd cmd = new LldpKvmAgentCommands.GetLldpInfoCmd();
        cmd.setPhysicalInterfaceName(interfaceVO.getInterfaceName());

        getNeighbourStateMap.put(interfaceUuid, LLDPGetNeighbourState.STARTING);
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.GET_LLDP_INFO_PATH);
        kmsg.setHostUuid(interfaceVO.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, interfaceVO.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    getNeighbourStateMap.put(interfaceUuid, LLDPGetNeighbourState.Done);
                    return;
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    LldpKvmAgentCommands.GetLldpInfoResponse rsp = r.toResponse(LldpKvmAgentCommands.GetLldpInfoResponse.class);
                    getNeighbourStateMap.put(interfaceUuid, LLDPGetNeighbourState.Done);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr(ORG_ZSTACK_NETWORK_HOSTNETWORKINTERFACE_LLDP_10002, "operation error, because %s", rsp.getError()));
                    } else {
                        HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class)
                                .eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuid).find();
                        syncHostNetworkInterfaceLldpInDb(interfaceUuid, rsp.getLldpInfo());
                        HostNetworkInterfaceLldpRefVO lldpRefVO =  Q.New(HostNetworkInterfaceLldpRefVO.class)
                                .eq(HostNetworkInterfaceLldpRefVO_.lldpUuid, vo.getUuid())
                                .find();
                        if (lldpRefVO != null) {
                            completion.success(HostNetworkInterfaceLldpRefInventory.valueOf(lldpRefVO));
                        } else {
                            completion.fail(operr(ORG_ZSTACK_NETWORK_HOSTNETWORKINTERFACE_LLDP_10003, "get lldp ref for[%s] failed", interfaceUuid));
                        }
                    }
                }
            }
        });
    }

    private void handle(GetHostNetworkInterfaceLldpMsg msg) {
        GetHostNetworkInterfaceLldpReply reply = new GetHostNetworkInterfaceLldpReply();
        doGetHostNetworkInterfaceLLdpInfo(msg.getInterfaceUuid(), new ReturnValueCompletion<HostNetworkInterfaceLldpRefInventory>(msg) {
            @Override
            public void success(HostNetworkInterfaceLldpRefInventory returnValue) {
                reply.setLldp(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetHostNetworkInterfaceLldpMsg msg) {
        APIGetHostNetworkInterfaceLldpReply greply = new APIGetHostNetworkInterfaceLldpReply();

        doGetHostNetworkInterfaceLLdpInfo(msg.getInterfaceUuid(), new ReturnValueCompletion<HostNetworkInterfaceLldpRefInventory>(msg) {
            @Override
            public void success(HostNetworkInterfaceLldpRefInventory returnValue) {
                greply.setLldp(returnValue);
                bus.reply(msg, greply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                greply.setError(errorCode);
                bus.reply(msg, greply);
            }
        });
    }

    private void applyHostNetworkLldpConfig(List<HostNetworkInterfaceLldpVO> lldpVOS, String hostUuid, Completion completion) {
        LldpKvmAgentCommands.ApplyLldpConfigCmd cmd = new LldpKvmAgentCommands.ApplyLldpConfigCmd();
        List<LldpConfigSyncStruct.LldpModeConfig> configs = new ArrayList<>();
        for (HostNetworkInterfaceLldpVO lldpVO : lldpVOS) {
            LldpConfigSyncStruct.LldpModeConfig config = new LldpConfigSyncStruct.LldpModeConfig();
            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(lldpVO.getInterfaceUuid(), HostNetworkInterfaceVO.class);
            config.setPhysicalInterfaceName(interfaceVO.getInterfaceName());
            config.setMode(lldpVO.getMode());
            configs.add(config);
        }
        cmd.setLldpConfig(configs);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.APPLY_LLDP_CONFIG_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        List<String> interfaceUuids = Q.New(HostNetworkInterfaceLldpVO.class)
                .select(HostNetworkInterfaceLldpVO_.interfaceUuid)
                .listValues();
        List<String> interfaceUuidsOnHost = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.uuid)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .listValues();

        if (interfaceUuids != null && !interfaceUuids.isEmpty()) {
            interfaceUuids = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.uuid)
                    .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                    .notIn(HostNetworkInterfaceVO_.uuid, interfaceUuids)
                    .listValues();
        } else {
            interfaceUuids = interfaceUuidsOnHost;
        }

        List<HostNetworkInterfaceLldpVO> lldpVOS = new ArrayList<>();
        for (String interfaceUuid : interfaceUuids) {
            HostNetworkInterfaceLldpVO vo = new HostNetworkInterfaceLldpVO();
            vo.setUuid(Platform.getUuid());
            vo.setInterfaceUuid(interfaceUuid);
            vo.setMode(LldpConstant.mode.rx_only.toString());
            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            lldpVOS.add(vo);
        }
        dbf.persistCollection(lldpVOS);

        if (interfaceUuidsOnHost.isEmpty()) {
            return;
        }

        lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class)
                .in(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuidsOnHost)
                .list();
        if (lldpVOS.isEmpty()) {
            return;
        }

        applyHostNetworkLldpConfig(lldpVOS, host.getUuid(), new Completion(null) {
            @Override
            public void success() {
                logger.debug("apply the lldp configuration after host reconnected successfully");
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to apply the lldp configuration after host reconnected:%s", errorCode.toString()));
            }
        });
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        List<String> interfaceUuidsOnHost = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.uuid)
                .eq(HostNetworkInterfaceVO_.hostUuid, inventory.getUuid())
                .listValues();
        if (interfaceUuidsOnHost == null || interfaceUuidsOnHost.isEmpty()) {
            return;
        }
        List<String> lldpUuidsOnHost = Q.New(HostNetworkInterfaceLldpVO.class)
                .select(HostNetworkInterfaceLldpVO_.uuid)
                .in(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuidsOnHost)
                .listValues();
        SQL.New(HostNetworkInterfaceLldpVO.class)
                .in(HostNetworkInterfaceLldpVO_.uuid, lldpUuidsOnHost)
                .hardDelete();
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {

    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        boolean autoGetLldpNeighbor = LldpGlobalConfig.AUTO_GET_LLDP_NEIGHBOUR.value(Boolean.class);
        if (!autoGetLldpNeighbor) {
            completion.done();
            return;
        }

        List<String> interfaceUuids = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.uuid)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .listValues();
        if (interfaceUuids.isEmpty()) {
            completion.done();
            return;
        }

        List<HostNetworkInterfaceLldpVO> lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class)
                .in(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuids)
                .list();
        List<HostNetworkInterfaceLldpVO> toUpdate = new ArrayList<>();
        for (HostNetworkInterfaceLldpVO lldpVO : lldpVOS) {
            if (LldpConstant.mode.disable.toString().equals(lldpVO.getMode())) {
                continue;
            }

            if (lldpVO.getNeighborDevice() != null) {
                continue;
            }

            toUpdate.add(lldpVO);
        }

        if (toUpdate.isEmpty()) {
            completion.done();
            return;
        }

        completion.done();

        NopeCompletion nope = new NopeCompletion();
        new While<>(toUpdate).each((lldpVO, wcomp) -> {
            LLDPGetNeighbourState state = getNeighbourStateMap.get(lldpVO.getInterfaceUuid());
            if (state == LLDPGetNeighbourState.STARTING) {
                wcomp.done();
                return;
            }

            doGetHostNetworkInterfaceLLdpInfo(lldpVO.getInterfaceUuid(), new ReturnValueCompletion<HostNetworkInterfaceLldpRefInventory>(wcomp) {
                @Override
                public void success(HostNetworkInterfaceLldpRefInventory returnValue) {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug("get lldp info failed, ignore it");
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(nope) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                nope.success();
            }
        });
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
