package org.zstack.observability;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.header.AbstractService;
import org.zstack.header.core.execution.APIQueryExecutionMsg;
import org.zstack.header.core.execution.APIQueryExecutionReply;
import org.zstack.header.core.execution.ExecutionInventory;
import org.zstack.header.core.execution.ExecutionObservabilityConstant;
import org.zstack.header.core.execution.ExecutionObservabilityFacade;
import org.zstack.header.core.execution.ExecutionStageInventory;
import org.zstack.header.core.execution.GetLocalExecutionMsg;
import org.zstack.header.core.execution.GetLocalExecutionReply;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_CLOUDBUS_10012;

/**
 * Owns the execution observability API and cluster aggregation. Recording is
 * provided by {@link ExecutionObservabilityFacadeImpl}; this service only
 * handles query requests and the node-local fan-out protocol.
 */
public class ExecutionObservabilityManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ExecutionObservabilityManager.class);
    private static final int NODE_PAGE_SIZE = 200;

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ExecutionObservabilityFacade executionObservability;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIQueryExecutionMsg) {
            handleQueryExecution((APIQueryExecutionMsg) msg);
        } else if (msg instanceof GetLocalExecutionMsg) {
            handle((GetLocalExecutionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(GetLocalExecutionMsg msg) {
        GetLocalExecutionReply reply = new GetLocalExecutionReply();
        if (msg.getQuery() != null) {
            reply.setInventories(executionObservability.queryLocal(msg.getQuery()));
        }
        bus.reply(msg, reply);
    }

    private void handleQueryExecution(APIQueryExecutionMsg msg) {
        if (msg.getLimit() != null && (msg.getLimit() < 1 || msg.getLimit() > 200)) {
            bus.replyErrorByMessageType(msg, Platform.argerr(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    "limit must be between 1 and 200"));
            return;
        }
        Timestamp startedAfter = parseQueryTimestamp(msg.getStartedAfter());
        Timestamp startedBefore = parseQueryTimestamp(msg.getStartedBefore());
        if ((msg.getStartedAfter() != null && startedAfter == null)
                || (msg.getStartedBefore() != null && startedBefore == null)) {
            bus.replyErrorByMessageType(msg, Platform.argerr(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    "startedAfter and startedBefore must be epoch milliseconds or ISO-8601 timestamps"));
            return;
        }
        if (startedAfter != null && startedBefore != null && startedAfter.after(startedBefore)) {
            bus.replyErrorByMessageType(msg, Platform.argerr(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    "startedAfter cannot be later than startedBefore"));
            return;
        }
        int selectors = 0;
        if (msg.getExecutionUuid() != null) selectors++;
        if (msg.getApiUuid() != null) selectors++;
        if (msg.getMessageUuid() != null) selectors++;
        if (msg.getTaskRunUuid() != null) selectors++;
        if (selectors > 1) {
            bus.replyErrorByMessageType(msg, Platform.argerr(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    "only one of executionUuid, apiUuid, messageUuid and taskRunUuid can be specified"));
            return;
        }
        if (selectors == 0 && msg.getTriggerType() == null && msg.getTriggerName() == null
                && msg.getState() == null && msg.getStartedAfter() == null && msg.getStartedBefore() == null
                && msg.getNodeUuid() == null && msg.getMinExecutionDurationMs() == null) {
            bus.replyErrorByMessageType(msg, Platform.argerr(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    "an exact selector or a name, trigger, state, node, duration or time filter is required"));
            return;
        }

        List<ExecutionInventory> result = Collections.synchronizedList(
                new ArrayList<>(queryAllLocalFromNode(msg)));
        Collection<ResourceDestinationMaker.NodeInfo> nodes = destMaker.getAllNodeInfo();
        List<ResourceDestinationMaker.NodeInfo> remotes = nodes.stream()
                .filter(n -> !n.getNodeUuid().equals(Platform.getManagementServerId()))
                .collect(Collectors.toList());

        if (remotes.isEmpty()) {
            finishExecutionQuery(msg, result, false);
            return;
        }

        AtomicInteger pending = new AtomicInteger(remotes.size());
        AtomicBoolean partial = new AtomicBoolean(false);
        for (ResourceDestinationMaker.NodeInfo node : remotes) {
            queryNodeExecutions(msg, node, result, partial, pending, null);
        }
    }

    private List<ExecutionInventory> queryAllLocalFromNode(APIQueryExecutionMsg source) {
        List<ExecutionInventory> result = new ArrayList<>();
        String cursor = null;
        while (true) {
            List<ExecutionInventory> page = executionObservability.queryLocal(queryAllFromNode(source, cursor));
            if (page == null || page.isEmpty()) {
                return result;
            }
            result.addAll(page);
            if (page.size() < NODE_PAGE_SIZE) {
                return result;
            }
            cursor = nextNodeCursor(cursor, page.size());
        }
    }

    private void queryNodeExecutions(APIQueryExecutionMsg source,
                                     ResourceDestinationMaker.NodeInfo node,
                                     List<ExecutionInventory> result,
                                     AtomicBoolean partial,
                                     AtomicInteger pending,
                                     String cursor) {
        GetLocalExecutionMsg local = new GetLocalExecutionMsg();
        local.setQuery(queryAllFromNode(source, cursor));
        bus.makeServiceIdByManagementNodeId(local, ExecutionObservabilityConstant.SERVICE_ID, node.getNodeUuid());
        try {
            bus.send(local, new CloudBusCallBack(source) {
                @Override
                public void run(MessageReply r) {
                    try {
                        if (!r.isSuccess()) {
                            partial.set(true);
                            finishNodeQuery(source, result, partial, pending);
                            return;
                        }

                        List<ExecutionInventory> page = r.<GetLocalExecutionReply>castReply().getInventories();
                        if (page != null) {
                            result.addAll(page);
                        }
                        if (page != null && page.size() == NODE_PAGE_SIZE) {
                            queryNodeExecutions(source, node, result, partial, pending,
                                    nextNodeCursor(cursor, page.size()));
                            return;
                        }
                        finishNodeQuery(source, result, partial, pending);
                    } catch (Throwable t) {
                        logger.warn(String.format("failed to process execution records from management node[%s]",
                                node.getNodeUuid()), t);
                        partial.set(true);
                        finishNodeQuery(source, result, partial, pending);
                    }
                }
            });
        } catch (Throwable t) {
            logger.warn(String.format("failed to query execution records from management node[%s]",
                    node.getNodeUuid()), t);
            partial.set(true);
            finishNodeQuery(source, result, partial, pending);
        }
    }

    private void finishNodeQuery(APIQueryExecutionMsg source,
                                 List<ExecutionInventory> result,
                                 AtomicBoolean partial,
                                 AtomicInteger pending) {
        if (pending.decrementAndGet() == 0) {
            finishExecutionQuery(source, result, partial.get());
        }
    }

    private Timestamp parseQueryTimestamp(String value) {
        return ExecutionTimestampParser.parse(value);
    }

    private static String trimFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private APIQueryExecutionMsg queryAllFromNode(APIQueryExecutionMsg source, String cursor) {
        APIQueryExecutionMsg query = new APIQueryExecutionMsg();
        query.setExecutionUuid(trimFilter(source.getExecutionUuid()));
        query.setApiUuid(trimFilter(source.getApiUuid()));
        query.setMessageUuid(trimFilter(source.getMessageUuid()));
        query.setTaskRunUuid(trimFilter(source.getTaskRunUuid()));
        query.setTriggerType(trimFilter(source.getTriggerType()));
        query.setTriggerName(trimFilter(source.getTriggerName()));
        query.setState(trimFilter(source.getState()));
        query.setStartedAfter(trimFilter(source.getStartedAfter()));
        query.setStartedBefore(trimFilter(source.getStartedBefore()));
        query.setNodeUuid(trimFilter(source.getNodeUuid()));
        query.setMinExecutionDurationMs(source.getMinExecutionDurationMs());
        query.setDetail(source.getDetail());
        query.setLimit(NODE_PAGE_SIZE);
        query.setCursor(cursor);
        return query;
    }

    private String nextNodeCursor(String cursor, int pageSize) {
        int offset = 0;
        if (cursor != null) {
            try {
                offset = Math.max(0, Integer.parseInt(cursor));
            } catch (NumberFormatException ignored) {
                // Start over if an internal cursor is malformed.
            }
        }
        return String.valueOf(offset + pageSize);
    }

    private void finishExecutionQuery(APIQueryExecutionMsg msg, List<ExecutionInventory> inventories, boolean partial) {
        Map<String, ExecutionInventory> unique = new LinkedHashMap<>();
        for (ExecutionInventory inventory : inventories) {
            ExecutionInventory old = unique.get(inventory.getExecutionUuid());
            if (old == null) {
                unique.put(inventory.getExecutionUuid(), inventory);
                continue;
            }
            mergeExecutionInventory(old, inventory);
        }
        inventories = new ArrayList<>(unique.values());
        if (msg.getExecutionUuid() != null && inventories.isEmpty() && !partial) {
            bus.replyErrorByMessageType(msg, Platform.err(ORG_ZSTACK_CORE_CLOUDBUS_10012,
                    SysErrors.RESOURCE_NOT_FOUND, "execution[uuid:%s] not found", msg.getExecutionUuid()));
            return;
        }
        inventories.sort((a, b) -> {
            if (a.getAcceptedAt() == null || b.getAcceptedAt() == null) {
                return 0;
            }
            return b.getAcceptedAt().compareTo(a.getAcceptedAt());
        });

        int offset = 0;
        if (msg.getCursor() != null) {
            try {
                offset = Math.max(0, Integer.parseInt(msg.getCursor()));
            } catch (NumberFormatException ignored) {
                // Invalid cursors are treated as the first page for compatibility.
            }
        }
        int limit = msg.getLimit() == null ? 50 : Math.max(1, Math.min(200, msg.getLimit()));
        int total = inventories.size();
        int from = Math.min(offset, inventories.size());
        int to = Math.min(from + limit, inventories.size());
        List<ExecutionInventory> page = new ArrayList<>(inventories.subList(from, to));
        page.forEach(i -> {
            i.setVisibility("CLUSTER");
            i.setPartial(partial);
            i.setPartialReason(partial ? "one or more management nodes were unavailable" : null);
        });

        APIQueryExecutionReply reply = new APIQueryExecutionReply();
        reply.setInventories(page);
        reply.setTotal(total);
        reply.setNextCursor(to < inventories.size() ? String.valueOf(to) : null);
        bus.reply(msg, reply);
    }

    private void mergeExecutionInventory(ExecutionInventory target, ExecutionInventory source) {
        if (target.getNoReply() == null && source.getNoReply() != null) {
            target.setNoReply(source.getNoReply());
        }

        Set<String> nodes = new LinkedHashSet<>();
        if (target.getSourceNodes() != null) nodes.addAll(target.getSourceNodes());
        if (source.getSourceNodes() != null) nodes.addAll(source.getSourceNodes());
        target.setSourceNodes(new ArrayList<>(nodes));

        if (target.getFinishedAt() == null && source.getFinishedAt() != null) {
            target.setState(source.getState());
            target.setFinishedAt(source.getFinishedAt());
            target.setLastHeartbeatAt(source.getLastHeartbeatAt());
            target.setElapsedMs(source.getElapsedMs());
            target.setExecutionMs(source.getExecutionMs());
            target.setError(source.getError());
        }
        if (target.getEvents() != null && source.getEvents() != null) {
            Set<String> sequences = target.getEvents().stream()
                    .map(e -> eventKey(e.getNodeUuid(), e.getSequence())).collect(Collectors.toSet());
            source.getEvents().forEach(event -> {
                if (event.getSequence() == null || sequences.add(eventKey(event.getNodeUuid(), event.getSequence()))) {
                    target.getEvents().add(event);
                }
            });
            target.getEvents().sort((a, b) -> {
                if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                return a.getTimestamp().compareTo(b.getTimestamp());
            });
        }
        if (source.getActiveStages() != null && !source.getActiveStages().isEmpty()) {
            Map<String, ExecutionStageInventory> stages = new LinkedHashMap<>();
            if (target.getActiveStages() != null) {
                target.getActiveStages().forEach(stage -> stages.put(stage.getStageUuid(), stage));
            }
            source.getActiveStages().forEach(stage -> stages.putIfAbsent(stage.getStageUuid(), stage));
            target.setActiveStages(new ArrayList<>(stages.values()));
        }
    }

    private String eventKey(String nodeUuid, Long sequence) {
        return String.valueOf(nodeUuid) + ":" + String.valueOf(sequence);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ExecutionObservabilityConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
