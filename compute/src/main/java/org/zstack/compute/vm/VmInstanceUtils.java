package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.Platform;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.APIChangeInstanceOfferingMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.UpdateVmInstanceMsg;
import org.zstack.header.vm.UpdateVmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.zstack.compute.vm.VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME;
import static org.zstack.compute.vm.VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Created by Wenhao.Zhang on 22/03/10
 */
public class VmInstanceUtils {
    public static CreateVmInstanceMsg fromAPICreateVmInstanceMsg(APICreateVmInstanceMsg msg) {
        CreateVmInstanceMsg cmsg = NewVmInstanceMsgBuilder.fromAPINewVmInstanceMsg(msg);
        cmsg.setImageUuid(msg.getImageUuid());
        if (msg.getAllocatorStrategy() != null) {
            cmsg.setAllocatorStrategy(msg.getAllocatorStrategy());
        }
        cmsg.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        cmsg.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        cmsg.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        cmsg.setSshKeyPairUuids(msg.getSshKeyPairUuids());
        cmsg.setPlatform(msg.getPlatform());
        cmsg.setGuestOsType(msg.getGuestOsType());
        cmsg.setArchitecture(msg.getArchitecture());
        cmsg.setStrategy(msg.getStrategy());
        cmsg.setDevicesSpec(msg.getDevicesSpec());
        if (CollectionUtils.isNotEmpty(msg.getDataDiskOfferingUuids()) || CollectionUtils.isNotEmpty(msg.getDataDiskSizes())) {
            cmsg.setPrimaryStorageUuidForDataVolume(getPSUuidForDataVolume(msg.getSystemTags()));
        }
        if (msg.getVirtio() == null) {
            cmsg.setVirtio(false);
        } else {
            cmsg.setVirtio(msg.getVirtio());
        }

        if (!isEmpty(msg.getDiskAOs())) {
            DiskAO rootDisk = findOneOrNull(msg.getDiskAOs(), DiskAO::isBoot);
            cmsg.setRootDisk(rootDisk);
            cmsg.setDataDisks(new ArrayList<>(msg.getDiskAOs()));
            cmsg.getDataDisks().remove(rootDisk);
        }

        if (cmsg.getRootDisk() == null) {
            DiskAO bootDisk = DiskAO.rootDisk();

            if (msg.getRootDiskOfferingUuid() != null) {
                bootDisk.setDiskOfferingUuid(msg.getRootDiskOfferingUuid());
            } else if (msg.getRootDiskSize() != null) {
                bootDisk.setSize(msg.getRootDiskSize());
            }
            bootDisk.setPlatform(msg.getPlatform());
            bootDisk.setGuestOsType(msg.getGuestOsType());
            bootDisk.setArchitecture(msg.getArchitecture());
            cmsg.setRootDisk(bootDisk);
        } else {
            DiskAO bootDisk = cmsg.getRootDisk();
            if (msg.getRootDiskOfferingUuid() != null) {
                bootDisk.setDiskOfferingUuid(msg.getRootDiskOfferingUuid());
            } else if (msg.getRootDiskSize() != null) {
                bootDisk.setSize(msg.getRootDiskSize());
            }
            bootDisk.setPlatform(msg.getPlatform());
            bootDisk.setGuestOsType(msg.getGuestOsType());
            bootDisk.setArchitecture(msg.getArchitecture());
        }

        // dataDiskOfferingUuids + dataDiskSizes -> deprecatedDataVolumeSpecs
        int expectDataVolumeCount =
                (msg.getDataDiskOfferingUuids() == null ? 0 : msg.getDataDiskOfferingUuids().size()) +
                (msg.getDataDiskSizes() == null ? 0 : msg.getDataDiskSizes().size());
        List<DiskAO> deprecatedDataVolumeSpecs = new ArrayList<>();
        for (int i = 0; i < expectDataVolumeCount; i++) {
            deprecatedDataVolumeSpecs.add(DiskAO.nonRootDisk());
        }

        int index = 0;
        if (!isEmpty(msg.getDataDiskOfferingUuids())) {
            for (String dataDiskOfferingUuid : msg.getDataDiskOfferingUuids()) {
                deprecatedDataVolumeSpecs.get(index).setDiskOfferingUuid(dataDiskOfferingUuid);
                index++;
            }
        }
        if (!isEmpty(msg.getDataDiskSizes())) {
            for (Long dataDiskSize : msg.getDataDiskSizes()) {
                deprecatedDataVolumeSpecs.get(index).setSize(dataDiskSize);
                index++;
            }
        }

        // dataVolumeSystemTagsOnIndex -> deprecatedDataVolumeSpecs
        if (msg.getDataVolumeSystemTagsOnIndex() != null) {
            final Map<String, List<String>> dataVolumeSystemTagsOnIndex = msg.getDataVolumeSystemTagsOnIndex();
            for (int i = 0; i <= expectDataVolumeCount; i++) {
                String key = i + "";
                if (!dataVolumeSystemTagsOnIndex.containsKey(key)) {
                    continue;
                }

                final DiskAO diskAO = deprecatedDataVolumeSpecs.get(i);
                if (diskAO.getSystemTags() == null) {
                    diskAO.setSystemTags(new ArrayList<>());
                }
                diskAO.getSystemTags().addAll(dataVolumeSystemTagsOnIndex.get(key));
            }
        }
        cmsg.setDeprecatedDataVolumeSpecs(deprecatedDataVolumeSpecs);

        if (!isEmpty(msg.getSystemTags())) {
            for (Iterator<String> it = msg.getSystemTags().iterator(); it.hasNext();) {
                String tag = it.next();
                // systemTags:  primaryStorageUuidForDataVolume::{uuid} -> candidatePrimaryStorageUuidsForDataVolume
                if (PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.isMatch(tag)) {
                    String psUuid = PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.getTokenByTag(tag, PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
                    cmsg.setCandidatePrimaryStorageUuidsForDataVolume(list(psUuid));

                    if (cmsg.getDataDisks() != null) {
                        cmsg.getDataDisks().forEach(diskAO -> diskAO.setPrimaryStorageUuid(psUuid));
                    }
                    cmsg.getDeprecatedDataVolumeSpecs().forEach(diskAO -> diskAO.setPrimaryStorageUuid(psUuid));
                    it.remove();
                }
            }
        }

        applyForceEncryptEnvOverride(cmsg);
        return cmsg;
    }

    /**
     * Temporary debug switch. Priority (first match wins):
     * <ol>
     *   <li>{@link #FORCE_ENCRYPT_VOLUME_HARDCODED} — flip in code, rebuild, deploy.
     *       Use this when the deployment regenerates setenv.sh / systemd unit and
     *       JVM properties can't be reliably injected.</li>
     *   <li>System property {@code zstack.force.encrypt.volume} —
     *       pass via {@code -Dzstack.force.encrypt.volume=true}.</li>
     *   <li>Environment variable {@code ZSTACK_FORCE_ENCRYPT_VOLUME}.</li>
     * </ol>
     * When the switch is on, every {@link DiskAO} on the {@link CreateVmInstanceMsg}
     * is force-marked encrypted=true; when off, encrypted=false. Covers all five
     * disk sources funneled into this msg by {@link #fromAPICreateVmInstanceMsg}:
     * <ul>
     *   <li>root disk (empty / from-image)</li>
     *   <li>data disks from {@code APICreateVmInstanceMsg.diskAOs}
     *       (from-image / from-existing-volume)</li>
     *   <li>data disks from the legacy {@code dataDiskOfferingUuids /
     *       dataDiskSizes} path (deprecatedDataVolumeSpecs)</li>
     * </ul>
     */
    private static final Boolean FORCE_ENCRYPT_VOLUME_HARDCODED = false;
    static final String FORCE_ENCRYPT_VOLUME_ENV = "ZSTACK_FORCE_ENCRYPT_VOLUME";
    private static final String FORCE_ENCRYPT_VOLUME_PROPERTY = "zstack.force.encrypt.volume";

    private static boolean isForceEncryptVolume() {
        if (FORCE_ENCRYPT_VOLUME_HARDCODED != null) {
            return FORCE_ENCRYPT_VOLUME_HARDCODED;
        }
        String v = System.getProperty(FORCE_ENCRYPT_VOLUME_PROPERTY);
        if (v == null || v.isEmpty()) {
            v = System.getenv(FORCE_ENCRYPT_VOLUME_ENV);
        }
        if (v == null || v.isEmpty()) {
            return false;
        }
        v = v.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }

    private static void applyForceEncryptEnvOverride(CreateVmInstanceMsg cmsg) {
        boolean forceOn = isForceEncryptVolume();
        if (cmsg.getRootDisk() != null) {
            cmsg.getRootDisk().setEncrypted(forceOn);
        }
        if (cmsg.getDataDisks() != null) {
            for (DiskAO d : cmsg.getDataDisks()) {
                if (d != null) {
                    d.setEncrypted(forceOn);
                }
            }
        }
        if (cmsg.getDeprecatedDataVolumeSpecs() != null) {
            for (DiskAO d : cmsg.getDeprecatedDataVolumeSpecs()) {
                if (d != null) {
                    d.setEncrypted(forceOn);
                }
            }
        }
    }

    private static String getPSUuidForDataVolume(List<String> systemTags){
        if (systemTags == null || systemTags.isEmpty()){
            return null;
        }

        return SystemTagUtils.findTagValue(systemTags, PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
    }

    public static UpdateVmInstanceSpec convertToSpec(UpdateVmInstanceMsg message, VmInstanceVO vm) {
        requireNonNull(message);
        String vmUuid = requireNonNull(requireNonNull(vm).getUuid());

        UpdateVmInstanceSpec spec = new UpdateVmInstanceSpec();
        spec.setVmInstanceUuid(vmUuid);

        if (vm.getHostUuid() != null) {
            spec.setHostUuid(vm.getHostUuid());
        } else if (vm.getLastHostUuid() != null) {
            spec.setHostUuid(vm.getLastHostUuid());
        } else {
            throw new OperationFailureException(Platform.operr("failed to find host of vm[uuid=%s]", vmUuid));
        }

        if (!Objects.equals(vm.getName(), message.getName())) {
            spec.setName(message.getName());
        }
        if (!Objects.equals(vm.getCpuNum(), message.getCpuNum())) {
            spec.setCpuNum(message.getCpuNum());
        }
        if (!Objects.equals(vm.getMemorySize(), message.getMemorySize())) {
            spec.setMemorySize(message.getMemorySize());
        }
        if (!Objects.equals(vm.getReservedMemorySize(), message.getReservedMemorySize())) {
            spec.setReservedMemorySize(message.getReservedMemorySize());
        }

        return spec;
    }

    public static UpdateVmInstanceSpec convertToSpec(APIChangeInstanceOfferingMsg message,
                                                     InstanceOfferingInventory inv,
                                                     VmInstanceVO vm) {
        requireNonNull(message);
        final String vmUuid = requireNonNull(vm.getUuid());

        UpdateVmInstanceSpec spec = new UpdateVmInstanceSpec();
        spec.setVmInstanceUuid(vmUuid);

        if (vm.getHostUuid() != null) {
            spec.setHostUuid(vm.getHostUuid());
        } else if (vm.getLastHostUuid() != null) {
            spec.setHostUuid(vm.getLastHostUuid());
        } else {
            throw new OperationFailureException(Platform.operr("failed to find host of vm[uuid=%s]", vmUuid));
        }

        if (!Objects.equals(vm.getCpuNum(), inv.getCpuNum())) {
            spec.setCpuNum(inv.getCpuNum());
        }
        if (!Objects.equals(vm.getMemorySize(), inv.getMemorySize())) {
            spec.setMemorySize(inv.getMemorySize());
        }

        return spec;
    }

    private static void setVmInstanceInfoFromRootDiskAO(CreateVmInstanceMsg cmsg, APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }
        DiskAO rootdiskAO = msg.getDiskAOs().stream()
                .filter(DiskAO::isBoot).findFirst().orElse(null);
        if (rootdiskAO == null) {
            return;
        }
        cmsg.setPlatform(rootdiskAO.getPlatform());
        cmsg.setGuestOsType(rootdiskAO.getGuestOsType());
        cmsg.setArchitecture(rootdiskAO.getArchitecture());
        if (CollectionUtils.isNotEmpty(rootdiskAO.getSystemTags())
                && rootdiskAO.getSystemTags().contains(VmSystemTags.VIRTIO.getTagFormat())) {
            cmsg.setVirtio(true);
        }
    }
}
