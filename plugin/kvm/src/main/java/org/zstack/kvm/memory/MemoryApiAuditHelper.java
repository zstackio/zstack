package org.zstack.kvm.memory;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vm.VmInstanceVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Resource association for memory API audits. Audit records describe API/task
 * acceptance; the task inventory retains the independent execution outcome. */
final class MemoryApiAuditHelper {
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{32}");

    private MemoryApiAuditHelper() { }

    static List<APIAuditor.Result> update(APIUpdateMemoryPolicyMsg msg, APIEvent response) {
        List<APIAuditor.Result> results = new ArrayList<>();
        Class<?> type = resourceType(msg.getScope());
        if (response == null || !response.isSuccess()) {
            // Preserve rejection evidence without turning an untrusted or
            // cross-account requested UUID into a resource association.
            return unassociated();
        }
        if (type != null && validUuid(msg.getResourceUuid())) {
            results.add(new APIAuditor.Result(msg.getResourceUuid(), type));
            return results;
        }
        if (!"Global".equals(msg.getScope())) {
            // Keep invalid/rejected attempts visible without attributing an
            // unknown scope to a real Host/Cluster/VM.
            return unassociated();
        }

        // Global is a logical policy, not a Host. Associate only explicit
        // target Hosts after a matching accepted task; response dumps retain
        // task UUID/action/status without inventing a resource type for tasks.
        MemoryTaskInventory task = updateTask(response);
        if ("stageTargetShard".equals(msg.getAction()) ||
                "cancelTargetShards".equals(msg.getAction())) {
            return unassociated();
        }
        if (!matchesAcceptedGlobalTask(msg, response, task)) {
            return unassociated();
        }
        if ("commitTargetShards".equals(msg.getAction()) ||
                msg.getTargetVmUuids() != null) {
            // Staging/commit payloads are snapshots rather than a server-echoed
            // resource set. Do not turn caller-supplied shard members into audit
            // resource associations here.
            return unassociated();
        }
        List<String> targets = msg.getTargetHostUuids();
        Class<?> targetType = HostVO.class;
        Set<String> trustedTargets = uniqueValidTargets(targets);
        for (String target : trustedTargets) {
            results.add(new APIAuditor.Result(target, targetType));
        }
        if (trustedTargets.isEmpty()) { return unassociated(); }
        return results;
    }

    static List<APIAuditor.Result> cancel(APICancelMemoryTaskMsg msg, APIEvent response) {
        List<APIAuditor.Result> results = new ArrayList<>();
        MemoryTaskInventory task = response instanceof APICancelMemoryTaskEvent
                ? ((APICancelMemoryTaskEvent) response).getInventory() : null;
        boolean matchingSuccess = response != null && response.isSuccess() && task != null
                && msg.getUuid() != null && msg.getUuid().equals(task.getUuid());
        if (!matchingSuccess) {
            // The request/response dumps keep the requested task UUID and
            // rejection; no resource can be resolved safely without manager
            // cooperation, so do not type the task UUID as a Host/resource.
            return unassociated();
        }
        Class<?> type = resourceType(task.getScope());
        if (type != null && validUuid(task.getResourceUuid())) {
            results.add(new APIAuditor.Result(task.getResourceUuid(), type));
        } else {
            results.addAll(unassociated());
        }
        return results;
    }

    static List<APIAuditor.Result> delete(APIDeleteMemoryTaskMsg msg, APIEvent response) {
        List<APIAuditor.Result> results = new ArrayList<>();
        if (!(response instanceof APIDeleteMemoryTaskEvent) || !response.isSuccess()) { return unassociated(); }
        APIDeleteMemoryTaskEvent event = (APIDeleteMemoryTaskEvent) response;
        if (!event.isDeleted() || !validUuid(msg.getUuid()) || !msg.getUuid().equals(event.getTaskUuid())) {
            return unassociated();
        }
        Class<?> type = resourceType(event.getScope());
        if (type != null && validUuid(event.getResourceUuid())) {
            results.add(new APIAuditor.Result(event.getResourceUuid(), type));
            return results;
        }
        if ("Global".equals(event.getScope())) {
            for (String host : uniqueValidTargets(event.getHostUuids())) {
                results.add(new APIAuditor.Result(host, HostVO.class));
            }
            if (!results.isEmpty()) { return results; }
        }
        return unassociated();
    }

    private static boolean matchesAcceptedGlobalTask(APIUpdateMemoryPolicyMsg msg,
                                                       APIEvent response,
                                                       MemoryTaskInventory task) {
        return response != null && response.isSuccess() && task != null
                && "Global".equals(task.getScope())
                && "global".equals(task.getResourceUuid())
                && "global".equals(msg.getResourceUuid())
                && (msg.getAction() != null && msg.getAction().equals(task.getAction()) ||
                    "commitTargetShards".equals(msg.getAction()) && "apply".equals(task.getAction()))
                && validUuid(task.getUuid());
    }

    private static MemoryTaskInventory updateTask(APIEvent response) {
        return response instanceof APIUpdateMemoryPolicyEvent
                ? ((APIUpdateMemoryPolicyEvent) response).getInventory() : null;
    }

    private static Set<String> uniqueValidTargets(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String target : targets) {
            if (!validUuid(target) || !result.add(target)) {
                return Collections.emptySet();
            }
        }
        return result;
    }

    private static List<APIAuditor.Result> unassociated() {
        return Collections.singletonList(new APIAuditor.Result("", ResourceVO.class));
    }

    private static Class<?> resourceType(String scope) {
        if ("Host".equals(scope)) { return HostVO.class; }
        if ("Cluster".equals(scope)) { return ClusterVO.class; }
        if ("VM".equals(scope)) { return VmInstanceVO.class; }
        return null;
    }

    private static boolean validUuid(String value) {
        return value != null && UUID.matcher(value).matches();
    }
}
