package org.zstack.testlib


import org.zstack.sdk.SdnControllerInventory
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
import org.zstack.testlib.*

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
            inventory = querySdnController {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
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
                GetH3cTenantsRsp rsp = new GetH3cTenantsRsp()
                rsp.tenants = new ArrayList<>()

                H3cTenantStruct t1 = new H3cTenantStruct()
                t1.type = "default"
                t1.name = "default"
                t1.id = "id1"
                rsp.tenants.add(t1)

                H3cTenantStruct t2 = new H3cTenantStruct()
                t2.type = "zstack"
                t2.name = "zstack"
                t2.id = "id2"
                rsp.tenants.add(t2)

                return rsp
            }

            simulator(H3cVcfcCommands.H3C_VCFC_TEAM_LEADERIP) {
                GetH3cTeamLederIpReply rsp = new GetH3cTeamLederIpReply()
                rsp.ip = "127.1.1.1"
                return rsp
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
