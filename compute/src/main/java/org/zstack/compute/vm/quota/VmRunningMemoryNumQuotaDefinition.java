package org.zstack.compute.vm.quota;

import org.zstack.compute.vm.VmQuotaConstant;
import org.zstack.compute.vm.VmQuotaGlobalConfig;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmRunningMemoryNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VmQuotaConstant.VM_RUNNING_MEMORY_SIZE;
    }

    @Override
    public Long getDefaultValue() {
        return VmQuotaGlobalConfig.VM_RUNNING_MEMORY_SIZE.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select sum(vm.memorySize)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and not (vm.state = :starting and vm.hostUuid is null)" +
                " and vm.state not in (:states)" +
                " and vm.type not in (:vmtypes)";

        List<String> excludeVmTypes = new ArrayList<>();
        excludeVmTypes.add("baremetal2");
        excludeVmTypes.add("ApplianceVm");

        Long used = SQL.New(sql, Long.class)
                .param("auuid", accountUuid)
                .param("rtype", VmInstanceVO.class.getSimpleName())
                .param("starting", VmInstanceState.Starting)
                .param("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                        VmInstanceState.Destroyed, VmInstanceState.Created))
                .param("vmtypes", excludeVmTypes)
                .find();
        return used == null ? 0L : used;
    }
}

