package org.zstack.kvm;

import org.zstack.core.db.SQL;
import org.zstack.core.db.HardDeleteEntityExtensionPoint;
import org.zstack.core.db.SoftDeleteEntityExtensionPoint;
import org.zstack.header.host.HostVO;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.ServerRoleType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * P1-6 (ZSTAC-84191): atomically delete the KVM host's PhysicalServerRoleVO row
 * inside the same transaction as the HostVO soft-delete.
 *
 * <p>Background. The previous {@code KVMHost.deleteHook()} fired the role-row
 * delete from inside {@code HostBase.handle(HostDeletionMsg)}, BEFORE the
 * cascade callback in {@code HostCascadeExtension.handleDeletion} ran
 * {@code dbf.removeByPrimaryKeys(uuids, HostVO.class)}. Since {@code deleteHook}
 * had no transaction boundary, the role-row DELETE committed immediately. If
 * any later step (the {@code afterDelete} extension chain, or the HostEO
 * soft-delete UPDATE itself) failed, the system was left with the role row
 * gone but the HostVO still present — the exact reverse-orphan that violates
 * the {@code UNIQUE(serverUuid, KVM_HOST)} invariant during a retry.
 *
 * <p>Fix. Move the role-row delete to a {@link SoftDeleteEntityExtensionPoint}
 * keyed on {@link HostVO}. {@code DatabaseFacadeImpl.softDelete(Collection)}
 * fires {@code postSoftDelete} synchronously inside the same
 * {@code REQUIRES_NEW} transaction that performs the {@code UPDATE HostEO SET
 * deleted=NOW()}. Either both writes commit atomically or both roll back —
 * MySQL handles the atomicity at the transaction level, no FK is required
 * (which would not work anyway because {@code roleUuid} points at HostVO for
 * KVM/Container but at {@code BareMetal2ChassisVO} for BM2, so a single FK
 * cannot apply across role types).
 *
 * <p>Cross-host-type safety. The hook fires for ALL HostVO soft-deletes
 * (KVM, ESXi, NativeHostVO/container, baremetal-1). The WHERE-clause filter on
 * {@code roleType = KVM_HOST} keeps it harmless on non-KVM HostVO subclasses:
 * the DELETE matches zero rows for those, contributing only a bounded constant
 * to the soft-delete tx.
 *
 * <p>Note. BM2 ({@code BareMetal2ChassisVO}) and any later role types backed
 * by VOs other than HostVO need their own equivalent extension keyed on the
 * appropriate parent VO.
 */
public class KvmPhysicalServerRoleSoftDeleteExtension implements SoftDeleteEntityExtensionPoint, HardDeleteEntityExtensionPoint {

    @Override
    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
        return Collections.singletonList(HostVO.class);
    }

    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        return Collections.singletonList(HostVO.class);
    }

    @Override
    public void postSoftDelete(Collection entityIds, Class entityClass) {
        deleteRoleRows(entityIds);
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        deleteRoleRows(entityIds);
    }

    private void deleteRoleRows(Collection entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }
        SQL.New(PhysicalServerRoleVO.class)
                .in(PhysicalServerRoleVO_.roleUuid, entityIds)
                .eq(PhysicalServerRoleVO_.roleType, ServerRoleType.KVM_HOST.toString())
                .delete();
    }
}
