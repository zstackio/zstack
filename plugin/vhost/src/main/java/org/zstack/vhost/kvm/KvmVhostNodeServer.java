package org.zstack.vhost.kvm;

import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.vm.VmAttachVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeProtocolCapability;
import org.zstack.kvm.*;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class KvmVhostNodeServer implements Component, KVMStartVmExtensionPoint,
        KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint,
        VmAttachVolumeExtensionPoint {
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    private PluginRegistry pluginRgty;

    private static final VolumeProtocolCapability capability = VolumeProtocolCapability
            .register(VolumeProtocol.Vhost.name(), KVMConstant.KVM_HYPERVISOR_TYPE);

    static  {
        capability.setSupportQosOnHypervisor(false);
        capability.setSupportResizeOnHypervisor(false);
        capability.setSupportReadonly(false);
    }

    private boolean needSupportVhostPrimaryStorage(String clusterUuid) {
        String generateVhostConfig = VmGlobalConfig.GENERATE_CONFIG_VHOST_REQUIRED.value(String.class);
        if (!"auto".equals(generateVhostConfig)) {
            return Boolean.parseBoolean(generateVhostConfig);
        }
        String sql = "select count(ref) from PrimaryStorageClusterRefVO ref, PrimaryStorageOutputProtocolRefVO protoRef" +
                " where ref.primaryStorageUuid = protoRef.primaryStorageUuid" +
                " and ref.clusterUuid = :clusterUuid" +
                " and protoRef.outputProtocol = :outputProtocol";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("clusterUuid", clusterUuid);
        q.setParameter("outputProtocol", VolumeProtocol.Vhost.name());
        Long count = q.getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeIfNeeded(spec.getDestRootVolume(), host, cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeIfNeeded(vol, host, to));
                    break;
                }
            }
        }

        if (VolumeProtocol.Vhost.name().equals(spec.getDestRootVolume().getProtocol()) ||
                spec.getDestDataVolumes().stream().anyMatch(v -> VolumeProtocol.Vhost.name().equals(v.getProtocol())) ||
                needSupportVhostPrimaryStorage(host.getClusterUuid())) {
            cmd.setUseHugePage(true);
            cmd.setMemAccess("shared");
        }

        cmd.setDataVolumes(dtos);

        // vhostuser disk not support readonly mode, so no iso.
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }


    private PrimaryStorageNodeSvc getNodeService(VolumeInventory volumeInventory) {
        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return null;
        }

        return extPsFactory.getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, HostInventory host, VolumeTO volumeTO) {
        if (!VolumeProtocol.Vhost.name().equals(volumeInventory.getProtocol())) {
            return volumeTO;
        }

        if (!volumeTO.isUseVirtio()) {
            throw new OperationFailureException(
                    argerr(ORG_ZSTACK_VHOST_KVM_10000, "vhostuser disk only support virtio mode, check image platform has virtio driver or not"));
        }

        if (volumeTO.isUseVirtioSCSI()) {
            throw new OperationFailureException(
                    argerr(ORG_ZSTACK_VHOST_KVM_10001, "vhostuser disk not support virtio-scsi mode, please turn off virtio-scsi mode"));
        }

        PrimaryStorageNodeSvc nodeSvc = getNodeService(volumeInventory);
        if (nodeSvc == null) {
            return volumeTO;
        }

        String path = nodeSvc.getActivePath(BaseVolumeInfo.valueOf(volumeInventory), host,false);
        volumeTO.setInstallPath(path);
        return volumeTO;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeIfNeeded(inventory, host, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {}

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {}

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {}
    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {}

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!VolumeProtocol.Vhost.name().equals(volume.getProtocol())) {
            completion.success();
            return;
        }
        if (vm.getHostUuid() == null || !VmInstanceState.Running.toString().equals(vm.getState())) {
            completion.success();
            return;
        }
        if (vmHasSharedMemory(vm)) {
            completion.success();
            return;
        }
        completion.fail(argerr(ORG_ZSTACK_VHOST_KVM_10002,
                "cannot attach vhost-user data volume online: the running VM does not have shared memory enabled." +
                " Either shut down the VM and then attach, or set the global config" +
                " 'generate.config.vhost.required' to 'auto' or 'true' and restart the VM" +
                " (this disables memory overcommit for the VM)"));
    }

    private boolean vmHasSharedMemory(VmInstanceInventory vm) {
        if (vm.getAllVolumes() != null && vm.getAllVolumes().stream()
                .anyMatch(v -> VolumeProtocol.Vhost.name().equals(v.getProtocol()))) {
            return true;
        }
        return needSupportVhostPrimaryStorage(vm.getClusterUuid());
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {}

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {}
}
