package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class ApplianceVmKvmBackend implements KVMStartVmAddonExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmKvmBackend.class);

    @Autowired
    private CloudBus bus;

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return ApplianceVmFactory.type;
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        KVMAddons.Channel chan = new KVMAddons.Channel();
        chan.setSocketPath(makeChannelSocketPath(spec.getVmInventory().getUuid()));
        chan.setTargetName(String.format("applianceVm.vport"));
        cmd.getAddons().put(chan.NAME, chan);
        logger.debug(String.format("make kvm channel device[path:%s, target:%s]", chan.getSocketPath(), chan.getTargetName()));
    }

    public String makeChannelSocketPath(String apvmuuid) {
        return PathUtil.join(ApplianceVmConstant.KVM_CHANNEL_AGENT_PATH, String.format("applianceVm.%s", apvmuuid));
    }
}
