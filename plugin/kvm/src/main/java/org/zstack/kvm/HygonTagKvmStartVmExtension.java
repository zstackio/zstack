package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigInventory;

import java.util.List;

import static org.zstack.kvm.KVMHostFactory.allGuestOsCharacter;

public class HygonTagKvmStartVmExtension implements KVMStartVmExtensionPoint {
    @Autowired
    ResourceConfigFacade rcf;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!HostSystemTags.HOST_CPU_MODEL_NAME.getTokenByResourceUuid(host.getUuid(), HostSystemTags.HOST_CPU_MODEL_NAME_TOKEN).toLowerCase().contains("hygon")) {
            return;
        }

        if (spec.getVmInventory().getType().equals("ApplianceVm")) {
            return;
        }

        // vm_level cpu mode
        String vmLevelCpuMode = null;
        ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        List<ResourceConfigInventory> configs = rc.getEffectiveResourceConfigs(spec.getVmInventory().getUuid());
        if (!configs.isEmpty()) {
            if (VmInstanceVO.class.getSimpleName().equals(configs.get(0).getResourceType())){
                vmLevelCpuMode = configs.get(0).getValue();
            }
        }

        if (vmLevelCpuMode == null) {
            nullVmCpuModeSetVmCpuMode(cmd, spec);
        } else if (vmLevelCpuMode.equals(KVMConstant.CPU_MODE_NONE)) {
            noneVmCpuModeSetVmCpuMode(cmd, spec);
        } else if (vmLevelCpuMode.equals(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED)) {
            hygonCustomizedVmCpuModeSetVmCpuMode(cmd, spec);
        }
    }

    private void hygonCustomizedVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        //special vm which not need add hygonTag
        if (guestOsNotNeedHygonTag(spec)) {
            cmd.setNestedVirtualization(KVMConstant.CPU_MODE_NONE);
            cmd.setVmCpuModel(null);
            //upgrade vm.cpuMode ResourceConfig
            ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
            rc.updateValue(spec.getVmInventory().getUuid(), KVMConstant.CPU_MODE_NONE);
            return;
        }

        cmd.setNestedVirtualization(KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        cmd.setVmCpuModel(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
    }

    private boolean guestOsNotNeedHygonTag(VmInstanceSpec spec) {
        String vmArchPlatformRelease = String.format("%s_%s_%s", spec.getVmInventory().getArchitecture(), spec.getVmInventory().getPlatform(), spec.getVmInventory().getGuestOsType());
        if (allGuestOsCharacter.containsKey(vmArchPlatformRelease)) {
            if (allGuestOsCharacter.get(vmArchPlatformRelease).getHygonTag() != null && !allGuestOsCharacter.get(vmArchPlatformRelease).getHygonTag()) {
                return true;
            }
        }
        return false;
    }

    private void noneVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        //special vm which not need add hygonTag
        if (guestOsNotNeedHygonTag(spec)) {
            return;
        }

        //other vm which need add hygonTag
        cmd.setNestedVirtualization(KVMConstant.CPU_MODE_HOST_PASSTHROUGH);
        cmd.setVmCpuModel(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
        //upgrade vm.cpuMode ResourceConfig
        ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        rc.updateValue(spec.getVmInventory().getUuid(), KVMConstant.CPU_MODE_HYGON_CUSTOMIZED);
    }

    private void nullVmCpuModeSetVmCpuMode(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        //special vm which not need add hygonTag
        if (guestOsNotNeedHygonTag(spec)) {
            //if vm.cpuMode cluster level is hygon_customized, change mode to none
            //not change vm.cpuMode resourceConfig in cluster level
            String notNeedAddHygonTagVmCpuMode = rcf.getResourceConfigValue(KVMGlobalConfig.NESTED_VIRTUALIZATION, spec.getVmInventory().getClusterUuid(), String.class);
            if (notNeedAddHygonTagVmCpuMode.equals(KVMConstant.CPU_MODE_HYGON_CUSTOMIZED)) {
                cmd.setNestedVirtualization(KVMConstant.CPU_MODE_NONE);
                cmd.setVmCpuModel(null);
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
