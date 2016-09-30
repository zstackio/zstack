package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.exception.CloudRuntimeException;
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
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException(String.format("FilterFlow cannot be the first flow in the host allocator chains"));
        }

        for (HostAllocatorFilterExtensionPoint filter : pluginRgty.getExtensionList(HostAllocatorFilterExtensionPoint.class)) {
            logger.debug(String.format("before being filtered by HostAllocatorFilterExtensionPoint[%s], candidates num: %s", filter.getClass(), candidates.size()));
            candidates = filter.filterHostCandidates(candidates, spec);
            logger.debug(String.format("after being filtered by HostAllocatorFilterExtensionPoint[%s], candidates num: %s", filter.getClass(), candidates.size()));

            if (candidates.isEmpty()) {
                fail(String.format("after filtering, HostAllocatorFilterExtensionPoint[%s] returns zero candidate host", filter.getClass()));
            }
        }

        next(candidates);
    }
}
