package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.zstack.sdk.SdnControllerInventory
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.LoginReply
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.LoginRsp
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.CreateH3cNetworksRsp
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.NetworkCmd
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.GetH3cVniRangeRsp
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.H3cVniRangeStruct
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.VniRangeStruct
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.GetH3cTenantsRsp
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.H3cTenantStruct
import org.zstack.sdnController.h3cVcfc.H3cVcfcCommands.GetH3cTeamLederIpReply
import org.zstack.sdnController.h3cVcfc.H3cVcfcV2Commands
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant
import org.zstack.sugonSdnController.controller.api.ApiSerializer
import org.zstack.sugonSdnController.controller.api.TfCommands
import org.zstack.sugonSdnController.controller.api.types.Domain
import org.zstack.sugonSdnController.controller.api.types.MacAddressesType
import org.zstack.sugonSdnController.controller.api.types.Project
import org.zstack.sugonSdnController.controller.api.types.VirtualMachine
import org.zstack.sugonSdnController.controller.api.types.VirtualMachineInterface
import org.zstack.sugonSdnController.controller.api.types.VirtualNetwork

/**
 * Created by shixin.ruan on 2019/09/26.
 */
class SdnControllerSpec extends Spec implements HasSession {
    String vendorType
    String name
    String description
    String ip
    String userName
    String password
    List<String> systemTags

    SdnControllerInventory inventory

    SdnControllerSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addSdnController {
            delegate.resourceUuid = uuid
            delegate.sessionId = sessionId
            delegate.vendorType = vendorType
            delegate.name = name
            delegate.description = description
            delegate.ip = ip
            delegate.userName = userName
            delegate.password = password
            delegate.systemTags = systemTags
        }

        postCreate {
            inventory = JSONObjectUtil.rehashObject(querySdnController {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0], SdnControllerInventory.class)
        }

        return id(name, inventory.uuid)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(H3cVcfcCommands.H3C_VCFC_GET_TOKEN) {
                LoginReply reply = new LoginReply()
                LoginRsp login = new LoginRsp()
                reply.token = "token1"
                login.record = reply
                return login
            }

            simulator(H3cVcfcCommands.H3C_VCFC_L2_NETWORKS) {
                CreateH3cNetworksRsp rsp = new CreateH3cNetworksRsp()
                rsp.networks = new ArrayList()

                NetworkCmd cmd = new NetworkCmd()
                cmd.id = "123456"
                rsp.networks.add(cmd)

                return rsp
            }

            simulator(H3cVcfcCommands.H3C_VCFC_VNI_RANGES) {
                GetH3cVniRangeRsp rsp = new GetH3cVniRangeRsp()
                rsp.domains = new ArrayList<>()

                H3cVniRangeStruct ranges = new H3cVniRangeStruct()
                ranges.vlan_map_list = new ArrayList<>()

                VniRangeStruct s1 = new VniRangeStruct()
                s1.start_vxlan = "100"
                s1.end_vxlan = "200"
                ranges.vlan_map_list.add(s1)

                VniRangeStruct s2 = new VniRangeStruct()
                s2.start_vxlan = "300"
                s2.end_vxlan = "400"
                ranges.vlan_map_list.add(s2)

                rsp.domains.add(ranges)

                return rsp
            }

            simulator(H3cVcfcCommands.H3C_VCFC_TENANTS) {
                H3cVcfcV2Commands.GetH3cTenantsRsp rsp = new H3cVcfcV2Commands.GetH3cTenantsRsp()
                rsp.tenants = new ArrayList<>()

                H3cVcfcV2Commands.H3cTenantStruct t1 = new H3cVcfcV2Commands.H3cTenantStruct()
                t1.id = "03e01b37-8440-471a-aa8f-8d1fb8cc1381"
                t1.name = "Test"
                t1.type = "local-create"
                t1.vds_list = ["eb32cf5e-04e9-42ad-b64c-2c3f9bacd3cc"]
                t1.cloud_region_name = null
                t1.cloud_domain_name = null
                rsp.tenants.add(t1)

                H3cVcfcV2Commands.H3cTenantStruct t2 = new H3cVcfcV2Commands.H3cTenantStruct()
                t2.id = "ffffffff-0000-0000-0000-000000000001"
                t2.name = "default"
                t2.type = "default"
                t2.vds_list = ["ffffffff-0000-0000-0000-000000000001"]
                t2.cloud_region_name = null
                t2.cloud_domain_name = null
                rsp.tenants.add(t2)

                H3cVcfcV2Commands.H3cTenantStruct t3 = new H3cVcfcV2Commands.H3cTenantStruct()
                t3.id = "c9d49b6f-d2cd-4636-b9d4-be0f9c9c7783"
                t3.name = "sr"
                t3.type = "local-create"
                t3.vds_list = ["ffffffff-0000-0000-0000-000000000001"]
                t3.cloud_region_name = null
                t3.cloud_domain_name = null
                rsp.tenants.add(t3)

                return rsp
            }

            simulator(H3cVcfcCommands.H3C_VCFC_TEAM_LEADERIP) {
                GetH3cTeamLederIpReply rsp = new GetH3cTeamLederIpReply()
                rsp.ip = "127.1.1.1"
                return rsp
            }

