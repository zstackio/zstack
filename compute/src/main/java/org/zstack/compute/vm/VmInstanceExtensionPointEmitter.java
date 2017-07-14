package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.List;

public class VmInstanceExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(VmInstanceExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    private List<VmInstanceStartNewCreatedVmExtensionPoint> startNewCreatedVmExtensions;
    private List<VmInstanceStopExtensionPoint> stopVmExtensions;
    private List<VmInstanceRebootExtensionPoint> rebootVmExtensions;
    private List<VmInstanceDestroyExtensionPoint> destroyVmExtensions;
    private List<VmInstanceStartExtensionPoint> startVmExtensions;
    private List<VmInstanceMigrateExtensionPoint> migrateVmExtensions;
    private List<VmAttachVolumeExtensionPoint> attachVolumeExtensions;
    private List<VmDetachVolumeExtensionPoint> detachVolumeExtensions;

    public ErrorCode preStartNewCreatedVm(VmInstanceInventory inv) {
        for (VmInstanceStartNewCreatedVmExtensionPoint ext : startNewCreatedVmExtensions) {
            try {
                String err = ext.preStartNewCreatedVm(inv);
                if (err != null) {
                    return operr("VmInstanceStartNewCreatedVmExtensionPoint[%s] refuses to create vm[uuid:%s] because %s",
                            ext.getClass().getName(), inv.getUuid(), err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
            }
        }
        return null;
    }

    public void beforeStartNewCreatedVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startNewCreatedVmExtensions, new ForEachFunction<VmInstanceStartNewCreatedVmExtensionPoint>() {
            @Override
            public void run(VmInstanceStartNewCreatedVmExtensionPoint arg) {
                arg.beforeStartNewCreatedVm(inv);
            }
        });
    }

    public void afterStartNewCreatedVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(startNewCreatedVmExtensions, new ForEachFunction<VmInstanceStartNewCreatedVmExtensionPoint>() {
            @Override
            public void run(VmInstanceStartNewCreatedVmExtensionPoint arg) {
                arg.afterStartNewCreatedVm(inv);
            }
        });
    }

    public void failedToStartNewCreatedVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.forEach(startNewCreatedVmExtensions, new ForEachFunction<VmInstanceStartNewCreatedVmExtensionPoint>() {
            @Override
            public void run(VmInstanceStartNewCreatedVmExtensionPoint arg) {
                arg.failedToStartNewCreatedVm(inv, reason);
            }
        });
    }

    public ErrorCode preStopVm(VmInstanceInventory inv) {
        for (VmInstanceStopExtensionPoint ext : stopVmExtensions) {
            try {
                String err = ext.preStopVm(inv);
                if (err != null) {
                    return operr("VmInstanceStopVmExtensionPoint[%s] refuses to stop vm[uuid:%s] because %s",
                            ext.getClass().getName(), inv.getUuid(), err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
            }
        }
        return null;
    }

    public void beforeStopVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(stopVmExtensions, new ForEachFunction<VmInstanceStopExtensionPoint>() {
            @Override
            public void run(VmInstanceStopExtensionPoint arg) {
                arg.beforeStopVm(inv);
            }
        });
    }

    public void afterStopVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(stopVmExtensions, new ForEachFunction<VmInstanceStopExtensionPoint>() {
            @Override
            public void run(VmInstanceStopExtensionPoint arg) {
                arg.afterStopVm(inv);
            }
        });
    }

    public void failedToStopVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.forEach(stopVmExtensions, new ForEachFunction<VmInstanceStopExtensionPoint>() {
            @Override
            public void run(VmInstanceStopExtensionPoint arg) {
                arg.failedToStopVm(inv, reason);
            }
        });
    }

    public ErrorCode preRebootVm(final VmInstanceInventory inv) {
        for (VmInstanceRebootExtensionPoint ext : rebootVmExtensions) {
            try {
                String err = ext.preRebootVm(inv);
                if (err != null) {
                    return operr("VmInstanceRebootExtensionPoint[%s] refuses to reboot vm[uuid:%s] because %s", ext.getClass().getName(),
                            inv.getUuid(), err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
            }
        }
        return null;
    }

    public void beforeRebootVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(rebootVmExtensions, new ForEachFunction<VmInstanceRebootExtensionPoint>() {
            @Override
            public void run(VmInstanceRebootExtensionPoint arg) {
                arg.beforeRebootVm(inv);
            }
        });
    }

    public void afterRebootVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(rebootVmExtensions, new ForEachFunction<VmInstanceRebootExtensionPoint>() {
            @Override
            public void run(VmInstanceRebootExtensionPoint arg) {
                arg.afterRebootVm(inv);
            }
        });
    }

    public void failedToRebootVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(rebootVmExtensions, new ForEachFunction<VmInstanceRebootExtensionPoint>() {
            @Override
            public void run(VmInstanceRebootExtensionPoint arg) {
                arg.failedToRebootVm(inv, reason);
            }
        });
    }

    public ErrorCode preDestroyVm(VmInstanceInventory inv) {
        for (VmInstanceDestroyExtensionPoint ext : destroyVmExtensions) {
            try {
                String err = ext.preDestroyVm(inv);
                if (err != null) {
                    return operr("VmInstanceDestroyVmExtensionPoint[%s] refuses to destroy vm[uuid:%s] because %s", ext.getClass().getName(), inv.getUuid(), err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
            }
        }
        return null;
    }

    public void beforeDestroyVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(destroyVmExtensions, new ForEachFunction<VmInstanceDestroyExtensionPoint>() {
            @Override
            public void run(VmInstanceDestroyExtensionPoint arg) {
                arg.beforeDestroyVm(inv);
            }
        });
    }

    public void afterDestroyVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(destroyVmExtensions, new ForEachFunction<VmInstanceDestroyExtensionPoint>() {
            @Override
            public void run(VmInstanceDestroyExtensionPoint arg) {
                arg.afterDestroyVm(inv);
            }
        });
    }

    public void failedToDestroyVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(destroyVmExtensions, new ForEachFunction<VmInstanceDestroyExtensionPoint>() {
            @Override
            public void run(VmInstanceDestroyExtensionPoint arg) {
                arg.failedToDestroyVm(inv, reason);
            }
        });
    }

    public ErrorCode preStartVm(VmInstanceInventory inv) {
        for (VmInstanceStartExtensionPoint ext : startVmExtensions) {
            try {
                String err = ext.preStartVm(inv);
                if (err != null) {
                    return operr("VmInstanceStartExtensionPoint[%s] refuses to start vm[uuid:%s] because %s", ext.getClass().getName(),
                            inv.getUuid(), err);
                }
            } catch (Exception e) {
                logger.error(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
                throw e;
            }
        }
        return null;
    }

    public void beforeStartVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startVmExtensions, new ForEachFunction<VmInstanceStartExtensionPoint>() {
            @Override
            public void run(VmInstanceStartExtensionPoint arg) {
                arg.beforeStartVm(inv);
            }
        });
    }

    public void afterStartVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startVmExtensions, new ForEachFunction<VmInstanceStartExtensionPoint>() {
            @Override
            public void run(VmInstanceStartExtensionPoint arg) {
                arg.afterStartVm(inv);
            }
        });
    }

    public void failedToStartVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(startVmExtensions, new ForEachFunction<VmInstanceStartExtensionPoint>() {
            @Override
            public void run(VmInstanceStartExtensionPoint arg) {
                arg.failedToStartVm(inv, reason);
            }
        });
    }

    public void afterMigrateVm(final VmInstanceInventory inv, final String srcHostUuid) {
        CollectionUtils.safeForEach(migrateVmExtensions, new ForEachFunction<VmInstanceMigrateExtensionPoint>() {
            @Override
            public void run(VmInstanceMigrateExtensionPoint arg) {
                arg.afterMigrateVm(inv, srcHostUuid);
            }
        });
    }

    public void failedToMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, final ErrorCode reason) {
        CollectionUtils.safeForEach(migrateVmExtensions, new ForEachFunction<VmInstanceMigrateExtensionPoint>() {
            @Override
            public void run(final VmInstanceMigrateExtensionPoint arg) {
                arg.failedToMigrateVm(inv, dstHostUuid, reason);
            }
        });
    }

    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        for (VmAttachVolumeExtensionPoint ext : attachVolumeExtensions) {
            ext.preAttachVolume(vm, volume);
        }
    }

    public void beforeAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(attachVolumeExtensions, new ForEachFunction<VmAttachVolumeExtensionPoint>() {
            @Override
            public void run(VmAttachVolumeExtensionPoint arg) {
                arg.beforeAttachVolume(vm, volume);
            }
        });
    }

    public void afterAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(attachVolumeExtensions, new ForEachFunction<VmAttachVolumeExtensionPoint>() {
            @Override
            public void run(VmAttachVolumeExtensionPoint arg) {
                arg.afterAttachVolume(vm, volume);
            }
        });
    }

    public void failedToAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode) {
        CollectionUtils.safeForEach(attachVolumeExtensions, new ForEachFunction<VmAttachVolumeExtensionPoint>() {
            @Override
            public void run(VmAttachVolumeExtensionPoint arg) {
                arg.failedToAttachVolume(vm, volume, errorCode);
            }
        });
    }

    public void preDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        for (VmDetachVolumeExtensionPoint ext : detachVolumeExtensions) {
            ext.preDetachVolume(vm, volume);
        }
    }

    public void beforeDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(detachVolumeExtensions, new ForEachFunction<VmDetachVolumeExtensionPoint>() {
            @Override
            public void run(VmDetachVolumeExtensionPoint arg) {
                arg.beforeDetachVolume(vm, volume);
            }
        });
    }

    public void afterDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(detachVolumeExtensions, new ForEachFunction<VmDetachVolumeExtensionPoint>() {
            @Override
            public void run(VmDetachVolumeExtensionPoint arg) {
                arg.afterDetachVolume(vm, volume);
            }
        });
    }

    public void failedToDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode) {
        CollectionUtils.safeForEach(detachVolumeExtensions, new ForEachFunction<VmDetachVolumeExtensionPoint>() {
            @Override
            public void run(VmDetachVolumeExtensionPoint arg) {
                arg.failedToDetachVolume(vm, volume, errorCode);
            }
        });
    }

    private void populateExtensions() {
        startNewCreatedVmExtensions = pluginRgty.getExtensionList(VmInstanceStartNewCreatedVmExtensionPoint.class);
        stopVmExtensions = pluginRgty.getExtensionList(VmInstanceStopExtensionPoint.class);
        rebootVmExtensions = pluginRgty.getExtensionList(VmInstanceRebootExtensionPoint.class);
        destroyVmExtensions = pluginRgty.getExtensionList(VmInstanceDestroyExtensionPoint.class);
        startVmExtensions = pluginRgty.getExtensionList(VmInstanceStartExtensionPoint.class);
        migrateVmExtensions = pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class);
        attachVolumeExtensions = pluginRgty.getExtensionList(VmAttachVolumeExtensionPoint.class);
        detachVolumeExtensions = pluginRgty.getExtensionList(VmDetachVolumeExtensionPoint.class);
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
