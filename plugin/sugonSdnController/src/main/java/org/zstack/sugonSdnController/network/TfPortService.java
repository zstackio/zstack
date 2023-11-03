package org.zstack.sugonSdnController.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.zstack.compute.vm.MacOperator;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.identity.AccountManager;
import org.zstack.sugonSdnController.controller.api.Status;
import org.zstack.sugonSdnController.controller.api.types.KeyValuePair;
import org.zstack.sugonSdnController.controller.api.types.KeyValuePairs;
import org.zstack.sugonSdnController.controller.api.types.VirtualMachineInterface;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortClient;
import org.zstack.sugonSdnController.controller.neutronClient.TfPortResponse;
import org.zstack.utils.StringDSL;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TfPortService {
    @Autowired
    protected AccountManager acntMgr;

    public TfPortResponse getTfPort(String tfPortUUid) {
        TfPortClient tfPortClient = new TfPortClient();
        return tfPortClient.getVirtualMachineInterface(tfPortUUid);
    }

    public List<VirtualMachineInterface> getTfPortsDetail() {
        TfPortClient tfPortClient = new TfPortClient();
        return tfPortClient.getVirtualMachineInterfaceDetail();
    }

    public TfPortResponse createTfPort(String tfPortUUid, String l2NetworkUuid, String l3NetworkUuid, String mac, String ip) {
        //invoke tf rest interface to retrieve real ip and mac and portId
        TfPortClient tfPortClient = new TfPortClient();
        String tfL2NetworkId = StringDSL.transToTfUuid(l2NetworkUuid);
        String tfL3NetworkId = StringDSL.transToTfUuid(l3NetworkUuid);
        TfPortResponse port = tfPortClient.createPort(tfL2NetworkId, tfL3NetworkId, mac, ip, null,
                tfPortUUid, null, null);
        if (port.getCode() != HttpStatus.OK.value()) {
            // fail  to rollback the flowchain
            throw new RuntimeException("failed to invoke creating tf port: " + port);
        }
        return port;
    }

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
        String vmiUuid = StringDSL.transToTfUuid(vm.getUuid());
        String vmName = vm.getName();

        try {
            boolean availability = tfPortClient.checkTfIpAvailability(customIp, l3.getUuid());
            if (availability){
                customIp = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TfPortResponse port = tfPortClient.createPort(tfL2NetworkId, tfL3NetworkId, customMac, customIp, vmiUuid,
                tfPortUUid, vmName, null);
        if (port.getCode() != HttpStatus.OK.value()) {
            // fail  to rollback the flowchain
            throw new RuntimeException("failed to invoke creating tf port: " + port);
        }
        return port;
    }

    public TfPortResponse createTfPort(L3NetworkInventory l3, String portUuid, String ip, String mac, KeyValuePairs bindInfo) {
        //invoke tf rest interface to retrieve real ip and mac and portId
        TfPortClient tfPortClient = new TfPortClient();
        String tfL2NetworkId = StringDSL.transToTfUuid(l3.getL2NetworkUuid());
        String tfL3NetworkId = StringDSL.transToTfUuid(l3.getUuid());
        String tfPortUuid = StringDSL.transToTfUuid(portUuid);

        try {
            boolean availability = tfPortClient.checkTfIpAvailability(ip, l3.getUuid());
            if (availability){
                ip = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TfPortResponse port = tfPortClient.createPort(tfL2NetworkId, tfL3NetworkId, mac, ip, null, tfPortUuid, null, bindInfo);
        if (port.getCode() != HttpStatus.OK.value()) {
            // fail  to rollback the flowchain
            throw new RuntimeException("failed to invoke creating tf port: " + port);
        }
        return port;
    }

    public TfPortResponse deleteTfPort(String portUUid) {
        String tfPortUUid = StringDSL.transToTfUuid(portUUid);
        TfPortClient tfPortClient = new TfPortClient();
        return tfPortClient.deletePort(tfPortUUid);
    }

    public Status updateTfPort(String portUUid, String bmUuid, KeyValuePairs bindInfo) {
        String accountId = StringDSL.transToTfUuid(acntMgr.getOwnerAccountUuidOfResource(bmUuid));
        String tfBmUUid = StringDSL.transToTfUuid(bmUuid);
        String tfPortUUid = StringDSL.transToTfUuid(portUUid);
        TfPortClient tfPortClient = new TfPortClient();
        return tfPortClient.updateTfPort(tfPortUUid, accountId, tfBmUUid, bindInfo);
    }

}
