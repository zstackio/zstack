package org.zstack.simulator.sdnController
//package org.zstack.simulator
//
//import org.zstack.sdnController.h3c.H3cCommands
//import org.zstack.simulator2.Simulator
//
///**
// * Created by shixin.ruan on 2019/9/26.
// */
//class org.zstack.simulator.sdnController.SdnControllerAgent extends Agent {
//    org.zstack.simulator.sdnController.SdnControllerAgent(Simulator simulator) {
//        super(simulator)
//    }
//
//    @Override
//    void setupAgentHandler() {
//        handle(H3cCommands.H3C_VCFC_GET_TOKEN) {
//            H3cCommands.LoginReply reply = new H3cCommands.LoginReply()
//            H3cCommands.LoginRsp login = new H3cCommands.LoginRsp()
//            reply.token = "token2"
//            login.record = reply
//            return login
//        }
//
//        handle(H3cCommands.H3C_VCFC_L2_NETWORKS) {
//            H3cCommands.CreateH3cNetworksRsp rsp = new H3cCommands.CreateH3cNetworksRsp()
//            rsp.networks = new ArrayList()
//
//            H3cCommands.NetworkCmd cmd = new H3cCommands.NetworkCmd()
//            cmd.id = "123456"
//            rsp.networks.add(cmd)
//
//            return rsp
//        }
//
//        handle(H3cCommands.H3C_VCFC_VNI_RANGES) {
//            H3cCommands.GetH3cVniRangeRsp rsp = new H3cCommands.GetH3cVniRangeRsp()
//            rsp.domains = new ArrayList<>()
//
//            H3cCommands.H3cVniRangeStruct ranges = new H3cCommands.H3cVniRangeStruct()
//            ranges.vlan_map_list = new ArrayList<>()
//
//            H3cCommands.VniRangeStruct s1 = new H3cCommands.VniRangeStruct()
//            s1.start_vxlan = "100"
//            s1.end_vxlan = "200"
//            ranges.vlan_map_list.add(s1)
//
//            H3cCommands.VniRangeStruct s2 = new H3cCommands.VniRangeStruct()
//            s2.start_vxlan = "300"
//            s2.end_vxlan = "400"
//            ranges.vlan_map_list.add(s2)
//
//            rsp.domains.add(ranges)
//            return rsp
//        }
//
//        handle(H3cCommands.H3C_VCFC_TENANTS) {
//            H3cCommands.GetH3cTenantsRsp rsp = new H3cCommands.GetH3cTenantsRsp()
//            rsp.tenants = new ArrayList<>()
//
//            H3cCommands.H3cTenantStruct t1 = new H3cCommands.H3cTenantStruct()
//            t1.type = "default"
//            t1.name = "default"
//            t1.id = "id1"
//            rsp.tenants.add(t1)
//
//            H3cCommands.H3cTenantStruct t2 = new H3cCommands.H3cTenantStruct()
//            t2.type = "zstack"
//            t2.name = "zstack"
//            t2.id = "id2"
//            rsp.tenants.add(t2)
//            return rsp
//        }
//
//        handle(H3cCommands.H3C_VCFC_TEAM_LEADERIP) {
//            H3cCommands.GetH3cTeamLederIpReply rsp = new H3cCommands.GetH3cTeamLederIpReply()
//            rsp.ip = "127.1.1.1"
//            return rsp
//        }
//    }
//}
