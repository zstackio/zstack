package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KVMExtensionEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(KVMExtensionEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<KVMStartVmExtensionPoint> startVmExts = new ArrayList<KVMStartVmExtensionPoint>();
    private List<KVMDestroyVmExtensionPoint> destroyVmExts = new ArrayList<KVMDestroyVmExtensionPoint>();
    private List<KVMStopVmExtensionPoint> stopVmExts = new ArrayList<KVMStopVmExtensionPoint>();
    private List<KVMRebootVmExtensionPoint> rebootVmExts = new ArrayList<KVMRebootVmExtensionPoint>();
    private List<KVMStartVmAddonExtensionPoint> addonsExts = new ArrayList<KVMStartVmAddonExtensionPoint>();

    private void populateExtensions() {
        startVmExts = pluginRgty.getExtensionList(KVMStartVmExtensionPoint.class);
        destroyVmExts = pluginRgty.getExtensionList(KVMDestroyVmExtensionPoint.class);
        stopVmExts = pluginRgty.getExtensionList(KVMStopVmExtensionPoint.class);
        rebootVmExts = pluginRgty.getExtensionList(KVMRebootVmExtensionPoint.class);
        addonsExts = pluginRgty.getExtensionList(KVMStartVmAddonExtensionPoint.class);
    }

    public void beforeStartVmOnKvm(final KVMHostInventory host, final VmInstanceSpec spec, final StartVmCmd cmd) throws KVMException {
        for (KVMStartVmExtensionPoint extp : startVmExts) {
            extp.beforeStartVmOnKvm(host, spec, cmd);
        }
    }

    public void startVmOnKvmSuccess(final KVMHostInventory host, final VmInstanceSpec spec) {
        CollectionUtils.safeForEach(startVmExts, new ForEachFunction<KVMStartVmExtensionPoint>() {
            @Override
            public void run(KVMStartVmExtensionPoint arg) {
                arg.startVmOnKvmSuccess(host, spec);
            }
        });
    }

    public void startVmOnKvmFailed(final KVMHostInventory host, final VmInstanceSpec spec, final ErrorCode err) {
        CollectionUtils.safeForEach(startVmExts, new ForEachFunction<KVMStartVmExtensionPoint>() {
            @Override
            public void run(KVMStartVmExtensionPoint arg) {
                arg.startVmOnKvmFailed(host, spec, err);
            }
        });
    }

    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {
        for (KVMDestroyVmExtensionPoint extp : destroyVmExts) {
            extp.beforeDestroyVmOnKvm(host, vm);
        }
    }

    public void destroyVmOnKvmSuccess(final KVMHostInventory host, final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(destroyVmExts, new ForEachFunction<KVMDestroyVmExtensionPoint>() {
            @Override
            public void run(KVMDestroyVmExtensionPoint arg) {
                arg.destroyVmOnKvmSuccess(host, vm);
            }
        });
    }

    public void destroyVmOnKvmFailed(final KVMHostInventory host, final VmInstanceInventory vm, final ErrorCode err) {
        CollectionUtils.safeForEach(destroyVmExts, new ForEachFunction<KVMDestroyVmExtensionPoint>() {
            @Override
            public void run(KVMDestroyVmExtensionPoint arg) {
                arg.destroyVmOnKvmFailed(host, vm, err);
            }
        });
    }

    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {
        for (KVMStopVmExtensionPoint extp : stopVmExts) {
            extp.beforeStopVmOnKvm(host, vm);
        }
    }

    public void stopVmOnKvmSuccess(final KVMHostInventory host, final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(stopVmExts, new ForEachFunction<KVMStopVmExtensionPoint>() {
            @Override
            public void run(KVMStopVmExtensionPoint arg) {
                arg.stopVmOnKvmSuccess(host, vm);
            }
        });
    }

    public void stopVmOnKvmFailed(final KVMHostInventory host, final VmInstanceInventory vm, final ErrorCode err) {
        CollectionUtils.safeForEach(stopVmExts, new ForEachFunction<KVMStopVmExtensionPoint>() {
            @Override
            public void run(KVMStopVmExtensionPoint arg) {
                arg.stopVmOnKvmFailed(host, vm, err);
            }
        });
    }

    public void beforeRebootVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {
        for (KVMRebootVmExtensionPoint extp : rebootVmExts) {
            extp.beforeRebootVmOnKvm(host, vm);
        }
    }

    public void rebootVmOnKvmSuccess(final KVMHostInventory host, final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(rebootVmExts, new ForEachFunction<KVMRebootVmExtensionPoint>() {
            @Override
            public void run(KVMRebootVmExtensionPoint arg) {
                arg.rebootVmOnKvmSuccess(host, vm);
            }
        });
    }

    public void rebootVmOnKvmFailed(final KVMHostInventory host, final VmInstanceInventory vm, final ErrorCode err) {
        CollectionUtils.safeForEach(rebootVmExts, new ForEachFunction<KVMRebootVmExtensionPoint>() {
            @Override
            public void run(KVMRebootVmExtensionPoint arg) {
                arg.rebootVmOnKvmFailed(host, vm, err);
            }
        });
    }

    public void addOn(final KVMHostInventory host, final VmInstanceSpec spec, final StartVmCmd cmd) {
        CollectionUtils.safeForEach(addonsExts, new ForEachFunction<KVMStartVmAddonExtensionPoint>() {
            @Override
            public void run(KVMStartVmAddonExtensionPoint extp) {
                if (spec.getVmInventory().getType().equals(extp.getVmTypeForAddonExtension().toString())) {
                    extp.addAddon(host, spec, cmd);
                }
            }
        });
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
