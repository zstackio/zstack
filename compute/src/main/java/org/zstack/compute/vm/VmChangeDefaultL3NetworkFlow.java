package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmChangeDefaultL3NetworkFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmChangeDefaultL3NetworkFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private PluginRegistry pluginRgty;

    private boolean isDefaultNic(VmNicInventory nic, String dafaultL3Uuid) {
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.getL3NetworkUuid().equals(dafaultL3Uuid)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmNicInventory nic = (VmNicInventory) data.get(VmInstanceConstant.Params.VmNicInventory.toString());

        if (nic.getVmInstanceUuid() == null) {
            trigger.next();
            return;
        }

        VmInstanceVO vmVo = dbf.findByUuid(nic.getVmInstanceUuid(), VmInstanceVO.class);
        VmNicVO nicvo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);

        /* if this is default nic and vm default l3 is different from nic l3 */
        if (isDefaultNic(nic, vmVo.getDefaultL3NetworkUuid()) ) {
            if (vmVo.getDefaultL3NetworkUuid() == null || !vmVo.getDefaultL3NetworkUuid().equals(nicvo.getL3NetworkUuid())) {
                vmVo.setDefaultL3NetworkUuid(nicvo.getL3NetworkUuid());
                dbf.updateAndRefresh(vmVo);
            }
        }

        trigger.next();
    }
}
