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
        } else if (spec.getMessage() instanceof APIRebootVmInstanceMsg) {
            systemTags = ((APIRebootVmInstanceMsg)spec.getMessage()).getSystemTags();
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

        /* make a new allocated ip for default public nic */
        for (VmNicVO nic : applianceVmVO.getVmNics()) {
            if (applianceVmVO.getDefaultRouteL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.ip, nic.getIp()).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).find();
                if (vipVO != null) {

                    Long[] usedIpInLongs;
                    List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid()).list();
                    ipRangeVOS = ipRangeVOS.stream().sorted(new Comparator<IpRangeVO>() {
                        @Override
                        public int compare(IpRangeVO r1, IpRangeVO r2) {
                            return r1.getStartIp().compareTo(r2.getStartIp());
                        }
                    }).collect(Collectors.toList());

                    List<String> usedIps = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, nic.getL3NetworkUuid()).select(UsedIpVO_.ip)
                            .orderBy(UsedIpVO_.ip, SimpleQuery.Od.ASC).listValues();
                    if (usedIps == null || usedIps.isEmpty()) {
                        usedIpInLongs = new Long[0];
                    } else {
                        usedIpInLongs = new Long[usedIps.size()];
                        int i = 0;
                        for (String ip : usedIps) {
                            usedIpInLongs[i++] = NetworkUtils.ipv4StringToLong(ip);
                        }
                    }

                    String startIp = ipRangeVOS.get(0).getStartIp();
                    String endIp = ipRangeVOS.get(ipRangeVOS.size() -1).getEndIp();
                    String ip = NetworkUtils.findFirstAvailableIpv4Address(startIp, endIp, usedIpInLongs);

                    AllocateIpMsg amsg = new AllocateIpMsg();
                    amsg.setL3NetworkUuid(nic.getL3NetworkUuid());
                    if (ip != null) {
                        amsg.setRequiredIp(ip);
                    }
                    bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                    AllocateIpReply reply = (AllocateIpReply)bus.call(amsg);
                    if (!reply.isSuccess()) {
                        chain.fail(reply.getError());
                        return;
                    }

                    UsedIpInventory usedIp = reply.getIpInventory();
                    data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_oldUsedIpUuid.toString(), nic.getUsedIpUuid());
                    data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_oldUsedIp.toString(), nic.getIp());
                    data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_nic.toString(), nic);
                    data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_newUsedIp.toString(), usedIp);
                    SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid()).set(VmNicVO_.usedIpUuid, usedIp.getUuid())
                            .set(VmNicVO_.ip, usedIp.getIp()).update();
                    SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIp.getUuid()).set(UsedIpVO_.vmNicUuid, nic.getUuid()).update();

                    VmInstanceVO vmInstanceVO = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
                    spec.setVmInventory(VmInstanceInventory.valueOf(vmInstanceVO));
                }
            }
        }

        SQL.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, inv.getUuid()).set(ApplianceVmVO_.haStatus, ApplianceVmHaStatus.Backup).update();

        for (ApplianceVmSyncConfigToHaGroupExtensionPoint exp : exps) {
            exp.applianceVmSyncConfigToHa(ApplianceVmInventory.valueOf(applianceVmVO), haUuid);
        }

        data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_applianceVm.toString(), applianceVmVO);
        data.put(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_haUuid, haUuid);
        chain.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        ApplianceVmVO applianceVmVO = (ApplianceVmVO)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_applianceVm.toString());
        String haUuid = (String)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_haUuid.toString());
        VmNicVO nicVO = (VmNicVO)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_nic.toString());
        UsedIpInventory usedIp = (UsedIpInventory)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_newUsedIp.toString());
        String oldIp = (String)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_oldUsedIp.toString());
        String oldUuid = (String)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_oldUsedIpUuid.toString());

        if (nicVO != null && oldUuid != null) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, nicVO.getUuid()).set(VmNicVO_.usedIpUuid, oldUuid).set(VmNicVO_.ip, oldIp).update();
            ReturnIpMsg rmsg = new ReturnIpMsg();
            rmsg.setL3NetworkUuid(nicVO.getL3NetworkUuid());
            rmsg.setUsedIpUuid(usedIp.getUuid());
            rmsg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
            bus.send(rmsg);
        }

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
