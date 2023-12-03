package org.zstack.compute.vm.quota;

import org.zstack.compute.vm.VmQuotaConstant;
import org.zstack.compute.vm.VmQuotaGlobalConfig;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;

import static org.zstack.utils.CollectionDSL.list;

public class VmTotalNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VmQuotaConstant.VM_TOTAL_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VmQuotaGlobalConfig.VM_TOTAL_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(vm)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and not (vm.hostUuid is null and vm.lastHostUuid is null and vm.rootVolumeUuid is null)" +
                " and vm.state not in (:states)" +
                " and vm.type != :vmtype";

        Long used = SQL.New(sql, Long.class)
                .param("auuid", accountUuid)
                .param("rtype", VmInstanceVO.class.getSimpleName())
                .param("states", list(VmInstanceState.Destroyed))
                .param("vmtype", "baremetal2")
                .find();
        return used == null ? 0L : used;
    }
}
