package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.stringtemplate.v4.ST;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.kvm.KVMHostFactory.allGuestOsCharacter;

public class HygonTagKvmStartVmExtension implements KVMStartVmExtensionPoint {
    @Autowired
    ResourceConfigFacade rcf;

    private static final CLogger logger = Utils.getLogger(KVMHost.class);

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!HostSystemTags.HOST_CPU_MODEL_NAME.getTokenByResourceUuid(host.getUuid(), HostSystemTags.HOST_CPU_MODEL_NAME_TOKEN).toLowerCase().contains("hygon")) {
            return;
        }

        if (spec.getVmInventory().getType().equals("ApplianceVm")) {
            return;
        }

        //hygonTag host and UserVm
        String vmCpuMode = Q.New(ResourceConfigVO.class)
                .select(ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, KVMGlobalConfig.NESTED_VIRTUALIZATION.getName())
                .eq(ResourceConfigVO_.resourceUuid, spec.getVmInventory().getUuid())
                .eq(ResourceConfigVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .findValue();

        if (vmCpuMode == null) {
            NullVmCpuModeSetVmCpuMode(cmd, spec);
        } else if (vmCpuMode.equals(KVMConstant.CPU_MODE_NONE)) {
            NoneVmCpuModeSetVmCpuMode(cmd, spec);
        } else if (vmCpuMode.equals(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED)) {
            HygonCustomizedVmCpuModeSetVmCpuMode(cmd);
        }
    }

    private void HygonCustomizedVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd) {
        cmd.setNestedVirtualization(KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        cmd.setVmCpuModel(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
    }

    private boolean GuestOsNotNeedHygonTag(VmInstanceSpec spec) {
        String vmArchPlatformRelease = String.format("%s_%s_%s", spec.getVmInventory().getArchitecture(), spec.getVmInventory().getPlatform(), spec.getVmInventory().getGuestOsType());
        if (allGuestOsCharacter.containsKey(vmArchPlatformRelease)) {
            if (allGuestOsCharacter.get(vmArchPlatformRelease).getHygonTag() != null && !allGuestOsCharacter.get(vmArchPlatformRelease).getHygonTag()) {
                return true;
            }
        }
        return false;
    }

    private void NoneVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        //special vm which not need add hygonTag
        if (GuestOsNotNeedHygonTag(spec)) {
            return;
        }

        //other vm which need add hygonTag
        cmd.setNestedVirtualization(KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        cmd.setVmCpuModel(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
        //upgrade vm.cpuMode ResourceConfig
        ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        rc.updateValue(spec.getVmInventory().getUuid(), KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
    }

    private void NullVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        //special vm which not need add hygonTag
        if (GuestOsNotNeedHygonTag(spec)) {
            //if vm.cpuMode cluster level is hygon_customized, change mode to none
            //not change vm.cpuMode resourceConfig in cluster level
            String NotNeedAddHygonTagVmCpuMode = rcf.getResourceConfigValue(KVMGlobalConfig.NESTED_VIRTUALIZATION, spec.getVmInventory().getUuid(), String.class);
            if (NotNeedAddHygonTagVmCpuMode.equals(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED)) {
                cmd.setVmCpuModel(KVMConstant.CPU_MODE_NONE);
            }
            return;
        }

        //other vm which need add hygonTag
        cmd.setNestedVirtualization(KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        cmd.setVmCpuModel(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
