package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class PhysicalServerAssignmentRepository {
    private static final CLogger logger =
            Utils.getLogger(PhysicalServerAssignmentRepository.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public void ensureDefaults(
            Collection<String> serverUuids, String roleType) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return;
        }
        Query insert = dbf.getEntityManager().createNativeQuery(
                "INSERT IGNORE INTO PhysicalServerResourceAssignmentVO " +
                        "(uuid, serverUuid, roleType, cpuSet, state, createDate, lastOpDate) " +
                        "SELECT REPLACE(UUID(), '-', ''), p.uuid, :roleType, '', :state, NOW(), NOW() " +
                        "FROM PhysicalServerVO p WHERE p.uuid IN (:serverUuids)");
        insert.setParameter("serverUuids", serverUuids);
        insert.setParameter("roleType", roleType);
        insert.setParameter("state", PhysicalServerResourceAssignmentState.Unsynced.name());
        insert.executeUpdate();
    }

    public PhysicalServerResourceAssignmentVO update(
            APIUpdatePhysicalServerResourceAssignmentMsg msg) {
        PhysicalServerResourceAssignmentVO current = requireAssignment(
                msg.getServerUuid(), msg.getRoleType());
        String cpuSet = msg.getCpuSet() == null
                ? current.getCpuSet() : msg.getCpuSet();
        Long memory = msg.getMemory() == null
                ? current.getMemory() : msg.getMemory();
        int updated = SQL.New(
                        "update PhysicalServerResourceAssignmentVO a set " +
                                "a.cpuSet = :cpuSet, a.memory = :memory, a.state = :state " +
                                "where a.uuid = :uuid")
                .param("cpuSet", cpuSet)
                .param("memory", memory)
                .param("state", PhysicalServerResourceAssignmentState.Unsynced)
                .param("uuid", current.getUuid())
                .execute();
        if (updated == 0) {
            throw assignmentMissing(msg.getServerUuid(), msg.getRoleType());
        }
        PhysicalServerResourceAssignmentVO result = dbf.findByUuid(
                current.getUuid(), PhysicalServerResourceAssignmentVO.class);
        logger.info(String.format(
                "physical server resource assignment updated: serverUuid[%s], " +
                        "roleType[%s], accountUuid[%s], cpuSet[%s -> %s], memory[%s -> %s]",
                current.getServerUuid(), current.getRoleType(),
                msg.getSession() == null ? null : msg.getSession().getAccountUuid(),
                current.getCpuSet(), result.getCpuSet(),
                current.getMemory(), result.getMemory()));
        return result;
    }

    public PhysicalServerResourceAssignmentVO initializeCpuSet(
            PhysicalServerResourceAssignmentVO current, String cpuSet) {
        if (current.getCpuSet() != null && !current.getCpuSet().isEmpty()) {
            return current;
        }
        SQL.New(
                        "update PhysicalServerResourceAssignmentVO a set " +
                                "a.cpuSet = :cpuSet, a.state = :state " +
                                "where a.uuid = :uuid and a.cpuSet = ''")
                .param("cpuSet", cpuSet)
                .param("state", PhysicalServerResourceAssignmentState.Unsynced)
                .param("uuid", current.getUuid())
                .execute();
        return dbf.findByUuid(
                current.getUuid(), PhysicalServerResourceAssignmentVO.class);
    }

    public boolean markSynced(PhysicalServerResourceAssignmentVO applied) {
        String memoryPredicate = applied.getMemory() == null
                ? "a.memory is null" : "a.memory = :memory";
        SQL sql = SQL.New(
                        "update PhysicalServerResourceAssignmentVO a set a.state = :state " +
                                "where a.uuid = :uuid and a.cpuSet = :cpuSet and " + memoryPredicate)
                .param("state", PhysicalServerResourceAssignmentState.Synced)
                .param("uuid", applied.getUuid())
                .param("cpuSet", applied.getCpuSet());
        if (applied.getMemory() != null) {
            sql.param("memory", applied.getMemory());
        }
        return sql.execute() == 1;
    }

    public void markUnsynced(String uuid) {
        SQL.New("update PhysicalServerResourceAssignmentVO a " +
                        "set a.state = :state where a.uuid = :uuid")
                .param("state", PhysicalServerResourceAssignmentState.Unsynced)
                .param("uuid", uuid)
                .execute();
    }

    public boolean delete(String uuid) {
        boolean deleted = SQL.New(
                        "delete from PhysicalServerResourceAssignmentVO a where a.uuid = :uuid")
                .param("uuid", uuid)
                .execute() == 1;
        if (deleted) {
            logger.info(String.format(
                    "physical server resource assignment deleted after release: assignmentUuid[%s]",
                    uuid));
        }
        return deleted;
    }

    public PhysicalServerResourceAssignmentVO find(
            String serverUuid, String roleType) {
        return Q.New(PhysicalServerResourceAssignmentVO.class)
                .eq(PhysicalServerResourceAssignmentVO_.serverUuid, serverUuid)
                .eq(PhysicalServerResourceAssignmentVO_.roleType, roleType)
                .find();
    }

    public List<PhysicalServerResourceAssignmentVO> listAssignments() {
        return Q.New(PhysicalServerResourceAssignmentVO.class).list();
    }

    public List<PhysicalServerResourceAssignmentVO> listAssignments(
            Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return Collections.emptyList();
        }
        return Q.New(PhysicalServerResourceAssignmentVO.class)
                .in(PhysicalServerResourceAssignmentVO_.serverUuid, serverUuids)
                .list();
    }

    private PhysicalServerResourceAssignmentVO requireAssignment(
            String serverUuid, String roleType) {
        PhysicalServerResourceAssignmentVO current = find(serverUuid, roleType);
        if (current == null) {
            throw assignmentMissing(serverUuid, roleType);
        }
        return current;
    }

    private OperationFailureException assignmentMissing(
            String serverUuid, String roleType) {
        return new OperationFailureException(operr(
                PhysicalServerConstant.ERROR_CODE,
                "RESOURCE_ASSIGNMENT_NOT_FOUND: resource assignment for role[%s] " +
                        "does not exist on physical server[uuid:%s]",
                roleType, serverUuid));
    }
}
