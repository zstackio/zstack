package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2NetworkDeleteExtensionPoint;
import org.zstack.header.network.l2.L2NetworkException;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkUpdateExtensionPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.inerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_L2_10024;

public class L2NetworkExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(L2NetworkExtensionPointEmitter.class);
    private List<L2NetworkDeleteExtensionPoint> extensions = null;

    private List<L2NetworkUpdateExtensionPoint> updateExtensions = null;

    @Autowired
    private PluginRegistry pluginRgty;

    public void preDelete(L2NetworkInventory inv) throws L2NetworkException {
        for (L2NetworkDeleteExtensionPoint ext : extensions) {
            try {
                ext.preDeleteL2Network(inv);
            } catch (L2NetworkException l2e) {
                throw l2e;
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception happened while calling L2NetworkDeleteExtensionPoint.preDeleteL2Network of %s", ext.getClass().getCanonicalName()), e);
            }
        }
    }
    
    public void beforeDelete(final L2NetworkInventory inv) {
        CollectionUtils.safeForEach(extensions, new ForEachFunction<L2NetworkDeleteExtensionPoint>() {
            @Override
            public void run(L2NetworkDeleteExtensionPoint arg) {
                arg.beforeDeleteL2Network(inv);
            }
        });
    }
    
    public void afterDelete(final L2NetworkInventory inv) {
        CollectionUtils.safeForEach(extensions, new ForEachFunction<L2NetworkDeleteExtensionPoint>() {
            @Override
            public void run(L2NetworkDeleteExtensionPoint arg) {
                arg.afterDeleteL2Network(inv);
            }
        });
    }

    public void beforeUpdate(final L2NetworkInventory inv) {
        for (L2NetworkUpdateExtensionPoint ext : updateExtensions) {
            try {
                ext.beforeChangeL2NetworkVlanId(inv);
            } catch (RuntimeException e) {
                // propagate validation failures and other runtime exceptions immediately
                throw e;
            } catch (Exception e) {
                logger.warn(String.format("unhandled exception in L2NetworkUpdateExtensionPoint.beforeChangeL2NetworkVlanId of %s",
                        ext.getClass().getCanonicalName()), e);
            }
        }
    }

    public void beforeUpdate(final L2NetworkInventory inv, Completion completion) {
        beforeUpdate(inv, 0, completion);
    }

    private void beforeUpdate(final L2NetworkInventory inv, int index, Completion completion) {
        if (index >= updateExtensions.size()) {
            completion.success();
            return;
        }
        L2NetworkUpdateExtensionPoint extension = updateExtensions.get(index);
        try {
            extension.beforeChangeL2NetworkVlanId(inv, new Completion(completion) {
                @Override
                public void success() {
                    beforeUpdate(inv, index + 1, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            logger.warn(String.format("exception in L2NetworkUpdateExtensionPoint.beforeChangeL2NetworkVlanId of %s",
                    extension.getClass().getCanonicalName()), e);
            completion.fail(inerr(ORG_ZSTACK_NETWORK_L2_10024,
                    "l2 network update extension[%s] failed before changing vlan id: %s",
                    extension.getClass().getCanonicalName(), e.getMessage()));
        }
    }

    public void afterUpdate(final L2NetworkInventory inv) {
        CollectionUtils.safeForEach(updateExtensions, arg -> arg.afterChangeL2NetworkVlanId(inv));
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        extensions        = pluginRgty.getExtensionList(L2NetworkDeleteExtensionPoint.class);
        updateExtensions  = pluginRgty.getExtensionList(L2NetworkUpdateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
