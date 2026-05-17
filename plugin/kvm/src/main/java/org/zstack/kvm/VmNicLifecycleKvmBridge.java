package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmNicLifecycleGlobalConfig;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmNicLifecycleExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class VmNicLifecycleKvmBridge implements KVMPingAgentNoFailureExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VmNicLifecycleKvmBridge.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

    private List<VmNicLifecycleExtensionPoint> getExtensions() {
        return pluginRgty.getExtensionList(VmNicLifecycleExtensionPoint.class);
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        List<VmNicLifecycleExtensionPoint> extensions = getExtensions();
        if (extensions.isEmpty()) {
            completion.done();
            return;
        }

        String hostUuid = host.getUuid();

        List<VmNicInventory> allExpectedNics;
        try {
            List<VmInstanceVO> runningVms = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.hostUuid, hostUuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Running)
                    .list();
            allExpectedNics = runningVms.stream()
                    .flatMap(vm -> VmNicInventory.valueOf(vm.getVmNics()).stream())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn(String.format("[VmNicLifecycle] failed to query Running VMs " +
                    "on host[uuid:%s] for reconciliation, skip this round", hostUuid), e);
            completion.done();
            return;
        }

        long timeoutSeconds = VmNicLifecycleGlobalConfig.RECONCILE_TIMEOUT.value(Long.class);

        new While<>(extensions).step((ext, whileCompletion) -> {
            List<VmNicInventory> matchedNics;
            try {
                matchedNics = allExpectedNics.stream()
                        .filter(ext::isApplicable)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn(String.format("[VmNicLifecycle] %s.isApplicable threw exception " +
                                "during reconciliation on host[uuid:%s]",
                        ext.getClass().getSimpleName(), hostUuid), e);
                whileCompletion.done();
                return;
            }

            AtomicBoolean completed = new AtomicBoolean(false);

            ThreadFacadeImpl.TimeoutTaskReceipt receipt = thdf.submitTimeoutTask(() -> {
                if (completed.compareAndSet(false, true)) {
                    logger.warn(String.format("[VmNicLifecycle] %s.reconcileOnHost timed out " +
                                    "after %ds on host[uuid:%s]",
                            ext.getClass().getSimpleName(), timeoutSeconds, hostUuid));
                    whileCompletion.done();
                }
            }, TimeUnit.SECONDS, timeoutSeconds);

            try {
                ext.reconcileOnHost(hostUuid, matchedNics, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (completed.compareAndSet(false, true)) {
                            receipt.cancel();
                            whileCompletion.done();
                        }
                    }
                });
            } catch (Exception e) {
                if (completed.compareAndSet(false, true)) {
                    receipt.cancel();
                    logger.warn(String.format("[VmNicLifecycle] %s.reconcileOnHost(host=%s) " +
                            "threw exception", ext.getClass().getSimpleName(), hostUuid), e);
                    whileCompletion.done();
                }
            }
        }, extensions.size()).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }
}
