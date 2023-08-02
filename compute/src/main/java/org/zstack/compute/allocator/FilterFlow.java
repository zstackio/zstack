package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionUtils.*;

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
            throw new CloudRuntimeException("FilterFlow cannot be the first flow in the host allocator chains");
        }

        for (HostAllocatorFilterExtensionPoint filter : pluginRgty.getExtensionList(HostAllocatorFilterExtensionPoint.class)) {
            final String filterName = filter.getClass().getSimpleName();
            logger.debug(String.format("before being filtered by HostAllocatorFilterExtensionPoint[%s], candidates remain %d",
                    filterName, candidates.size()));

            ErrorableValue<List<HostVO>> result = filter.filterHostCandidates(candidates, spec);
            if (!result.isSuccess()) {
                fail(Platform.err(HostAllocatorError.NO_AVAILABLE_HOST, result.error,
                        "after filtering, HostAllocatorFilterExtensionPoint[%s] returns zero candidate host",
                        filterName));
                return;
            }

            candidates = result.result;
            if (isEmpty(candidates)) {
                fail(Platform.err(HostAllocatorError.NO_AVAILABLE_HOST,
                        "after filtering, HostAllocatorFilterExtensionPoint[%s] returns zero candidate host",
                        filterName));
                return;
            }
            logger.debug(String.format("after being filtered by HostAllocatorFilterExtensionPoint[%s], candidates remain %d",
                    filter.getClass(), candidates.size()));
        }

        next(candidates);
    }
}
