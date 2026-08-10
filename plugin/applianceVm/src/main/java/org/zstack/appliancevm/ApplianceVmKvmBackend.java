package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.kvm.KVMAddons;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Locale;

/**
 */
public class ApplianceVmKvmBackend implements KVMStartVmAddonExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmKvmBackend.class);
    private static final String AARCH64 = "aarch64";
    private static final String ALINUX_DISTRIBUTION_MARKER = "alinux";
    private static final String ALIBABA_DISTRIBUTION_MARKER = "alibaba";

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return ApplianceVmFactory.type;
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        KVMAddons.Channel chan = new KVMAddons.Channel();
        KVMAddons.Channel chan_vr = new KVMAddons.Channel();
        cmd.setEmulateHyperV(false);

        chan.setSocketPath(makeChannelSocketPath(spec.getVmInventory().getUuid()));
        chan.setTargetName("applianceVm.vport");
        Integer virtioSerialPort = getAliLinuxVirtioSerialPort(host);
        if (virtioSerialPort != null) {
            chan.setVirtioSerialPort(virtioSerialPort);
        }
        cmd.getAddons().put(KVMAddons.Channel.NAME, chan);
        logger.debug(String.format("make kvm channel device[path:%s, target:%s, virtioSerialPort:%s]", chan.getSocketPath(), chan.getTargetName(), chan.getVirtioSerialPort()));

        chan_vr.setSocketPath(makeChannelVRSocketPath(spec.getVmInventory().getUuid()));
        chan_vr.setTargetName("org.qemu.guest_agent.0"); // this channel name must keep org.qemu.guest_agent to use qemu-ga
        cmd.getAddons().put(KVMAddons.Channel.VR_NAME, chan_vr);
        logger.debug(String.format("make kvm channel device[path:%s, target:%s]", chan_vr.getSocketPath(), chan_vr.getTargetName()));
    }

    public String makeChannelSocketPath(String apvmuuid) {
        return PathUtil.join(ApplianceVmConstant.KVM_CHANNEL_AGENT_PATH, String.format("applianceVm.%s", apvmuuid));
    }
    private String makeChannelVRSocketPath(String apvmuuid) {
        return PathUtil.join(ApplianceVmConstant.KVM_CHANNEL_QEMU_GA_PATH, apvmuuid);
    }

    private Integer getAliLinuxVirtioSerialPort(KVMHostInventory host) {
        String osDistribution = host.getOsDistribution();
        if (!AARCH64.equals(host.getArchitecture()) || osDistribution == null) {
            return null;
        }

        String normalizedDistribution = osDistribution.toLowerCase(Locale.ROOT);
        if (!normalizedDistribution.contains(ALINUX_DISTRIBUTION_MARKER) &&
                !normalizedDistribution.contains(ALIBABA_DISTRIBUTION_MARKER)) {
            return null;
        }

        int port = ApplianceVmGlobalConfig.ALINUX_VIRTIO_SERIAL_PORT.value(Integer.class);
        return port == ApplianceVmGlobalConfig.DISABLED_ALINUX_VIRTIO_SERIAL_PORT ? null : port;
    }
}
