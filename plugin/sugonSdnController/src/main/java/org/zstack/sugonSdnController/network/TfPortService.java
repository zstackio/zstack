package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.zstack.compute.vm.MacOperator;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;

import java.util.List;
import java.util.Map;

public class TfPortService {
    @Autowired
    protected AccountManager acntMgr;

    public TfPortResponse createTfPort(String tfPortUUid, VmInstanceInventory vm, L3NetworkInventory l3) {
        MacOperator mo = new MacOperator();
        String customMac = mo.getMac(vm.getUuid(), l3.getUuid());
        Map<String, List<String>> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(vm.getUuid());
        Map<Integer, String> nicStaticIpMap = new StaticIpOperator().getNicStaticIpMap(vmStaticIps.get(l3.getUuid()));

        // ignoring ipv6 now , so if the user didn't assign ip on the webpage,then useTf Ip;
        String customIp = nicStaticIpMap.get(4);

        //invoke tf rest interface to retrieve real ip and mac and portId
        TfPortClient tfPortClient = new TfPortClient();
        String tfL2NetworkId = StringDSL.transToTfUuid(l3.getL2NetworkUuid());
        String tfL3NetworkId = StringDSL.transToTfUuid(l3.getUuid());
        String accountId = StringDSL.transToTfUuid(acntMgr.getOwnerAccountUuidOfResource(vm.getUuid()));
        String vmiUuid = StringDSL.transToTfUuid(vm.getUuid());
        TfPortResponse port = tfPortClient.createPort(tfL2NetworkId, tfL3NetworkId, customMac, customIp, accountId, vmiUuid, tfPortUUid);
        if (port.getCode() != HttpStatus.OK.value()) {
            // fail  to rollback the flowchain
            throw new RuntimeException("failed to invoke creating tf port: " + port);
        }
        return port;
    }
}
