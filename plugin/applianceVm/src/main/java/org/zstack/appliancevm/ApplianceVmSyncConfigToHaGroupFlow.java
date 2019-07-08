package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmSyncConfigToHaGroupFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ApplianceVmSyncConfigToHaGroupFlow.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        List<ApplianceVmSyncConfigToHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(ApplianceVmSyncConfigToHaGroupExtensionPoint.class);
        if (exps == null || exps.isEmpty()) {
            chain.next();
            return;
        }

        List<String> systemTags = null;
        if (spec.getMessage() instanceof APIStartVmInstanceMsg) {
            systemTags = ((APIStartVmInstanceMsg)spec.getMessage()).getSystemTags();
        } else {
            chain.next();
            return;
        }

        if (systemTags == null || systemTags.isEmpty()) {
            chain.next();
            return;
        }

        VmInstanceInventory inv = spec.getVmInventory();
        ApplianceVmVO applianceVmVO = dbf.findByUuid(inv.getUuid(), ApplianceVmVO.class);
        /* only handle first join ha group */
        if (applianceVmVO.isHaEnabled()) {
            chain.next();
            return;
        }

        String haUuid = null;
        for (String sysTag : systemTags) {
            if (!ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTagFormat(), sysTag);
            haUuid = token.get(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);
        }
        if (haUuid == null) {
            chain.next();
            return;
        }

        SQL.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, inv.getUuid()).set(ApplianceVmVO_.haStatus, ApplianceVmHaStatus.Backup).update();

        for (ApplianceVmSyncConfigToHaGroupExtensionPoint exp : exps) {
            exp.applianceVmSyncConfigToHa(ApplianceVmInventory.valueOf(applianceVmVO), haUuid);
        }

        data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_applianceVm.toString(), applianceVmVO);
        data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_haUuid.toString(), haUuid);
        chain.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        ApplianceVmVO applianceVmVO = (ApplianceVmVO)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_applianceVm.toString());
        String haUuid = (String)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_haUuid.toString());

        if (haUuid == null || applianceVmVO == null) {
            trigger.rollback();
            return;
        }

        for (ApplianceVmSyncConfigToHaGroupExtensionPoint exp : pluginRgty.getExtensionList(ApplianceVmSyncConfigToHaGroupExtensionPoint.class)) {
            exp.applianceVmSyncConfigToHaRollback(ApplianceVmInventory.valueOf(applianceVmVO), haUuid);
        }

        trigger.rollback();
    }
}
