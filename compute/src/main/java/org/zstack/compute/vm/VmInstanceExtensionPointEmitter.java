package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VmInstanceExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(VmInstanceExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    private List<VmInstanceBeforeStartExtensionPoint> VmInstanceBeforeStartExtensions;
    private List<VmInstanceResumeExtensionPoint> VmInstanceResumeExtensionPoints;
    private List<VmInstanceStartNewCreatedVmExtensionPoint> startNewCreatedVmExtensions;
    private List<BeforeVmInstanceStopExtensionPoint> beforeVmStopExtensions;
    private List<VmInstanceStopExtensionPoint> stopVmExtensions;
    private List<VmInstanceRebootExtensionPoint> rebootVmExtensions;
    private List<VmInstanceDestroyExtensionPoint> destroyVmExtensions;
    private List<VmInstanceStartExtensionPoint> startVmExtensions;
    private List<VmInstanceMigrateExtensionPoint> migrateVmExtensions;
    private List<VmAttachVolumeExtensionPoint> attachVolumeExtensions;
    private List<VmDetachVolumeExtensionPoint> detachVolumeExtensions;
    private List<VmCapabilitiesExtensionPoint> capabilitiesExtensionPoints;
    private List<CleanUpAfterVmFailedToStartExtensionPoint> cleanUpAfterVmFailedToStartExtensionPoints;
    private List<CleanUpAfterVmChangeImageExtensionPoint> cleanUpAfterVmChangeImageExtensionPoints;
    private List<VmNicChangeStateExtensionPoint> vmNicChangeStateExtensionPoints;

    public List<ErrorCode> handleSystemTag(String vmUuid, List<String> tags){
        List<ErrorCode> errorCodes = new ArrayList<>();
        CollectionUtils.safeForEach(VmInstanceBeforeStartExtensions, extension -> {
            ErrorCode errorCode = extension.handleSystemTag(vmUuid, tags);
            if (errorCode != null) {
                errorCodes.add(errorCode);
            }
        });
        return errorCodes;
    }

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

    public void beforeVmStop(VmInstanceInventory inv, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.allowEmptyFlow();
        for(BeforeVmInstanceStopExtensionPoint ext: beforeVmStopExtensions) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ext.beforeVmInstanceStop(inv, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public ErrorCode preStopVm(VmInstanceInventory inv) {
        for (VmInstanceStopExtensionPoint ext : stopVmExtensions) {
            try {
                String err = ext.preStopVm(inv);
                if (err != null) {
                    return errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, String.format("VmInstanceStopVmExtensionPoint[%s] refuses to stop vm[uuid:%s] because %s",
                            ext.getClass().getName(), inv.getUuid(), err));
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

    public void afterFailedToStopVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.forEach(stopVmExtensions, new ForEachFunction<VmInstanceStopExtensionPoint>() {
            @Override
            public void run(VmInstanceStopExtensionPoint arg) {
                arg.afterFailedToStopVm(inv, reason);
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

    public void afterResumeVm(VmInstanceInventory inv) {
        CollectionUtils.safeForEach(VmInstanceResumeExtensionPoints, new ForEachFunction<VmInstanceResumeExtensionPoint>() {
            @Override
            public void run(VmInstanceResumeExtensionPoint arg) {
                arg.afterResumeVm(inv);
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

    public void preMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, Completion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.preMigrateVm(inv, dstHostUuid, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.addError(errorCode);
                comp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() > 0) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    public void beforeMigrateVm(final VmInstanceInventory inv, final String dstHostUuid) {
        CollectionUtils.safeForEach(migrateVmExtensions, arg -> arg.beforeMigrateVm(inv, dstHostUuid));
    }

    public void afterMigrateVm(final VmInstanceInventory inv, final String srcHostUuid, NoErrorCompletion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.afterMigrateVm(inv, srcHostUuid, new NoErrorCompletion(comp) {
            @Override
            public void done() {
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void failedToMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, final ErrorCode reason, NoErrorCompletion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.failedToMigrateVm(inv, dstHostUuid, reason, new NoErrorCompletion(comp) {
            @Override
            public void done() {
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        for (VmAttachVolumeExtensionPoint ext : attachVolumeExtensions) {
            ext.preAttachVolume(vm, volume);
        }
    }

    public void beforeAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume, Map data) {
        CollectionUtils.safeForEach(attachVolumeExtensions, new ForEachFunction<VmAttachVolumeExtensionPoint>() {
            @Override
            public void run(VmAttachVolumeExtensionPoint arg) {
                arg.beforeAttachVolume(vm, volume, data);
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

    public void failedToAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode, Map data) {
        CollectionUtils.safeForEach(attachVolumeExtensions, new ForEachFunction<VmAttachVolumeExtensionPoint>() {
            @Override
            public void run(VmAttachVolumeExtensionPoint arg) {
                arg.failedToAttachVolume(vm, volume, errorCode, data);
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

    public void afterDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final Completion completion) {
        if (detachVolumeExtensions.isEmpty()) {
            completion.success();
            return;
        }
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        CollectionUtils.safeForEach(detachVolumeExtensions, new ForEachFunction<VmDetachVolumeExtensionPoint>() {
            @Override
            public void run(VmDetachVolumeExtensionPoint arg) {
                chain.then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        arg.afterDetachVolume(vm, volume, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.debug(String.format("found a error when calling afterDetachVolume[volumeUuid:%s] extension point" +
                                        ":%s, ignore it", volume.getUuid(), errorCode.getDetails()));
                                trigger.next();
                            }
                        });
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public void failedToDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode) {
        CollectionUtils.safeForEach(detachVolumeExtensions, new ForEachFunction<VmDetachVolumeExtensionPoint>() {
            @Override
            public void run(VmDetachVolumeExtensionPoint arg) {
                arg.failedToDetachVolume(vm, volume, errorCode);
            }
        });
    }

    public void getVmCapabilities(final VmInstanceInventory vm, final VmCapabilities capabilities) {
        CollectionUtils.safeForEach(capabilitiesExtensionPoints, new ForEachFunction<VmCapabilitiesExtensionPoint>() {
            @Override
            public void run(VmCapabilitiesExtensionPoint arg) {
                arg.checkVmCapability(vm, capabilities);
            }
        });
    }

    public void cleanUpAfterVmFailedToStart(final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(cleanUpAfterVmFailedToStartExtensionPoints, arg -> arg.cleanUpAfterVmFailedToStart(vm));
    }

    public void cleanUpAfterVmChangeImage(final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(cleanUpAfterVmChangeImageExtensionPoints, arg -> arg.cleanUpAfterVmChangeImage(vm));
    }

    public void afterChangeVmNicState(final String vmNic, final String state) {
        CollectionUtils.safeForEach(vmNicChangeStateExtensionPoints, arg -> arg.afterChangeVmNicState(vmNic, state));
    }

    private void populateExtensions() {
        VmInstanceBeforeStartExtensions = pluginRgty.getExtensionList(VmInstanceBeforeStartExtensionPoint.class);
        VmInstanceResumeExtensionPoints = pluginRgty.getExtensionList(VmInstanceResumeExtensionPoint.class);
        startNewCreatedVmExtensions = pluginRgty.getExtensionList(VmInstanceStartNewCreatedVmExtensionPoint.class);
        beforeVmStopExtensions = pluginRgty.getExtensionList(BeforeVmInstanceStopExtensionPoint.class);
        stopVmExtensions = pluginRgty.getExtensionList(VmInstanceStopExtensionPoint.class);
        rebootVmExtensions = pluginRgty.getExtensionList(VmInstanceRebootExtensionPoint.class);
        destroyVmExtensions = pluginRgty.getExtensionList(VmInstanceDestroyExtensionPoint.class);
        startVmExtensions = pluginRgty.getExtensionList(VmInstanceStartExtensionPoint.class);
        migrateVmExtensions = pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class);
        attachVolumeExtensions = pluginRgty.getExtensionList(VmAttachVolumeExtensionPoint.class);
        detachVolumeExtensions = pluginRgty.getExtensionList(VmDetachVolumeExtensionPoint.class);
        capabilitiesExtensionPoints = pluginRgty.getExtensionList(VmCapabilitiesExtensionPoint.class);
        cleanUpAfterVmFailedToStartExtensionPoints = pluginRgty.getExtensionList(CleanUpAfterVmFailedToStartExtensionPoint.class);
        cleanUpAfterVmChangeImageExtensionPoints = pluginRgty.getExtensionList(CleanUpAfterVmChangeImageExtensionPoint.class);
        vmNicChangeStateExtensionPoints = pluginRgty.getExtensionList(VmNicChangeStateExtensionPoint.class);
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
