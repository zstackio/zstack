package org.zstack.compute.vm;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.storage.snapshot.group.MemorySnapshotGroupExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressVO_;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicManagerImpl implements VmNicManager, VmNicExtensionPoint, PrepareDbInitialValueExtensionPoint, VmPlatformChangedExtensionPoint, Component, MemorySnapshotGroupExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmNicManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private VmInstanceDeviceManager vidm;

    private List<String> supportNicDriverTypes;
    private String defaultPVNicDriver;
    private String defaultNicDriver;

    @Override
    public void afterAddIpAddress(String vmNicUUid, String usedIpUuid) {
        /* update UsedIpVO */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).set(UsedIpVO_.vmNicUuid, vmNicUUid).update();

        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (!temp.getUuid().equals(nic.getUsedIpUuid())) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void afterDelIpAddress(String vmNicUUid, String usedIpUuid) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();
        if (nic.getUsedIpUuid() != null && !nic.getUsedIpUuid().equals(usedIpUuid)) {
            return;
        }

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void prepareDbInitialValue() {
        List<VmNicVO> nics = Q.New(VmNicVO.class).notNull(VmNicVO_.vmInstanceUuid).list();
        List<VmNicVO> ns = nics.stream()
                .filter(v -> v.getDriverType() == null
                        && v.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)
                        && v.getVmInstanceUuid() != null)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(ns)) {
            return;
        }

        List<String> vmUuids = ns.stream()
                .map(VmNicVO::getVmInstanceUuid)
                .collect(Collectors.toList());

        List<Tuple> tupleList = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.platform)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple();

        Map<String, String> vmPlatforms = Maps.newHashMap();
        for (Tuple vmTuple : tupleList) {
            String vmUuid = vmTuple.get(0, String.class);
            String vmPlatform = vmTuple.get(1, String.class);
            vmPlatforms.put(vmUuid, ImagePlatform.valueOf(vmPlatform).isParaVirtualization() ?
                    defaultPVNicDriver : defaultNicDriver);
        }

        Map<Boolean, List<String>> nicGroups = nics.stream()
                .filter(v -> vmPlatforms.containsKey(v.getVmInstanceUuid()))
                .collect(
                        Collectors.groupingBy(
                                v -> vmPlatforms.get(v.getVmInstanceUuid()).equals(defaultPVNicDriver),
                                Collectors.mapping(VmNicVO::getUuid, Collectors.toList()))
                );

        List<String> pvNics = nicGroups.get(true);
        List<String> defaultNics = nicGroups.get(false);

        if (CollectionUtils.isNotEmpty(pvNics)) {
            SQL.New(VmNicVO.class)
                    .in(VmNicVO_.uuid, pvNics)
                    .set(VmNicVO_.driverType, defaultPVNicDriver)
                    .update();
        }

        if (CollectionUtils.isNotEmpty(defaultNics)) {
            SQL.New(VmNicVO.class)
                    .in(VmNicVO_.uuid, defaultNics)
                    .set(VmNicVO_.driverType, defaultNicDriver)
                    .update();
        }
    }

    @Override
    public boolean skipPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        return false;
    }

    @Override
    public void vmPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        if (ImagePlatform.valueOf(nowPlatform).isParaVirtualization()) {
            resetVmNicDriverType(vm.getUuid(), defaultPVNicDriver);
            return;
        }

        resetVmNicDriverType(vm.getUuid(), defaultNicDriver);
    }

    private void resetVmNicDriverType(String vmUuid, String driverType) {
        VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        List<String> needUpdateNics = vo.getVmNics().stream()
                .filter(nic -> nic.getDriverType() == null || !nic.getDriverType().equals(driverType))
                .map(VmNicVO::getUuid)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needUpdateNics)) {
            return;
        }

        SQL.New(VmNicVO.class).in(VmNicVO_.uuid, needUpdateNics).set(VmNicVO_.driverType, driverType).update();
    }

    public void setSupportNicDriverTypes(List<String> supportNicDriverTypes) {
        this.supportNicDriverTypes = supportNicDriverTypes;
    }

    public void setDefaultPVNicDriver(String defaultPVNicDriver) {
        this.defaultPVNicDriver = defaultPVNicDriver;
    }

    public void setDefaultNicDriver(String defaultNicDriver) {
        this.defaultNicDriver = defaultNicDriver;
    }

    @Override
    public List<String> getSupportNicDriverTypes() {
        return supportNicDriverTypes;
    }

    @Override
    public String getDefaultPVNicDriver() {
        return defaultPVNicDriver;
    }

    @Override
    public String getDefaultNicDriver() {
        return defaultNicDriver;
    }

    @Override
    public void setNicDriverType(VmNicInventory nic, boolean isImageSupportVirtIo, boolean isParaVirtualization) {
        if (isImageSupportVirtIo || isParaVirtualization || VmSystemTags.VIRTIO.hasTag(nic.getVmInstanceUuid())) {
            nic.setDriverType(getDefaultPVNicDriver());
        } else {
            nic.setDriverType(getDefaultNicDriver());
        }
    }

    @Override
    public boolean start() {
        VmSystemTags.VIRTIO.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                resetVmNicDriverType(tag.getResourceUuid(), defaultPVNicDriver);
                vidm.deleteDeviceAddressesByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                resetVmNicDriverType(tag.getResourceUuid(), defaultNicDriver);
                vidm.deleteDeviceAddressesByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        });

        VmSystemTags.VIRTIO.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, resourceUuid)
                        .select(VmInstanceVO_.state)
                        .findValue();

                if (state == VmInstanceState.Running || state == VmInstanceState.Unknown) {
                    throw new OperationFailureException(argerr("vm current state[%s], " +
                            "modify virtio requires the vm state[%s]", state, VmInstanceState.Stopped));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterCreateMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, snapshotGroup.getVmInstanceUuid()).eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE).list();
        String defaultL3NetworkUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, snapshotGroup.getVmInstanceUuid()).findValue();
        if (defaultL3NetworkUuid == null) {
            completion.fail(operr("the vm %s doesn't have any nic, please attach a nic and try again", snapshotGroup.getVmInstanceUuid()));
            return;
        }

        new While<>(vmNicVOS).each((vmNicVO, whileCompletion) -> {
            ArchiveVmNicType archiveVmNicType = new ArchiveVmNicType(VmNicInventory.valueOf(vmNicVO));

            if (vmNicVO.getL3NetworkUuid().equals(defaultL3NetworkUuid)) {
                archiveVmNicType.setVmDefaultL3Network(true);
            }

            GetVmNicQosMsg msg = new GetVmNicQosMsg();
            msg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
            msg.setUuid(vmNicVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                        whileCompletion.allDone();
                        return;
                    }
                    GetVmNicQosReply getVmNicQosReply = reply.castReply();
                    archiveVmNicType.setInboundBandwidth(getVmNicQosReply.getInboundBandwidth());
                    archiveVmNicType.setOutboundBandwidth(getVmNicQosReply.getOutboundBandwidth());
                    vidm.createOrUpdateVmDeviceAddress(vmNicVO.getUuid(), null,
                            vmNicVO.getVmInstanceUuid(), JSONObjectUtil.toJsonString(archiveVmNicType),
                            ArchiveVmNicType.class.getCanonicalName());
                    whileCompletion.done();
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
    public void beforeRevertMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion) {
        List<VmInstanceDeviceAddressArchiveVO> needToRevertVmNicList = vidm.getAddressArchiveInfoFromArchiveForResourceUuid(snapshotGroup.getVmInstanceUuid(), snapshotGroup.getUuid(), ArchiveVmNicType.class.getCanonicalName());
        List<String> needToDetachVmNicUuidListCurrently = Q.New(VmNicVO.class)
                .select(VmNicVO_.uuid)
                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .eq(VmNicVO_.vmInstanceUuid, snapshotGroup.getVmInstanceUuid())
                .listValues();
        List<VmInstanceDeviceAddressArchiveVO> intersection = needToRevertVmNicList.stream().filter(originalVmNic -> needToDetachVmNicUuidListCurrently.contains(originalVmNic.getResourceUuid())).collect(Collectors.toList());
        needToDetachVmNicUuidListCurrently.removeAll(intersection.stream().map(VmInstanceDeviceAddressArchiveVO::getResourceUuid).collect(Collectors.toList()));
        needToRevertVmNicList.removeAll(intersection);

        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-nic-info", snapshotGroup.getVmInstanceUuid()));
        fchain.then(new ShareFlow() {
            List<ArchiveVmNicType> needToSetNicQosArchiveVmNicTypeList = new ArrayList<>();
            MacOperator mo = new MacOperator();

            private boolean checkDuplicateIp(String l3Uuid, String ip) {
                return Q.New(VmNicVO.class)
                        .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                        .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                        .eq(VmNicVO_.ip, ip)
                        .isExists();
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "detach-current-nics";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToDetachVmNicUuidListCurrently).step((currentVmNicUuid, whileCompletion) -> {
                            DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                            msg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
                            msg.setVmNicUuid(currentVmNicUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
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
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "update-nic-info-for-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return intersection.isEmpty();
                    }

                    private boolean isDefaultL3NetworkChange(String currentDefaultL3Network, String defaultL3Network) {
                        return !defaultL3Network.equals(currentDefaultL3Network);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(intersection).step((originalVmNic, whileCompletion) -> {
                            ArchiveVmNicType updateArchiveVmNicType = JSONObjectUtil.toObject(originalVmNic.getMetadata(), ArchiveVmNicType.class);

                            String currentDefaultL3NetworkUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, snapshotGroup.getVmInstanceUuid()).findValue();

                            if (updateArchiveVmNicType.isVmDefaultL3Network() && isDefaultL3NetworkChange(currentDefaultL3NetworkUuid, updateArchiveVmNicType.getVmNicInventory().getL3NetworkUuid())) {
                                logger.info(String.format("update defaultL3NetworkUuid [%s->%s] before update nic info", currentDefaultL3NetworkUuid, updateArchiveVmNicType.getVmNicInventory().getL3NetworkUuid()));
                                SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, originalVmNic.getVmInstanceUuid())
                                        .set(VmInstanceVO_.defaultL3NetworkUuid, updateArchiveVmNicType.getVmNicInventory().getL3NetworkUuid()).update();
                            }

                            needToSetNicQosArchiveVmNicTypeList.add(updateArchiveVmNicType);

                            VmNicInventory updateNicInventory = updateArchiveVmNicType.getVmNicInventory();
                            VmNicVO currentNic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, updateNicInventory.getUuid()).find();
                            logger.info(String.format("start update vmNic[%s]: driverType[%s->%s], deviceId[%s->%s] for memory snapshot group"
                                    , updateNicInventory.getUuid()
                                    , currentNic.getDriverType(), updateNicInventory.getDriverType()
                                    , currentNic.getDeviceId(), updateNicInventory.getDeviceId()));
                            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, updateNicInventory.getUuid())
                                    .set(VmNicVO_.driverType, updateNicInventory.getDriverType())
                                    .set(VmNicVO_.deviceId, updateNicInventory.getDeviceId()).update();

                            if (!mo.checkDuplicateMac(updateNicInventory.getHypervisorType(), updateNicInventory.getMac())) {
                                logger.info(String.format("start update vmNic[%s]: mac[%s->%s]for memory snapshot group"
                                        , updateNicInventory.getUuid(), currentNic.getMac(), updateNicInventory.getMac()));
                                SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, updateNicInventory.getUuid()).set(VmNicVO_.mac, updateNicInventory.getMac()).update();
                            }

                            if (!currentNic.getL3NetworkUuid().equals(updateNicInventory.getL3NetworkUuid())) {
                                ChangeVmNicNetworkMsg cmsg = new ChangeVmNicNetworkMsg();
                                cmsg.setDestL3NetworkUuid(updateNicInventory.getL3NetworkUuid());
                                cmsg.setVmNicUuid(updateNicInventory.getUuid());
                                cmsg.setStaticIp(updateNicInventory.getIp());
                                cmsg.setVmInstanceUuid(updateNicInventory.getVmInstanceUuid());
                                cmsg.setRequiredIpMap(new HashMap<String, List<String>>() {{
                                    put(updateNicInventory.getL3NetworkUuid(), Arrays.asList(updateNicInventory.getIp()));
                                }});
                                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
                                bus.send(cmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            whileCompletion.addError(reply.getError());
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        whileCompletion.done();
                                    }
                                });
                            } else if (!checkDuplicateIp(updateNicInventory.getL3NetworkUuid(), updateNicInventory.getIp())) {
                                SetVmStaticIpMsg smsg = new SetVmStaticIpMsg();
                                smsg.setIp(updateNicInventory.getIp());
                                smsg.setL3NetworkUuid(updateNicInventory.getL3NetworkUuid());
                                smsg.setVmInstanceUuid(updateNicInventory.getVmInstanceUuid());
                                bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
                                bus.send(smsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            whileCompletion.addError(reply.getError());
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        whileCompletion.done();
                                    }
                                });
                            } else {
                                whileCompletion.done();
                            }
                        }, 10).run(new WhileDoneCompletion(trigger) {
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
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "attach-nics-saved-by-memory-snapshot-group";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToRevertVmNicList).step((originalVmNic, whileCompletion) -> {
                            ArchiveVmNicType updateArchiveVmNicType = JSONObjectUtil.toObject(originalVmNic.getMetadata(), ArchiveVmNicType.class);

                            VmNicInventory originalNicInventory = updateArchiveVmNicType.getVmNicInventory();
                            VmAttachNicMsg msg = new VmAttachNicMsg();
                            msg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
                            msg.setL3NetworkUuid(originalNicInventory.getL3NetworkUuid());
                            if (!mo.checkDuplicateMac(originalNicInventory.getHypervisorType(), originalNicInventory.getMac())) {
                                msg.setSystemTags(Arrays.asList(VmSystemTags.CUSTOM_MAC.instantiateTag(map(
                                        e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, originalNicInventory.getL3NetworkUuid()),
                                        e(VmSystemTags.MAC_TOKEN, originalNicInventory.getMac())))));
                            }
                            if (!checkDuplicateIp(originalNicInventory.getL3NetworkUuid(), originalNicInventory.getIp())) {
                                msg.setStaticIpMap(new HashMap<String, List<String>>() {{
                                    put((String) originalNicInventory.getL3NetworkUuid(), Arrays.asList(originalNicInventory.getIp()));
                                }});
                            }
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                    }
                                    vidm.createDeviceAddressFromArchive(msg.getVmInstanceUuid(), snapshotGroup.getUuid(), new HashMap<String, String>() {
                                        {
                                            put(originalNicInventory.getUuid(), ((VmAttachNicReply) reply.castReply()).getInventroy().getUuid());
                                        }
                                    });
                                    vidm.deleteVmDeviceAddress(originalNicInventory.getUuid(), originalNicInventory.getVmInstanceUuid());

                                    VmAttachNicReply vmAttachNicReply = reply.castReply();
                                    if (updateArchiveVmNicType.isVmDefaultL3Network()) {
                                        String currentDefaultL3NetworkUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, snapshotGroup.getVmInstanceUuid()).findValue();
                                        logger.info(String.format("update defaultL3NetworkUuid [%s->%s] before update nic info", currentDefaultL3NetworkUuid, vmAttachNicReply.getInventroy().getL3NetworkUuid()));
                                        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, originalVmNic.getVmInstanceUuid())
                                                .set(VmInstanceVO_.defaultL3NetworkUuid, vmAttachNicReply.getInventroy().getL3NetworkUuid()).update();
                                    }

                                    if (!originalNicInventory.getDriverType().equals(vmAttachNicReply.getInventroy().getDriverType())) {
                                        logger.info(String.format("update driverType[%s->%s]for new vmNic %s"
                                                , vmAttachNicReply.getInventroy().getDriverType(), originalNicInventory.getDriverType(), vmAttachNicReply.getInventroy().getUuid()));
                                        SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmAttachNicReply.getInventroy().getUuid()).set(VmNicVO_.driverType, originalNicInventory.getDriverType()).update();
                                    }

                                    needToSetNicQosArchiveVmNicTypeList.add(new ArchiveVmNicType(vmAttachNicReply.getInventroy(), updateArchiveVmNicType.getOutboundBandwidth(), updateArchiveVmNicType.getInboundBandwidth()));

                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
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
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-nics-qos-by-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return needToSetNicQosArchiveVmNicTypeList.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToSetNicQosArchiveVmNicTypeList).step((needToSetNicQosArchiveVmNicType, whileCompletion) -> {
                            SetVmNicQosMsg smsg = new SetVmNicQosMsg();
                            smsg.setInboundBandwidth(needToSetNicQosArchiveVmNicType.getInboundBandwidth());
                            smsg.setOutboundBandwidth(needToSetNicQosArchiveVmNicType.getOutboundBandwidth());
                            smsg.setUuid(needToSetNicQosArchiveVmNicType.getVmNicInventory().getUuid());
                            smsg.setVmInstanceUuid(needToSetNicQosArchiveVmNicType.getVmNicInventory().getVmInstanceUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }

                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
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
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
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
}
