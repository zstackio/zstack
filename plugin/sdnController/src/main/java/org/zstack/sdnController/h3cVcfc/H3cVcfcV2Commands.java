package org.zstack.sdnController.h3cVcfc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boce.wang on 04/28/2025.
 */
public class H3cVcfcV2Commands extends H3cVcfcCommands {

    public static final String H3C_VCFC_SUBNETS = "/vds/1.0/subnets";
    public static final String H3C_VCFC_ROUTERS = "/vds/1.0/routers";
    public static final String H3C_VCFC_VDS = "/vds/1.0/h3c_vdsconf";

    public static class NetworkCmd extends H3cVcfcCommands.NetworkCmd {
        String tenant_name;
        String vds_name;
    }

    public static class CreateH3cNetworksCmd extends H3cCmd {
        List<H3cVcfcV2Commands.NetworkCmd> networks = new ArrayList<>();
    }

    public static class CreateH3cNetworksRsp extends H3cRsp {
        List<H3cVcfcV2Commands.NetworkCmd> networks = new ArrayList<>();
    }

    public static class H3cTenantStruct extends H3cVcfcCommands.H3cTenantStruct {
        public List<String> vds_list;
        public String cloud_region_name;
        public String cloud_domain_name;

        public H3cTenantStruct() {
            vds_list = new ArrayList<>();
        }
    }

    public static class GetH3cTenantsRsp extends H3cRsp {
        public List<H3cVcfcV2Commands.H3cTenantStruct> tenants;
    }

    public static class AllocationPoolStruct {
        public String start;
        public String end;
    }

    public static class SubnetCmd {
        String id;
        String name;
        String network_id;
        String nqa_profile_id;
        String segment_id;
        String cidr;
        String gateway_ip;
        Boolean enable_dhcp;
        List<AllocationPoolStruct> allocation_pools;
        List<String> dns_nameservers;
        List<String> service_types;
        String cloud_region_name;
        List<String> protocal_ips;
        String tenant_name;
        String vds_name;
    }

    public static class CreateH3cSubnetsCmd extends H3cCmd {
        public List<SubnetCmd> subnets = new ArrayList<>();
    }

    public static class CreateH3cSubnetsRsp extends H3cRsp {
        public List<SubnetCmd> subnets = new ArrayList<>();
    }

    public static class GetH3cSubnetsCmd extends H3cCmd {
        public String network_id;
    }
    public static class GetH3cSubnetsRsp extends H3cRsp {
        public List<SubnetCmd> subnets = new ArrayList<>();
    }

    public static class RouterCmd {
        String id;
        String name;
        String tenant_id;
    }

    public static class CreateH3cRoutersCmd extends H3cCmd {
        public List<RouterCmd> routers = new ArrayList<>();
    }

    public static class CreateH3cRoutersRsp extends H3cRsp {
        public List<RouterCmd> routers = new ArrayList<>();
    }

    public static class DeleteH3cRoutersCmd extends H3cCmd {
    }

    public static class DeleteH3cRoutersRsp extends H3cRsp {
    }

    public static class AddRouterInterfaceCmd extends H3cCmd {
        public String subnet_id;
    }

    public static class AddRouterInterfaceRsp extends H3cRsp {
        public String id;
        public String subnet_id;
        public String tenant_id;
        public String port_id;
    }

    public static class RemoveRouterInterfaceCmd extends H3cCmd {
        public String subnet_id;
    }

    public static class RemoveRouterInterfaceRsp extends H3cRsp {
    }

    public static class H3cVdsStruct {
        public String uuid;
        public String name;
        public String bridge;
        public String status;
        public String openflow_hard_age;
        public String vxlan_tunnel_name;
        public String vxlan_range;
        public String virtual_mac;
        public String forwarding_mode;
    }

    public static class GetH3cVdsCmd extends H3cCmd {
    }

    public static class GetH3cVdsRsp extends H3cRsp {
        public List<H3cVdsStruct> vds;
    }
}
