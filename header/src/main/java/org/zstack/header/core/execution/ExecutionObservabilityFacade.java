package org.zstack.header.core.execution;

import java.util.List;

/**
 * System-level execution observation query facade.
 *
 * <p>Integration hooks are exposed through narrow observer interfaces so that
 * CloudBus, REST, and thread adapters only depend on the lifecycle they report.</p>
 */
public interface ExecutionObservabilityFacade {
    /**
     * Query executions recorded on the current management node.
     *
     * @param query filter, paging and detail options; {@code null} returns no results
     * @return the matching node-local execution inventories
     */
    List<ExecutionInventory> queryLocal(APIQueryExecutionMsg query);
}