            simulator(H3cVcfcV2Commands.H3C_VCFC_VDS) {
                H3cVcfcV2Commands.GetH3cVdsRsp rsp = new H3cVcfcV2Commands.GetH3cVdsRsp()
                rsp.vds = new ArrayList<>()

                H3cVcfcV2Commands.H3cVdsStruct vds1 = new H3cVcfcV2Commands.H3cVdsStruct()
                vds1.uuid = "eb32cf5e-04e9-42ad-b64c-2c3f9bacd3cc"
                vds1.name = "Test_VDS"
                vds1.bridge = "Test_VDS-br"
                vds1.status = "UP"
                vds1.openflow_hard_age = "300"
                vds1.vxlan_tunnel_name = "vxlan_Test_VDS-br"
                vds1.vxlan_range = "1-16777215"
                vds1.virtual_mac = "00:00:00:00:00:01"
                vds1.forwarding_mode = "mac-forwarding"
                rsp.vds.add(vds1)

                H3cVcfcV2Commands.H3cVdsStruct vds2 = new H3cVcfcV2Commands.H3cVdsStruct()
                vds2.uuid = "ffffffff-0000-0000-0000-000000000001"
                vds2.name = "Default_VDS"
                vds2.bridge = "Default_VDS-br"
                vds2.status = "UP"
                vds2.openflow_hard_age = "300"
                vds2.vxlan_tunnel_name = "vxlan_Default_VDS-br"
                vds2.vxlan_range = "1-16777215"
                vds2.virtual_mac = "00:00:00:00:00:02"
                vds2.forwarding_mode = "mac-forwarding"
                rsp.vds.add(vds2)

                return rsp
            }

            simulator(TfCommands.TF_GET_DAEMON) {
                TfCommands.GetDomainRsp rsp = new TfCommands.GetDomainRsp()
                rsp.uuid = TfCommands.TEST_DOMAIN_UUID
                return rsp
            }

            simulator(TfCommands.TF_GET_DAEMON_DETAIL) {
                Domain rsp = new Domain()
                rsp.uuid = TfCommands.TEST_DOMAIN_UUID
                rsp.name = SugonSdnControllerConstant.TF_DEFAULT_DOMAIN
                String json = ApiSerializer.serializeObject("domain", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_GET_PROJECT) {
                Project rsp = new Project();
                rsp.name = TfCommands.TEST_PROJECT_UUID
                rsp.uuid = TfCommands.TEST_PROJECT_UUID
                rsp.displayName = "admin";
                String json = ApiSerializer.serializeObject("project", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);

                return response.getBody()
            }

            simulator(TfCommands.TF_CREATE_PROJECT) {
                Project rsp = new Project();
                rsp.name = TfCommands.TEST_PROJECT_UUID
                rsp.uuid = TfCommands.TEST_PROJECT_UUID
                rsp.displayName = "admin";
                String json = ApiSerializer.serializeObject("project", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_CREATE_NETWORK) {
                VirtualNetwork rsp = new VirtualNetwork();
                rsp.name = TfCommands.TEST_L2_UUID
                rsp.uuid = TfCommands.TEST_L2_UUID
                String json = ApiSerializer.serializeObject("virtual-network", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_GET_NETWORK) {
                VirtualNetwork rsp = new VirtualNetwork();
                rsp.name = TfCommands.TEST_L2_UUID
                rsp.uuid = TfCommands.TEST_L2_UUID
                String json = ApiSerializer.serializeObject("virtual-network", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_CREATE_VM) {
                VirtualMachine rsp = new VirtualMachine();
                rsp.name = TfCommands.TEST_VM_UUID
                rsp.uuid = TfCommands.TEST_VM_UUID
                String json = ApiSerializer.serializeObject("virtual-machine", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_GET_VM) {
                VirtualMachine rsp = new VirtualMachine();
                rsp.name = TfCommands.TEST_VM_UUID
                rsp.uuid = TfCommands.TEST_VM_UUID
                String json = ApiSerializer.serializeObject("virtual-machine", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_CREATE_VMI) {
                VirtualMachineInterface rsp = new VirtualMachineInterface();
                rsp.name = TfCommands.TEST_VMI_UUID
                rsp.uuid = TfCommands.TEST_VMI_UUID
                Project project = new Project();
                project.name = TfCommands.TEST_PROJECT_UUID
                project.uuid = TfCommands.TEST_PROJECT_UUID
                project.displayName = "admin";
                rsp.setParent(project)
                String json = ApiSerializer.serializeObject("virtual-machine-interface", rsp);
                ResponseEntity<String> response = new ResponseEntity<String>(json, HttpStatus.OK);
                return response.getBody()
            }

            simulator(TfCommands.TF_GET_VMI) {
                VirtualMachineInterface rsp = new VirtualMachineInterface();
                rsp.name = TfCommands.TEST_VMI_UUID
                rsp.uuid = TfCommands.TEST_VMI_UUID
                List<String> macList = new ArrayList<String>();
                macList.add("08:00:27:b4:e1:99");
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
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            removeSdnController {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
