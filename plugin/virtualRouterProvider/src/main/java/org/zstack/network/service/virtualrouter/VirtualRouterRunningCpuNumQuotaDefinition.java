package org.zstack.network.service.virtualrouter;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.vm.VmInstanceState;

import static org.zstack.utils.CollectionDSL.list;

public class VirtualRouterRunningCpuNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VirtualRouterQuotaConstant.VIRTUAL_ROUTER_RUNNING_CPU_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VirtualRouterQuotaGlobalConfig.VIRTUAL_ROUTER_RUNNING_CPU_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        // get running vrouter cpu num and memory size
        String sql = "select sum(vr.cpuNum)" +
                " from VirtualRouterVmVO vr, AccountResourceRefVO ref" +
                " where vr.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and not (vr.state = :starting and vr.hostUuid is null)" +
                " and vr.state not in (:states)";

        SQL query = SQL.New(sql, Long.class)
                .param("auuid", accountUuid)
                .param("starting", VmInstanceState.Starting)
                .param("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                        VmInstanceState.Destroyed, VmInstanceState.Created));
        Long cpuNum = query.find();
        if (cpuNum == null) {
            return 0L;
        }

        return cpuNum;
    }
}
