package org.zstack.sugonSdnController.network;

import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.kvm.KVMCompleteNicInformationExtensionPoint;
import org.zstack.network.service.MtuGetter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;

public class TfCompleteNicInformationExtensionPointImpl implements KVMCompleteNicInformationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TfCompleteNicInformationExtensionPointImpl.class);
    @Override
    public NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        NicTO to = new NicTO();
        to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
        to.setType(nic.getType());
        to.setIps(new ArrayList<String>(Arrays.asList(nic.getIp())));
        VmGlobalConfig.VM_CLEAN_TRAFFIC.updateValue(true);
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
        logger.debug("Complete nic information for TfL2Network");
        return to;
    }
    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        return null;
    }
    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn(){
        return L2NetworkType.valueOf(SugonSdnControllerConstant.L2_TF_NETWORK_TYPE);
    }
}
