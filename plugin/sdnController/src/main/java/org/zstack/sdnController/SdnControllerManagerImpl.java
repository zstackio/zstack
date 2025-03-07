package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.sdnController.header.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;

public class SdnControllerManagerImpl extends AbstractService implements SdnControllerManager,
        L2NetworkCreateExtensionPoint, L2NetworkDeleteExtensionPoint, InstantiateResourceOnAttachingNicExtensionPoint,
        PreVmInstantiateResourceExtensionPoint, VmReleaseResourceExtensionPoint,
        ReleaseNetworkServiceOnDetachingNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SdnControllerManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    private Map<String, SdnControllerFactory> sdnControllerFactories = Collections.synchronizedMap(new HashMap<String, SdnControllerFactory>());

    @Override
    public int getSyncLevel() {
        return super.getSyncLevel();
    }

    @Override
    public List<String> getAliasIds() {
        return super.getAliasIds();
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIAddSdnControllerMsg) {
            handle((APIAddSdnControllerMsg) msg);
        } else if (msg instanceof SdnControllerMessage) {
            handleSdnControllerMessage((SdnControllerMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleSdnControllerMessage(SdnControllerMessage msg) {
        SdnControllerVO vo = dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class);
        if (vo == null) {
            String err = String.format("Cannot find Sdn controller[uuid:%s], it may have been deleted", msg.getSdnControllerUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        SdnControllerBase sdnController = new SdnControllerBase(vo);
        sdnController.handleMessage(msg);
    }

    private void doCreateSdnController(SdnControllerVO vo, APIAddSdnControllerMsg msg, Completion completion) {
        SdnControllerFactory factory = getSdnControllerFactory(msg.getVendorType());
        SdnController controller = factory.getSdnController(vo);

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
                        controller.preInitSdnController(msg, new Completion(trigger) {
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
                    String __name__ = String.format("create-sdn-controller-%s-on-db", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        dbf.persist(vo);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeByPrimaryKey(vo.getUuid(), SdnControllerVO.class);
                        trigger.rollback();
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("init-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.initSdnController(msg, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                dbf.removeByPrimaryKey(vo.getUuid(), SdnControllerVO.class);
                                trigger.fail(errorCode);
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
                        controller.postInitSdnController(vo, new Completion(trigger) {
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

    private void handle(APIAddSdnControllerMsg msg) {
        APIAddSdnControllerEvent event = new APIAddSdnControllerEvent(msg.getId());

        SdnControllerVO vo = new SdnControllerVO();
        vo.setVendorType(msg.getVendorType());
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setIp(msg.getIp());
        vo.setUsername(msg.getUserName());
        vo.setPassword(msg.getPassword());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setStatus(SdnControllerStatus.Connected);

        doCreateSdnController(vo, msg, new Completion(msg) {
            @Override
            public void success() {
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), SdnControllerVO.class.getSimpleName());
                event.setInventory(SdnControllerInventory.valueOf(dbf.findByUuid(vo.getUuid(), SdnControllerVO.class)));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void postCreateL2Network(L2NetworkInventory l2Network, APICreateL2NetworkMsg msg, Completion completion) {
        VSwitchType vSwitchType = VSwitchType.valueOf(l2Network.getvSwitchType());
        if (vSwitchType.getSdnControllerType() == null) {
            completion.success();
            return;
        }

        String sdnControllerUuid = null;
        for (String systag : msg.getSystemTags()) {
            if (SdnControllerSystemTags.L2_NETWORK_OVN_UUID.isMatch(systag)) {
                sdnControllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByTag(
                        systag, SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
            }
        }

        if (sdnControllerUuid == null) {
            completion.fail(operr("can not create sdn l2 network because there is not sdn controller uuid in api message"));
            return;
        }

        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (vo == null) {
            completion.fail(operr("can not found sdn controller for l2 network[uuid:%s, vswitchType:%s]",
                    l2Network.getUuid(), l2Network.getvSwitchType()));
            return;
        }

        SdnControllerFactory factory = getSdnControllerFactory(vo.getVendorType());
        SdnControllerL2 controller = factory.getSdnControllerL2(vo);
        controller.createL2Network(l2Network, msg.getSystemTags(), completion);
    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {

    }

    @Override
    public void preDeleteL2Network(L2NetworkInventory inventory) throws L2NetworkException {

    }

    @Override
    public void beforeDeleteL2Network(L2NetworkInventory inventory) {

    }

    @Override
    public void deleteL2Network(L2NetworkInventory inv, NoErrorCompletion completion) {
        VSwitchType vSwitchType = VSwitchType.valueOf(inv.getvSwitchType());
        if (vSwitchType.getSdnControllerType() == null) {
            completion.done();
            return;
        }

        /* vswitch type: OvnDpdk will go here */
        SdnControllerFactory factory = getSdnControllerFactory(vSwitchType.getSdnControllerType());
        SdnControllerL2 controllerL2 = factory.getSdnControllerL2(inv.getUuid());
        if (controllerL2 == null) {
            logger.warn(String.format("can not found sdn controller for l2 network[uuid:%s, vswitchType:%s]",
                    inv.getUuid(), inv.getvSwitchType()));
            completion.done();
            return;
        }

        controllerL2.deleteL2Network(inv, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("can not found sdn controller for l2 network[uuid:%s, vswitchType:%s]",
                        inv.getUuid(), inv.getvSwitchType()));
                completion.done();
            }
        });
    }

    @Override
    public void afterDeleteL2Network(L2NetworkInventory inventory) {

    }

    private void sdnAddVmNic(String sdnControllerUuid, List<VmNicInventory> nics, Completion completion) {
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        SdnControllerFactory factory = getSdnControllerFactory(vo.getVendorType());
        if (factory == null) {
            completion.fail(operr("there is no sdn controller factory for sdn controller type:%s", vo.getVendorType()));
            return;
        }

        SdnControllerL2 controller = factory.getSdnControllerL2(vo);
        controller.addVmNics(nics, completion);
    }

    private void sdnAddVmNics(Map<String, List<VmNicInventory>> nicMaps, Completion completion) {
        new While<>(nicMaps.entrySet()).each((e, wcomp) -> {
            sdnAddVmNic(e.getKey(), e.getValue(), new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private void removeOvnLogicalPorts(String controllerUuid, List<VmNicInventory> nics, Completion completion) {
        SdnControllerVO vo = dbf.findByUuid(controllerUuid, SdnControllerVO.class);
        SdnControllerFactory factory = getSdnControllerFactory(vo.getVendorType());
        if (factory == null) {
            completion.fail(operr("there is no sdn controller factory for sdn controller type:%s", vo.getVendorType()));
            return;
        }

        SdnControllerL2 controller = factory.getSdnControllerL2(vo);
        controller.removeVmNics(nics, completion);
    }

    private void removeLogicalPort(Map<String, List<VmNicInventory>> nicMaps, Completion completion) {
        new While<>(nicMaps.entrySet()).each((e, wcomp) -> {
            removeOvnLogicalPorts(e.getKey(), e.getValue(), new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (VmInstanceConstant.VmOperation.DetachNic != spec.getCurrentVmOperation() &&
                VmInstanceConstant.VmOperation.Destroy != spec.getCurrentVmOperation()) {
            completion.success();
            return;
        }

        if (spec.getL3Networks() == null || spec.getL3Networks().isEmpty()) {
            completion.success();
            return;
        }

        // we run into this situation when VM nics are all detached and the
        // VM is being rebooted
        if (spec.getDestNics().isEmpty()) {
            completion.success();
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        for (VmNicInventory nic : spec.getDestNics()) {
            L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
            if (l3Vo == null) {
                continue;
            }

            L2NetworkVO l2VO = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);
            if (l2VO == null) {
                continue;
            }

            VSwitchType vSwitchType = VSwitchType.valueOf(l2VO.getvSwitchType());
            if (vSwitchType.getSdnControllerType() == null) {
                continue;
            }

            String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                    l2VO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
            if (controllerUuid == null) {
                completion.fail(operr("sdn l2 network[uuid:%s] is not attached controller", l2VO.getUuid()));
                return;
            }

            nicMaps.computeIfAbsent(controllerUuid, k -> new ArrayList<>()).add(nic);
        }

        if (nicMaps.isEmpty()) {
            completion.success();
            return;
        }

        removeLogicalPort(nicMaps, completion);
    }

    @Override
    public void instantiateResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, Completion completion) {
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3.getL2NetworkUuid(), L2NetworkVO.class);
        VSwitchType vSwitchType = VSwitchType.valueOf(l2NetworkVO.getvSwitchType());
        if (vSwitchType.getSdnControllerType() == null) {
            completion.success();
            return;
        }

        String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                l2NetworkVO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
        if (controllerUuid == null) {
            completion.fail(operr("sdn l2 network[uuid:%s] is not attached controller", l2NetworkVO.getUuid()));
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        List<VmNicInventory> nics = new ArrayList<>();
        nics.add(spec.getDestNics().get(0));
        nicMaps.put(controllerUuid, nics);
        sdnAddVmNics(nicMaps, completion);
    }

    @Override
    public void releaseResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, NoErrorCompletion completion) {
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3.getL2NetworkUuid(), L2NetworkVO.class);
        VSwitchType vSwitchType = VSwitchType.valueOf(l2NetworkVO.getvSwitchType());
        if (vSwitchType.getSdnControllerType() == null) {
            completion.done();
            return;
        }

        String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                l2NetworkVO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
        if (controllerUuid == null) {
            logger.warn(String.format("sdn l2 network[uuid:%s] is not attached controller", l2NetworkVO.getUuid()));
            completion.done();
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        List<VmNicInventory> nics = new ArrayList<>();
        nics.add(spec.getDestNics().get(0));
        nicMaps.put(controllerUuid, nics);

        removeLogicalPort(nicMaps, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.info(String.format("failed to remove logical port for vm[uuid:%s] nic[internalName:%s], because: %s",
                        spec.getVmInventory().getUuid(), spec.getDestNics().get(0).getInternalName(), errorCode.getDetails()));
                completion.done();
            }
        });
    }

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion) {
        L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);
        VSwitchType vSwitchType = VSwitchType.valueOf(l2NetworkVO.getvSwitchType());
        if (vSwitchType.getSdnControllerType() == null) {
            completion.done();
            return;
        }

        String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                l2NetworkVO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
        if (controllerUuid == null) {
            logger.warn(String.format("sdn l2 network[uuid:%s] is not attached controller", l2NetworkVO.getUuid()));
            completion.done();
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        List<VmNicInventory> nics = new ArrayList<>();
        nics.add(spec.getDestNics().get(0));
        nicMaps.put(controllerUuid, nics);

        removeLogicalPort(nicMaps, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.info(String.format("failed to remove logical port for vm[uuid:%s] nic[internalName:%s], because: %s",
                        spec.getVmInventory().getUuid(), spec.getDestNics().get(0).getInternalName(), errorCode.getDetails()));
                completion.done();
            }
        });
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getL3Networks() == null || spec.getL3Networks().isEmpty()) {
            completion.success();
            return;
        }

        // we run into this situation when VM nics are all detached and the
        // VM is being rebooted
        if (spec.getDestNics().isEmpty()) {
            completion.success();
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        for (VmNicInventory nic : spec.getDestNics()) {
            L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
            if (l3Vo == null) {
                continue;
            }

            L2NetworkVO l2VO = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);
            if (l2VO == null) {
                continue;
            }

            VSwitchType vSwitchType = VSwitchType.valueOf(l2VO.getvSwitchType());
            if (vSwitchType.getSdnControllerType() ==null) {
                continue;
            }

            String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                    l2VO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
            if (controllerUuid == null) {
                completion.fail(operr("sdn l2 network[uuid:%s] is not attached controller", l2VO.getUuid()));
                return;
            }

            nicMaps.computeIfAbsent(controllerUuid, k -> new ArrayList<>()).add(nic);
        }

        if (nicMaps.isEmpty()) {
            completion.success();
            return;
        }

        sdnAddVmNics(nicMaps, completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getL3Networks() == null || spec.getL3Networks().isEmpty()) {
            completion.success();
            return;
        }

        // we run into this situation when VM nics are all detached and the
        // VM is being rebooted
        if (spec.getDestNics().isEmpty()) {
            completion.success();
            return;
        }

        Map<String, List<VmNicInventory>> nicMaps = new HashMap<>();
        for (VmNicInventory nic : spec.getDestNics()) {
            L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
            if (l3Vo == null) {
                continue;
            }

            L2NetworkVO l2VO = dbf.findByUuid(l3Vo.getL2NetworkUuid(), L2NetworkVO.class);
            if (l2VO == null) {
                continue;
            }

            VSwitchType vSwitchType = VSwitchType.valueOf(l2VO.getvSwitchType());
            if (vSwitchType.getSdnControllerType() ==null) {
                continue;
            }

            String controllerUuid = SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                    l2VO.getUuid(), SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
            if (controllerUuid == null) {
                completion.fail(operr("sdn l2 network[uuid:%s] is not attached controller", l2VO.getUuid()));
                return;
            }

            nicMaps.computeIfAbsent(controllerUuid, k -> new ArrayList<>()).add(nic);
        }

        if (nicMaps.isEmpty()) {
            completion.success();
            return;
        }

        removeLogicalPort(nicMaps, completion);
    }

    @Override
    public SdnControllerFactory getSdnControllerFactory(String type) {
        SdnControllerFactory factory = sdnControllerFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find sdn controller for type(%s)", type));
        }

        return factory;
    }

    @Override
    public SdnController getSdnController(SdnControllerVO sdnControllerVO) {
        SdnControllerFactory factory = getSdnControllerFactory(sdnControllerVO.getVendorType());
        return factory.getSdnController(sdnControllerVO);
    }

    @Override
    public SdnControllerL2 getSdnControllerL2(SdnControllerVO sdnControllerVO) {
        SdnControllerFactory factory = getSdnControllerFactory(sdnControllerVO.getVendorType());
        return factory.getSdnControllerL2(sdnControllerVO);
    }

    @Override
    public FlowChain getSyncChain(SdnControllerVO sdnControllerVO) {
        SdnControllerFactory f = getSdnControllerFactory(sdnControllerVO.getVendorType());
        return f.getSyncChain();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SdnControllerConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        for (SdnControllerFactory f : pluginRgty.getExtensionList(SdnControllerFactory.class)) {
            SdnControllerFactory old = sdnControllerFactories.get(f.getVendorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SdnControllerFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getVendorType()));
            }
            sdnControllerFactories.put(f.getVendorType().toString(), f);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
