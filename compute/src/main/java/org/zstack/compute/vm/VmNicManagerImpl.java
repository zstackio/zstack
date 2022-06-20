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
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.storage.snapshot.group.MemorySnapshotGroupExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                resetVmNicDriverType(tag.getResourceUuid(), defaultNicDriver);
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

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

        for (VmNicVO vmNicVO : vmNicVOS) {
            vidm.createOrUpdateVmDeviceAddress(vmNicVO.getUuid(),
                    null,
                    vmNicVO.getVmInstanceUuid(),
                    JSONObjectUtil.toJsonString(vmNicVO),
                    VmNicInventory.class.getCanonicalName());
        }
        completion.success();
    }

    @Override
    public void beforeRevertMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion) {
        List<VmInstanceDeviceAddressArchiveVO> needToRevertVmNicList = vidm.getAddressArchiveInfoFromArchiveForResourceUuid(snapshotGroup.getVmInstanceUuid(), snapshotGroup.getUuid(), VmNicInventory.class.getCanonicalName());
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
                    String __name__ = "attach-nics-saved-by-memory-snapshot-group";

                    private boolean checkDuplicateIp(String l3Uuid, String ip) {
                        return Q.New(VmNicVO.class)
                                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                                .eq(VmNicVO_.ip, ip)
                                .isExists();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToRevertVmNicList).step((originalVmNic, whileCompletion) -> {
                            MacOperator mo = new MacOperator();
                            VmNicInventory originalNicInventory = JSONObjectUtil.toObject(originalVmNic.getMetadata(), VmNicInventory.class);
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
