package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 7/2/2015.
 */
public class FilterFlow extends AbstractHostAllocatorFlow {
    private final CLogger logger = Utils.getLogger(FilterFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        for (HostAllocatorFilterExtensionPoint filter : pluginRgty.getExtensionList(HostAllocatorFilterExtensionPoint.class)) {
            logger.debug(String.format("before being filtered by HostAllocatorFilterExtensionPoint[%s], candidates num: %s", filter.getClass(), candidates.size()));
            try {
                candidates = filter.filterHostCandidates(candidates, spec);
            } catch (OperationFailureException e) {
                fail(e.getErrorCode());
                return;
            }
            logger.debug(String.format("after being filtered by HostAllocatorFilterExtensionPoint[%s], candidates num: %s", filter.getClass(), candidates.size()));

            if (candidates.isEmpty()) {
                fail(Platform.operr("after filtering, HostAllocatorFilterExtensionPoint[%s] returns zero candidate host, it means: %s", filter.getClass().getSimpleName(), filter.filterErrorReason()));
            }
        }

        next(candidates);
    }
}
