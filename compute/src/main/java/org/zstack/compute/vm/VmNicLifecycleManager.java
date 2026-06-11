package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class VmNicLifecycleManager implements
        PreVmInstantiateResourceExtensionPoint,
        VmReleaseResourceExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        InstantiateResourceOnAttachingNicExtensionPoint,
        ReleaseNetworkServiceOnDetachingNicExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VmNicLifecycleManager.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<VmNicLifecycleExtensionPoint> getExtensions() {
        return pluginRgty.getExtensionList(VmNicLifecycleExtensionPoint.class);
    }

    // ===================== NIC filtering =====================

    private List<VmNicInventory> filterNics(VmNicLifecycleExtensionPoint ext,
                                            List<VmNicInventory> allNics) {
        List<VmNicInventory> matched = new ArrayList<>();
        for (VmNicInventory nic : allNics) {
            try {
                if (ext.isApplicable(nic)) {
                    matched.add(nic);
                }
            } catch (Exception e) {
                logger.error(String.format("[VmNicLifecycle] %s.isApplicable(nic=%s) threw exception",
                        ext.getClass().getSimpleName(), nic.getUuid()), e);
                throw new RuntimeException(String.format("%s.isApplicable failed for nic[uuid:%s]",
                        ext.getClass().getSimpleName(), nic.getUuid()), e);
            }
        }
        return matched;
    }

    // ===================== fail-fast serial runner (setup / preMigrate) =====================

    private void runSetup(VmNicLifecycleContext context, Iterator<VmNicLifecycleExtensionPoint> it,
                          String hostUuid, List<VmNicInventory> allNics,
                          Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        VmNicLifecycleExtensionPoint ext = it.next();
        List<VmNicInventory> nics;
        try {
            nics = filterNics(ext, allNics);
        } catch (RuntimeException e) {
            completion.fail(Platform.operr(ORG_ZSTACK_COMPUTE_VM_10331,
                    "vm nic lifecycle setup failed while selecting applicable NICs: %s", e.getMessage()));
            return;
        }
        if (nics.isEmpty()) {
            runSetup(context, it, hostUuid, allNics, completion);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            ext.setupOnHost(context, hostUuid, nics, new Completion(completion) {
                @Override
                public void success() {
                    logger.debug(String.format("[VmNicLifecycle] %s.setupOnHost(host=%s, nics=%d) " +
                                    "completed in %dms", ext.getClass().getSimpleName(), hostUuid,
                            nics.size(), System.currentTimeMillis() - start));
                    runSetup(context, it, hostUuid, allNics, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("[VmNicLifecycle] %s.setupOnHost(host=%s) failed: %s",
                            ext.getClass().getSimpleName(), hostUuid, errorCode));
                    completion.fail(errorCode);
                }
            });
        } catch (Throwable t) {
            completion.fail(Platform.operr(ORG_ZSTACK_COMPUTE_VM_10332,
                    "%s.setupOnHost(host=%s) threw exception: %s",
                    ext.getClass().getSimpleName(), hostUuid, t.getMessage()));
        }
    }

    private void runPreMigrate(Iterator<VmNicLifecycleExtensionPoint> it,
                               String srcHostUuid, String destHostUuid,
                               List<VmNicInventory> allNics, Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        VmNicLifecycleExtensionPoint ext = it.next();
        List<VmNicInventory> nics;
        try {
            nics = filterNics(ext, allNics);
        } catch (RuntimeException e) {
            completion.fail(Platform.operr(ORG_ZSTACK_COMPUTE_VM_10333,
                    "vm nic lifecycle pre-migration failed while selecting applicable NICs: %s", e.getMessage()));
            return;
        }
        if (nics.isEmpty()) {
            runPreMigrate(it, srcHostUuid, destHostUuid, allNics, completion);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            ext.preMigrate(srcHostUuid, destHostUuid, nics, new Completion(completion) {
                @Override
                public void success() {
                    logger.debug(String.format("[VmNicLifecycle] %s.preMigrate(src=%s, dest=%s, nics=%d) " +
                                    "completed in %dms", ext.getClass().getSimpleName(),
                            srcHostUuid, destHostUuid, nics.size(), System.currentTimeMillis() - start));
                    runPreMigrate(it, srcHostUuid, destHostUuid, allNics, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("[VmNicLifecycle] %s.preMigrate(src=%s, dest=%s) failed: %s",
                            ext.getClass().getSimpleName(), srcHostUuid, destHostUuid, errorCode));
                    completion.fail(errorCode);
                }
            });
        } catch (Throwable t) {
            completion.fail(Platform.operr(ORG_ZSTACK_COMPUTE_VM_10334,
                    "%s.preMigrate(src=%s, dest=%s) threw exception: %s",
                    ext.getClass().getSimpleName(), srcHostUuid, destHostUuid, t.getMessage()));
        }
    }

    // ===================== fail-logged serial runner (cleanup / postMigrate / failedMigrate) =====================

    @FunctionalInterface
    private interface CleanupAction {
        void execute(VmNicLifecycleExtensionPoint ext, List<VmNicInventory> nics,
                     NoErrorCompletion completion);
    }

    private void runFailLogged(Iterator<VmNicLifecycleExtensionPoint> it,
                               List<VmNicInventory> allNics,
                               String methodName, CleanupAction action,
                               NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        VmNicLifecycleExtensionPoint ext = it.next();
        List<VmNicInventory> nics;
        try {
            nics = filterNics(ext, allNics);
        } catch (RuntimeException e) {
            logger.warn(String.format("[VmNicLifecycle] %s.isApplicable threw exception during %s",
                    ext.getClass().getSimpleName(), methodName), e);
            runFailLogged(it, allNics, methodName, action, completion);
            return;
        }
        if (nics.isEmpty()) {
            runFailLogged(it, allNics, methodName, action, completion);
            return;
        }

        try {
            action.execute(ext, nics, new NoErrorCompletion() {
                @Override
                public void done() {
                    runFailLogged(it, allNics, methodName, action, completion);
                }
            });
        } catch (Throwable t) {
            logger.warn(String.format("[VmNicLifecycle] %s.%s threw exception",
                    ext.getClass().getSimpleName(), methodName), t);
            runFailLogged(it, allNics, methodName, action, completion);
        }
    }

    private void runCleanup(Iterator<VmNicLifecycleExtensionPoint> it,
                            String hostUuid, List<VmNicInventory> allNics,
                            NoErrorCompletion completion) {
        runFailLogged(it, allNics, "cleanupFromHost",
                (ext, nics, done) -> ext.cleanupFromHost(hostUuid, nics, done), completion);
    }

    private void runCleanupStale(Iterator<VmNicLifecycleExtensionPoint> it,
                                 String lastHostUuid, List<VmNicInventory> allNics,
                                 NoErrorCompletion completion) {
        runFailLogged(it, allNics, "cleanupStaleResource",
                (ext, nics, done) -> ext.cleanupStaleResource(lastHostUuid, nics, done), completion);
    }

    private void runPostMigrate(Iterator<VmNicLifecycleExtensionPoint> it,
                                String srcHostUuid, String destHostUuid,
                                List<VmNicInventory> allNics, NoErrorCompletion completion) {
        runFailLogged(it, allNics, "postMigrate",
                (ext, nics, done) -> ext.postMigrate(srcHostUuid, destHostUuid, nics, done),
                completion);
    }

    private void runFailedMigrate(Iterator<VmNicLifecycleExtensionPoint> it,
                                  String srcHostUuid, String destHostUuid,
                                  List<VmNicInventory> allNics, NoErrorCompletion completion) {
        runFailLogged(it, allNics, "failedMigrate",
                (ext, nics, done) -> ext.failedMigrate(srcHostUuid, destHostUuid, nics, done),
                completion);
    }

    // ===================== PreVmInstantiateResourceExtensionPoint =====================

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec)
            throws VmInstantiateResourceException {
        // sync hook — no resource operations
    }

    private VmNicLifecycleContext buildContext(VmInstanceSpec spec, String destHostUuid) {
        VmNicLifecycleContext ctx = new VmNicLifecycleContext();
        ctx.setOperation(spec.getCurrentVmOperation());
        ctx.setDestHostUuid(destHostUuid);
        if (spec.getVmInventory() != null) {
            ctx.setVmUuid(spec.getVmInventory().getUuid());
            ctx.setSrcHostUuid(spec.getVmInventory().getHostUuid());
            ctx.setLastHostUuid(spec.getVmInventory().getLastHostUuid());
        }
        return ctx;
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        List<VmNicInventory> allNics = spec.getDestNics();
        if (allNics == null || allNics.isEmpty() || getExtensions().isEmpty()) {
            completion.success();
            return;
        }

        if (spec.getDestHost() == null) {
            completion.success();
            return;
        }
        String destHostUuid = spec.getDestHost().getUuid();
        String lastHostUuid = spec.getVmInventory().getLastHostUuid();
        VmInstanceConstant.VmOperation op = spec.getCurrentVmOperation();
        VmNicLifecycleContext ctx = buildContext(spec, destHostUuid);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("vmnic-lifecycle-pre-instantiate-" + spec.getVmInventory().getUuid());

        if (lastHostUuid != null && !lastHostUuid.equals(destHostUuid)
                && op == VmInstanceConstant.VmOperation.Start) {
            chain.then(new NoRollbackFlow() {
                String __name__ = "cleanup-stale-resource-from-last-host";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    runCleanupStale(getExtensions().iterator(), lastHostUuid, allNics,
                            new NoErrorCompletion() {
                                @Override
                                public void done() {
                                    trigger.next();
                                }
                            });
                }
            });
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "setup-on-dest-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                runSetup(ctx, getExtensions().iterator(), destHostUuid, allNics, new Completion(trigger) {
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

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        doCleanup(spec, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    // ===================== VmReleaseResourceExtensionPoint =====================

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Reboot) {
            completion.success();
            return;
        }
        doCleanup(spec, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    private void doCleanup(VmInstanceSpec spec, NoErrorCompletion completion) {
        List<VmNicInventory> allNics = spec.getDestNics();
        if (allNics == null || allNics.isEmpty() || getExtensions().isEmpty()) {
            completion.done();
            return;
        }
        if (spec.getDestHost() == null) {
            completion.done();
            return;
        }
        String hostUuid = spec.getDestHost().getUuid();
        runCleanup(getExtensions().iterator(), hostUuid, allNics, completion);
    }

    // ===================== VmInstanceMigrateExtensionPoint =====================

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        // no-op — sync validation hook
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid,
                              Completion completion) {
        if (getExtensions().isEmpty()) {
            completion.success();
            return;
        }
        String srcHostUuid = inv.getHostUuid();
        List<VmNicInventory> allNics = inv.getVmNics();
        if (allNics == null || allNics.isEmpty()) {
            completion.success();
            return;
        }
        runPreMigrate(getExtensions().iterator(), srcHostUuid, destHostUuid, allNics, completion);
    }

    @Override
    public void postMigrateVm(VmInstanceInventory inv, String destHostUuid,
                               Completion completion) {
        if (getExtensions().isEmpty()) {
            completion.success();
            return;
        }
        String srcHostUuid = inv.getHostUuid();
        List<VmNicInventory> allNics = inv.getVmNics();
        if (allNics == null || allNics.isEmpty()) {
            completion.success();
            return;
        }
        runPostMigrate(getExtensions().iterator(), srcHostUuid, destHostUuid, allNics,
                new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.success();
                    }
                });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid,
                                NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid,
                                   ErrorCode reason, NoErrorCompletion completion) {
        if (getExtensions().isEmpty()) {
            completion.done();
            return;
        }
        String srcHostUuid = inv.getHostUuid();
        List<VmNicInventory> allNics = inv.getVmNics();
        if (allNics == null || allNics.isEmpty()) {
            completion.done();
            return;
        }
        runFailedMigrate(getExtensions().iterator(), srcHostUuid, destHostUuid, allNics, completion);
    }

    // ===================== InstantiateResourceOnAttachingNicExtensionPoint =====================

    @Override
    public void instantiateResourceOnAttachingNic(VmInstanceSpec spec,
                                                   L3NetworkInventory l3, Completion completion) {
        VmInstanceInventory vm = spec.getVmInventory();
        if (!VmInstanceState.Running.toString().equals(vm.getState()) || getExtensions().isEmpty()) {
            completion.success();
            return;
        }

        List<VmNicInventory> newNics = spec.getDestNics().stream()
                .filter(nic -> nic.getL3NetworkUuid().equals(l3.getUuid()))
                .collect(Collectors.toList());
        if (newNics.isEmpty()) {
            completion.success();
            return;
        }

        String hostUuid = vm.getHostUuid();
        VmNicLifecycleContext ctx = buildContext(spec, hostUuid);
        ctx.setOperation(VmInstanceConstant.VmOperation.AttachNic);
        runSetup(ctx, getExtensions().iterator(), hostUuid, newNics, completion);
    }

    @Override
    public void releaseResourceOnAttachingNic(VmInstanceSpec spec,
                                               L3NetworkInventory l3, NoErrorCompletion completion) {
        doCleanupForNic(spec, l3, completion);
    }

    private void doCleanupForNic(VmInstanceSpec spec, L3NetworkInventory l3,
                                  NoErrorCompletion completion) {
        if (getExtensions().isEmpty()) {
            completion.done();
            return;
        }
        String hostUuid = spec.getVmInventory().getHostUuid();
        if (hostUuid == null) {
            completion.done();
            return;
        }
        List<VmNicInventory> nics = spec.getDestNics().stream()
                .filter(nic -> nic.getL3NetworkUuid().equals(l3.getUuid()))
                .collect(Collectors.toList());
        runCleanup(getExtensions().iterator(), hostUuid, nics, completion);
    }

    // ===================== ReleaseNetworkServiceOnDetachingNicExtensionPoint =====================

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec,
                                               VmNicInventory nic, NoErrorCompletion completion) {
        if (getExtensions().isEmpty()) {
            completion.done();
            return;
        }
        String hostUuid = spec.getVmInventory().getHostUuid();
        if (hostUuid == null) {
            completion.done();
            return;
        }
        runCleanup(getExtensions().iterator(), hostUuid, Collections.singletonList(nic), completion);
    }
}
