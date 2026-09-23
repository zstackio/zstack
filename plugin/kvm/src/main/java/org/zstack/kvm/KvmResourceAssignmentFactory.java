package org.zstack.kvm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Query;
import javax.persistence.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostAO_;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.physicalserver.*;
import org.zstack.header.tag.AbstractSystemTagLifeCycleListener;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.physicalserver.PhysicalServerConstant;
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10000;

public class KvmResourceAssignmentFactory implements PhysicalServerResourceAssignmentFactory,
        HostAfterConnectedExtensionPoint, HostDeleteExtensionPoint, ManagementNodeReadyExtensionPoint, Component {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("COMPUTE");
    public static final String APPLY_RESOURCE_CONTROL_PATH = "/host/resourcecontrol/apply";
    public static final String RELEASE_RESOURCE_CONTROL_PATH = "/host/resourcecontrol/release";
    public static final String GET_MANAGED_SERVICE_USAGE_PATH = "/host/resourcecontrol/services";
    public static final String RESTART_MANAGED_SERVICES_PATH = "/host/resourcecontrol/restart";
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/compute.yaml";
    private static final int HOST_LINK_UPDATE_BATCH_SIZE = 500;
    private static final CLogger logger = Utils.getLogger(KvmResourceAssignmentFactory.class);

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Override
    public PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid) {
        return new KvmResourceAssignmentController(serverUuid);
    }

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (physicalServerManager == null || host.getServerUuid() == null) {
            return;
        }
        refreshResourceAssignment(host.getServerUuid());
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        if (inventory.getServerUuid() == null) {
            return;
        }
        String serverUuid = inventory.getServerUuid();
        org.zstack.core.db.SQL.New(HostEO.class)
                .eq(HostAO_.uuid, inventory.getUuid())
                .eq(HostAO_.serverUuid, serverUuid).set(HostAO_.serverUuid, null).update();
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
    }

    @Override
    public void managementNodeReady() {
        backfill();
        refreshHostAssignments();
    }

    @Override
    public boolean start() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.installUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                backfill();
                refreshHostAssignments();
            }
        });
        HostSystemTags.SYSTEM_SERIAL_NUMBER.installLifeCycleListener(new AbstractSystemTagLifeCycleListener() {
                    @Override
                    public void tagCreated(SystemTagInventory tag) {
                        backfill(Collections.singleton(tag.getResourceUuid()));
                    }

                    @Override
                    public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                        backfill(Collections.singleton(newTag.getResourceUuid()));
                    }
                });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Transactional
    private void backfill() {
        backfill(Collections.emptySet());
    }

    @Transactional
    private void backfill(Collection<String> hostUuids) {
        if (physicalServerManager == null
                || !PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)) {
            return;
        }
        Q hostQuery = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).isNull(HostVO_.serverUuid);
        if (hostUuids != null && !hostUuids.isEmpty()) {
            hostQuery.in(HostVO_.uuid, hostUuids);
        }
        List<String> unresolvedHostUuids = hostQuery.listValues();
        if (unresolvedHostUuids.isEmpty()) {
            return;
        }
        Map<String, Set<String>> serialsByHost = serialsByHost(unresolvedHostUuids);
        Map<String, List<String>> candidatesBySerial = new LinkedHashMap<>();
        for (String hostUuid : unresolvedHostUuids) {
            Set<String> serials = serialsByHost.get(hostUuid);
            if (serials == null || serials.size() != 1) {
                continue;
            }
            candidatesBySerial.computeIfAbsent(serials.iterator().next(), ignored -> new ArrayList<>()).add(hostUuid);
        }

        Map<String, String> candidates = new LinkedHashMap<>();
        Set<String> serialNumbers = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> candidate : candidatesBySerial.entrySet()) {
            if (candidate.getValue().size() != 1) {
                logger.warn(String.format("cannot backfill host physical server association for serialNumber[%s] " +
                                "because it matches multiple hosts", candidate.getKey()));
                continue;
            }
            String host = candidate.getValue().get(0);
            candidates.put(candidate.getKey(), host);
            serialNumbers.add(candidate.getKey());
        }
        Map<String, String> resolved = physicalServerManager.resolveBySerialNumbers(serialNumbers);
        clearDeletedHostLinks(resolved.values());
        Set<String> used = new HashSet<>(Q.New(HostVO.class)
                .select(HostVO_.serverUuid).notNull(HostVO_.serverUuid).listValues());
        Map<String, String> links = new LinkedHashMap<>();
        for (Map.Entry<String, String> candidate : candidates.entrySet()) {
            String serverUuid = resolved.get(candidate.getKey());
            if (serverUuid != null && used.add(serverUuid)) {
                links.put(candidate.getValue(), serverUuid);
            }
        }
        updateHostLinks(links);

        Set<String> linkedServers = new LinkedHashSet<>(links.values());
        for (String serverUuid : linkedServers) {
            refreshResourceAssignment(serverUuid);
        }
    }

    private void refreshResourceAssignment(String serverUuid) {
        physicalServerManager.refreshResourceAssignment(serverUuid, type.toString(), new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to refresh COMPUTE resource assignment for physical server[uuid:%s]: %s",
                        serverUuid, errorCode));
            }
        });
    }

    private void refreshHostAssignments() {
        if (physicalServerManager == null || !PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class)
                || !destinationMaker.isManagedByUs(PhysicalServerConstant.CONTROL_OWNER_KEY)) {
            return;
        }
        List<String> serverUuids = Q.New(HostVO.class).select(HostVO_.serverUuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).notNull(HostVO_.serverUuid).listValues();
        serverUuids.forEach(this::refreshResourceAssignment);
    }

    private Map<String, Set<String>> serialsByHost(Collection<String> hostUuids) {
        List<Tuple> tagTuples = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuids)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(
                        HostSystemTags.SYSTEM_SERIAL_NUMBER.getTagFormat())).listTuple();
        Map<String, Set<String>> result = new HashMap<>();
        for (Tuple tuple : tagTuples) {
            String hostUuid = tuple.get(0, String.class);
            String serialNumber = Platform.normalizeMachineSerialNumber(
                    HostSystemTags.SYSTEM_SERIAL_NUMBER.getTokenByTag(
                            tuple.get(1, String.class), HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN));
            if (serialNumber != null) {
                result.computeIfAbsent(hostUuid, ignored -> new LinkedHashSet<>()).add(serialNumber);
            }
        }
        return result;
    }

    private void updateHostLinks(Map<String, String> links) {
        if (links.isEmpty()) {
            return;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(links.entrySet());
        for (int start = 0; start < entries.size(); start += HOST_LINK_UPDATE_BATCH_SIZE) {
            updateHostLinks(entries.subList(start, Math.min(start + HOST_LINK_UPDATE_BATCH_SIZE, entries.size())));
        }
    }

    private void updateHostLinks(List<Map.Entry<String, String>> links) {
        StringBuilder sql = new StringBuilder("UPDATE IGNORE HostEO SET serverUuid = CASE uuid");
        int index = 0;
        for (Map.Entry<String, String> ignored : links) {
            sql.append(" WHEN :host").append(index).append(" THEN :server").append(index);
            index++;
        }
        sql.append(" ELSE serverUuid END WHERE serverUuid IS NULL AND uuid IN (");
        for (int i = 0; i < links.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(":host").append(i);
        }
        sql.append(')');
        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        index = 0;
        for (Map.Entry<String, String> link : links) {
            query.setParameter("host" + index, link.getKey());
            query.setParameter("server" + index, link.getValue());
            index++;
        }
        query.executeUpdate();
    }

    private void clearDeletedHostLinks(Collection<String> serverUuids) {
        Set<String> targets = new LinkedHashSet<>(serverUuids);
        targets.remove(null);
        if (targets.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "UPDATE HostEO SET serverUuid = NULL " + "WHERE deleted IS NOT NULL AND serverUuid IN (");
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(":server").append(i);
        }
        sql.append(')');
        Query query = dbf.getEntityManager().createNativeQuery(sql.toString());
        int index = 0;
        for (String serverUuid : targets) {
            query.setParameter("server" + index, serverUuid);
            index++;
        }
        query.executeUpdate();
    }

    public RoleServiceManifest roleServices() {
        return RoleServiceManifest.load(ROLE_SERVICE_MANIFEST_PATH, type.toString());
    }

    public static class ApplyResourceControlAgentCommand extends ManagedServiceAgentCommand {
        private String cpuSet;
        private Long memory;
        private String isolationMode;

        public String getCpuSet() {
            return cpuSet;
        }

        public void setCpuSet(String cpuSet) {
            this.cpuSet = cpuSet;
        }

        public Long getMemory() {
            return memory;
        }

        public void setMemory(Long memory) {
            this.memory = memory;
        }

        public String getIsolationMode() {
            return isolationMode;
        }

        public void setIsolationMode(String isolationMode) {
            this.isolationMode = isolationMode;
        }
    }

    public static class ManagedServiceAgentCommand extends KVMAgentCommands.AgentCommand {
        private String roleType;
        private String sliceName;
        private List<ResourceConsumerHandle> handles = new ArrayList<>();

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public String getSliceName() {
            return sliceName;
        }

        public void setSliceName(String sliceName) {
            this.sliceName = sliceName;
        }

        public List<ResourceConsumerHandle> getHandles() {
            return handles;
        }

        public void setHandles(List<ResourceConsumerHandle> handles) {
            this.handles = handles;
        }
    }

    public static class ManagedServiceUsageAgentResponse extends KVMAgentCommands.AgentResponse {
        private List<ManagedServiceResourceUsage> services = new ArrayList<>();

        public List<ManagedServiceResourceUsage> getServices() {
            return services;
        }

        public void setServices(List<ManagedServiceResourceUsage> services) {
            this.services = services;
        }
    }

    public static class ResourceControlAgentResponse extends KVMAgentCommands.AgentResponse {
        private boolean synced;

        public boolean isSynced() {
            return synced;
        }

        public void setSynced(boolean synced) {
            this.synced = synced;
        }

    }

}
