package org.zstack.test.integration.network.sdnController

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.*
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant
import org.zstack.sugonSdnController.controller.api.types.MacAddressesType
import org.zstack.sugonSdnController.controller.api.types.Project
import org.zstack.sugonSdnController.controller.api.types.VirtualMachineInterface
import org.zstack.sugonSdnController.controller.api.ApiSerializer
import org.zstack.sugonSdnController.controller.api.TfCommands
import org.zstack.sdnController.header.SdnControllerVO
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import javax.persistence.TypedQuery;
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus


class SugonSdnControllerCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        spring {
            useSpring(SdnControllerTest.springSpec)
        }
    }

    @Override
    void environment() {
        env = SugonSdnControllerEnv.SdnControllerBasicEnv()
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testTfApi()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testTfApi() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster1 = env.inventoryByName("cluster1") as ClusterInventory
        def cluster2 = env.inventoryByName("cluster2") as ClusterInventory
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image1") as ImageInventory

        String sql = "select sdn" +
                " from SdnControllerVO sdn" +
                " where sdn.vendorType = :vendorType";
        TypedQuery<SdnControllerVO> q = dbf.getEntityManager().createQuery(sql, SdnControllerVO.class);
        q.setParameter("vendorType", SugonSdnControllerConstant.TF_CONTROLLER);
        List<SdnControllerVO> sdns = q.getResultList();
        SdnControllerVO sdn = sdns.get(0);
        updateSdnController {
            uuid = sdn.uuid
            name = "sugon_sdn"
            description = "sugon sdn:tf"
        }
        SdnControllerVO  vo = dbf.findByUuid(sdn.uuid, SdnControllerVO.class)
        assert vo.name == "sugon_sdn"
        assert vo.description == "sugon sdn:tf"

        L2NetworkInventory l2TfNetwork = createL2TfNetwork {
            name = "tfL2Network"
            type = SugonSdnControllerConstant.L2_TF_NETWORK_TYPE
            physicalInterface = sdn.uuid
            zoneUuid = zone.uuid
        }

        updateL2Network {
            uuid = l2TfNetwork.uuid
            name = "test_tf_l2"
            description = "test tf l2 network"
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = l2TfNetwork.uuid
            clusterUuid = cluster1.uuid
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = l2TfNetwork.uuid
            clusterUuid = cluster2.uuid
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = l2TfNetwork.uuid
            clusterUuid = cluster1.uuid
        }

        List<L2NetworkInventory> l2TfNetworks = queryL2Network {
            conditions=["type=" + SugonSdnControllerConstant.L2_TF_NETWORK_TYPE]}
        assert l2TfNetworks.size() == 1

        L3NetworkInventory test_l3_1 = createL3Network {
            l2NetworkUuid = l2TfNetwork.uuid
            name = "test-l3"
            type = SugonSdnControllerConstant.L3_TF_NETWORK_TYPE
        }

        List<L3NetworkInventory> l3TfNetworks = queryL3Network {
            conditions=["type=" + SugonSdnControllerConstant.L3_TF_NETWORK_TYPE]}
        assert l3TfNetworks.size() == 1

        updateL3Network {
            uuid = test_l3_1.uuid
            name = "test_l3_1"
            description = "test_l3_1"
        }
        L3NetworkVO  l3Network = dbf.findByUuid(test_l3_1.uuid, L3NetworkVO.class)
        assert l3Network.name == "test_l3_1"
        assert l3Network.description == "test_l3_1"

        addIpRangeByNetworkCidr {
            name = "Test-IPRange"
            networkCidr = "192.168.10.0/24"
            l3NetworkUuid = test_l3_1.uuid
        }

        L3NetworkInventory l3Inv = addDnsToL3Network {
            l3NetworkUuid = test_l3_1.uuid
            dns = "1.1.1.1"
        }
        assert l3Inv.dns.get(0) == '1.1.1.1'

        l3Inv = removeDnsFromL3Network {
            l3NetworkUuid = test_l3_1.uuid
            dns = "1.1.1.1"
        }
        assert l3Inv.dns == null

        VmInstanceInventory vm = createVmInstance {
            name = "test-vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [test_l3_1.uuid]
        }

        L3NetworkInventory test_l3_2 = createL3Network {
            l2NetworkUuid = l2TfNetwork.uuid
            name = "test-l3-2"
            type = SugonSdnControllerConstant.L3_TF_NETWORK_TYPE
        }

        addIpRangeByNetworkCidr {
            name = "Test-IPRange2"
            networkCidr = "192.168.20.0/24"
            l3NetworkUuid = test_l3_2.uuid
        }

        env.simulator(TfCommands.TF_GET_VMI) {
            VirtualMachineInterface rsp = new VirtualMachineInterface();
            rsp.name = "5255167d-46c8-4d9e-b4b1-e20c38ce25d9"
            rsp.uuid = "5255167d-46c8-4d9e-b4b1-e20c38ce25d9"
            List<String> macList = new ArrayList<String>();
            macList.add("08:00:27:b4:e1:98");
            MacAddressesType macAddress = new MacAddressesType(macList);
            rsp.setMacAddresses(macAddress);
            Project project = new Project();
            project.name = TfCommands.TEST_PROJECT_UUID
            project.uuid = TfCommands.TEST_PROJECT_UUID
            project.displayName = "admin";
            rsp.parent = project
            rsp.instance_ip_back_refs = null
            String json = ApiSerializer.serializeObject("virtual-machine-interface", rsp);
            ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
            return response.getBody()
        }

        attachL3NetworkToVm {
            l3NetworkUuid = test_l3_2.uuid
            vmInstanceUuid = vm.uuid
        }
        VmInstanceInventory result = queryVmInstance {
            conditions = ["uuid=${vm.uuid}"]
        }[0]
        assert result.vmNics.size() == 2

        detachL3NetworkFromVm {
            vmNicUuid = vm.getVmNics().get(0).uuid
        }
        result = queryVmInstance {
            conditions = ["uuid=${vm.uuid}"]
        }[0]
        assert result.vmNics.size() == 1

        destroyVmInstance {
            uuid = vm.uuid
        }

        expungeVmInstance{
            uuid = vm.uuid
        }

        deleteL3Network {
            delegate.uuid = test_l3_1.uuid
        }

        deleteL3Network {
            delegate.uuid = test_l3_2.uuid
        }

        l3TfNetworks = queryL3Network {conditions=["type=" + SugonSdnControllerConstant.L3_TF_NETWORK_TYPE]}
        assert l3TfNetworks.size() == 0

        expectError {
            removeSdnController {
                uuid = sdn.uuid
            }
        }

        deleteL2Network {
            delegate.uuid = l2TfNetwork.uuid
        }

        l2TfNetworks = queryL2Network {conditions=["type=" + SugonSdnControllerConstant.L2_TF_NETWORK_TYPE]}
        assert l2TfNetworks.size() == 0

        removeSdnController {
            uuid = sdn.uuid
        }
    }
}
