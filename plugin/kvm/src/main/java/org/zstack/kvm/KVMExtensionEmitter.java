package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.CheckSnapshotOnHypervisorMsg;
import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.DetachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KVMExtensionEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(KVMExtensionEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<KVMStartVmExtensionPoint> startVmExts = new ArrayList<>();
    private List<KVMDestroyVmExtensionPoint> destroyVmExts = new ArrayList<>();
    private List<KVMStopVmExtensionPoint> stopVmExts = new ArrayList<>();
    private List<KVMRebootVmExtensionPoint> rebootVmExts = new ArrayList<>();
    private List<KVMStartVmAddonExtensionPoint> addonsExts = new ArrayList<>();
    private List<KVMAttachVolumeExtensionPoint> attachVolumeExts = new ArrayList<>();
    private List<KVMDetachVolumeExtensionPoint> detachVolumeExts = new ArrayList<>();
    private List<KVMTakeSnapshotExtensionPoint> takeSnapshotExts = new ArrayList<>();
    private List<KVMCheckSnapshotExtensionPoint> checkSnapshotExts = new ArrayList<>();
    private List<KVMMergeSnapshotExtensionPoint> mergeSnapshotExts = new ArrayList<>();
    private List<KVMCheckVmStateExtensionPoint> checkVmStateExts = new ArrayList<>();

    private void populateExtensions() {
        startVmExts = pluginRgty.getExtensionList(KVMStartVmExtensionPoint.class);
        destroyVmExts = pluginRgty.getExtensionList(KVMDestroyVmExtensionPoint.class);
        stopVmExts = pluginRgty.getExtensionList(KVMStopVmExtensionPoint.class);
        rebootVmExts = pluginRgty.getExtensionList(KVMRebootVmExtensionPoint.class);
        addonsExts = pluginRgty.getExtensionList(KVMStartVmAddonExtensionPoint.class);
        attachVolumeExts = pluginRgty.getExtensionList(KVMAttachVolumeExtensionPoint.class);
        detachVolumeExts = pluginRgty.getExtensionList(KVMDetachVolumeExtensionPoint.class);
        takeSnapshotExts = pluginRgty.getExtensionList(KVMTakeSnapshotExtensionPoint.class);
        mergeSnapshotExts = pluginRgty.getExtensionList(KVMMergeSnapshotExtensionPoint.class);
        checkVmStateExts = pluginRgty.getExtensionList(KVMCheckVmStateExtensionPoint.class);
        checkSnapshotExts = pluginRgty.getExtensionList(KVMCheckSnapshotExtensionPoint.class);
    }

    public void beforeStartVmOnKvm(final KVMHostInventory host, final VmInstanceSpec spec, final StartVmCmd cmd) {
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

    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) throws KVMException {
        for (KVMDestroyVmExtensionPoint extp : destroyVmExts) {
            extp.beforeDestroyVmOnKvm(host, vm, cmd);
        }
    }

    public void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd) {
        for (KVMDestroyVmExtensionPoint extp : destroyVmExts) {
            extp.beforeDirectlyDestroyVmOnKvm(cmd);
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

    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.StopVmCmd cmd) throws KVMException {
        for (KVMStopVmExtensionPoint extp : stopVmExts) {
            extp.beforeStopVmOnKvm(host, vm, cmd);
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

    public void doBeforeTakeSnapshot(final Iterator<KVMTakeSnapshotExtensionPoint> it, KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                     KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        KVMTakeSnapshotExtensionPoint ext = it.next();
        ext.beforeTakeSnapshot(host, msg, cmd, new Completion(completion) {
            @Override
            public void success() {
                doBeforeTakeSnapshot(it, host, msg, cmd, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void beforeCheckSnapshot(KVMHostInventory host, CheckSnapshotOnHypervisorMsg msg, KVMAgentCommands.CheckSnapshotCmd cmd, Completion completion) {
        ErrorCodeList errorCodeList = new ErrorCodeList();
        new While<>(checkSnapshotExts).each((ext, whileCompletion) -> {
            ext.beforeCheckSnapshot(host, msg, cmd, new Completion(whileCompletion) {
                @Override
                public void success() {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errorCodeList.getCauses().add(errorCode);
                    whileCompletion.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success();
            }
        });
    }

    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        Iterator<KVMTakeSnapshotExtensionPoint> it = takeSnapshotExts.iterator();
        doBeforeTakeSnapshot(it, host, msg, cmd, completion);
    }

    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp) {
        for (KVMTakeSnapshotExtensionPoint ext : takeSnapshotExts) {
            ext.afterTakeSnapshot(host, msg, cmd, rsp);
        }
    }

    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err) {
        for (KVMTakeSnapshotExtensionPoint ext : takeSnapshotExts) {
            ext.afterTakeSnapshotFailed(host, msg, cmd, rsp, err);
        }
    }

    public void beforeMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd) {
        for (KVMMergeSnapshotExtensionPoint ext : mergeSnapshotExts) {
            ext.beforeMergeSnapshot(host, msg, cmd);
        }
    }

    public void beforeCheckVmState(KVMHostInventory host, CheckVmStateOnHypervisorMsg msg, KVMAgentCommands.CheckVmStateCmd cmd) {
        for (KVMCheckVmStateExtensionPoint ext : checkVmStateExts) {
            ext.beforeCheckVmState(host, msg, cmd);
        }
    }

    public void afterCheckVmState(HostInventory host, Map<String, String> vmStateMap) {
        for (KVMCheckVmStateExtensionPoint ext : checkVmStateExts) {
            ext.afterCheckVmState(host, vmStateMap);
        }
    }

    public void afterMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd) {
        for (KVMMergeSnapshotExtensionPoint ext : mergeSnapshotExts) {
            ext.afterMergeSnapshot(host, msg, cmd);
        }
    }

    public void afterMergeSnapshotFailed(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd, ErrorCode err) {
        for (KVMMergeSnapshotExtensionPoint ext : mergeSnapshotExts) {
            ext.afterMergeSnapshotFailed(host, msg, cmd, err);
        }
    }

    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, AttachDataVolumeCmd cmd, Map data) {
        for (KVMAttachVolumeExtensionPoint ext : attachVolumeExts) {
            ext.beforeAttachVolume(host, vm, vol, cmd, data);
        }
    }

    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, AttachDataVolumeCmd cmd) {
        for (KVMAttachVolumeExtensionPoint ext : attachVolumeExts) {
            ext.afterAttachVolume(host, vm, vol, cmd);
        }
    }

    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, AttachDataVolumeCmd cmd, ErrorCode err, Map data) {
        for (KVMAttachVolumeExtensionPoint ext : attachVolumeExts) {
            ext.attachVolumeFailed(host, vm, vol, cmd, err, data);
        }
    }

    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, DetachDataVolumeCmd cmd) {
        for (KVMDetachVolumeExtensionPoint ext : detachVolumeExts) {
            ext.beforeDetachVolume(host, vm, vol, cmd);
        }
    }

    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, DetachDataVolumeCmd cmd) {
        for (KVMDetachVolumeExtensionPoint ext : detachVolumeExts) {
            ext.afterDetachVolume(host, vm, vol, cmd);
        }
    }

    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory vol, DetachDataVolumeCmd cmd, ErrorCode err) {
        for (KVMDetachVolumeExtensionPoint ext : detachVolumeExts) {
            ext.detachVolumeFailed(host, vm, vol, cmd, err);
        }
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
