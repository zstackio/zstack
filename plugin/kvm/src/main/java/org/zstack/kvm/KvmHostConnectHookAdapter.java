package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.plugin.FeaturePlugins;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.plugin.sdk.HostConnectContext;
import org.zstack.header.plugin.sdk.HostConnectHook;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_PLUGIN_SDK_10001;

/**
 * Calls the {@link HostConnectHook}s of feature plugins while a KVM host connects, one after another.
 */
public class KvmHostConnectHookAdapter implements KVMHostConnectExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmHostConnectHookAdapter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private static String pluginIdOf(Object hook) {
        String id = FeaturePlugins.pluginIdOf(hook.getClass());
        return id == null ? hook.getClass().getSimpleName() : id;
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "call-host-connect-hooks-of-feature-plugins";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.getFeaturePlugins() != null) {
                    context.getFeaturePlugins().forEach((id, status) -> {
                        if (!"Loaded".equals(status.get("status"))) {
                            logger.warn(String.format("the kvmagent part of feature plugin[%s] on host[uuid:%s] is %s: %s",
                                    id, context.getInventory().getUuid(), status.get("status"), status.get("error")));
                        }
                    });
                }

                HostConnectContext ctx = new HostConnectContext(context.getInventory(), context.isNewAddedHost(),
                        context.getPluginFacts(), context.getFeaturePlugins());
                List<HostConnectHook> hooks = new ArrayList<>(pluginRgty.getExtensionList(HostConnectHook.class));
                hooks.sort(Comparator.comparing(h -> h.getClass().getName()));
                new While<>(hooks).each((hook, com) -> {
                    Completion completion = new Completion(com) {
                        @Override
                        public void success() {
                            com.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            ErrorCode err = operr(ORG_ZSTACK_PLUGIN_SDK_10001, "feature plugin[%s] failed in %s",
                                    pluginIdOf(hook), "HostConnectHook");
                            err.setCause(errorCode);
                            com.addError(err);
                            com.allDone();
                        }
                    };
                    try {
                        hook.onConnecting(ctx, completion);
                    } catch (Throwable t) {
                        logger.warn(String.format("HostConnectHook of feature plugin[%s] failed", pluginIdOf(hook)), t);
                        completion.fail(operr(ORG_ZSTACK_PLUGIN_SDK_10001, "%s", t.toString()));
                    }
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        };
    }
}
