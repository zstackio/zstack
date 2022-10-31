package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.compute.vm.MacOperator;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmPreAttachL3NetworkExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/*
   request tf sdn controller to create a port and set the returned ip & mac & portId to system tag for later use
 */
public class VmPreAttachL3NetworkExtensionPointImpl implements VmPreAttachL3NetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmPreAttachL3NetworkExtensionPointImpl.class);
    @Autowired
    protected AccountManager acntMgr;
    @Override
    public void vmPreAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3) {
        if (!SugonSdnControllerConstant.L3_TF_NETWORK_TYPE.equals(l3.getType())) {
            return;
        }
        MacOperator mo = new MacOperator();
        String customMac = mo.getMac(vm.getUuid(), l3.getUuid());
        Map<String, List<String>> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(vm.getUuid());
        Map<Integer, String> nicStaticIpMap = new StaticIpOperator().getNicStaticIpMap(vmStaticIps.get(l3.getUuid()));
        // TODO
        // ignoring ipv6 now , so if the user didn't assign ip on the webpage,then useTf Ip;
        String customIp = nicStaticIpMap.get(4);

        //invoke tf rest interface to retrieve real ip and mac and portId
        TfPortClient tfPortClient = new TfPortClient();
        String tfL2NetworkId = StringDSL.transToTfUuid(l3.getL2NetworkUuid());
        String tfL3NetworkId = StringDSL.transToTfUuid(l3.getUuid());
        String accountId = StringDSL.transToTfUuid(acntMgr.getOwnerAccountUuidOfResource(vm.getUuid()));
        String vmiUuid = StringDSL.transToTfUuid(vm.getUuid());
        TfPortResponse port = tfPortClient.createPort(tfL2NetworkId, tfL3NetworkId, customMac, customIp, accountId, vmiUuid);
        String finalMac = port.getMacAddress();
        String finalIp = port.getFixedIps().get(0).getIpAddress();
        String nicUuid = StringDSL.transToZstackUuid(port.getPortId());

        // set the results to system tag
        // because MacOperator doesn't provide a set mac method , so we set it here using SystemTagCreator
        SystemTagCreator macCreator = VmSystemTags.CUSTOM_MAC.newSystemTagCreator(vm.getUuid());
        macCreator.ignoreIfExisting = false;
        macCreator.inherent = false;
        macCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3.getUuid()),
                e(VmSystemTags.MAC_TOKEN, finalMac)
        ));
        macCreator.create();

        SystemTagCreator ipTagCreator = VmSystemTags.STATIC_IP.newSystemTagCreator(vm.getUuid());
        ipTagCreator.ignoreIfExisting = false;
        ipTagCreator.inherent = false;
        ipTagCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3.getUuid()),
                e(VmSystemTags.STATIC_IP_TOKEN, finalIp)
        ));
        ipTagCreator.create();

        SystemTagCreator nicIdCreator = VmSystemTags.CUSTOM_NIC_UUID.newSystemTagCreator(vm.getUuid());
        nicIdCreator.ignoreIfExisting = false;
        nicIdCreator.inherent = false;
        nicIdCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3.getUuid()),
                e(VmSystemTags.NIC_UUID_TOKEN, nicUuid)
        ));
        nicIdCreator.create();
    }

}
