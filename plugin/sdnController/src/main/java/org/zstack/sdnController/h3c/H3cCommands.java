package org.zstack.sdnController.h3c;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class H3cCommands {
    public static final String H3C_VCFC_GET_TOKEN = "/sdn/v2.0/auth";
    public static final String H3C_VCFC_L2_NETWORKS = "/vds/1.0/networks";
    public static final String H3C_VCFC_VNI_RANGES = "/nem/v1.0/vlan_domains";
    public static final String H3C_VCFC_TENANTS = "/tenant/v1.0/tenants";
    public static final String H3C_VCFC_TEAM_LEADERIP = "/sdn/v2.0/team/leaderip";

    public static class H3cCmd {
        public String toString() {
            return JSONObjectUtil.toJsonString(this);
        }
    }

    public static class H3cRsp {
    }

    public static class LoginCmd {
        public String user;
        public String password;
        public String domain;
    }

    public static class GetH3cTokenCmd extends H3cCmd {
        public LoginCmd login;
    }

    public static class LoginReply {
        public String token;
        public String userName;
        public String domainName;
    }

    public static class LoginRsp extends H3cRsp {
        public LoginReply record;
    }

    public static class NetworkCmd {
        String id;
        String name;
        String tenant_id;
        Boolean distributed;
        String network_type;
        String original_network_type;
        String domain;
        Integer segmentation_id;
        Boolean external;
        Boolean force_flat;
    }

    public static class CreateH3cNetworksCmd extends H3cCmd{
        List<NetworkCmd> networks = new ArrayList<>();
    }

    public static class CreateH3cNetworksRsp extends H3cRsp{
        List<NetworkCmd> networks = new ArrayList<>();
    }

    public static class DeleteH3cNetworksCmd extends H3cCmd{
    }

    public static class DeleteH3cNetworksRsp extends H3cRsp{
    }

    public static class GetH3cVniRangeCmd extends H3cCmd {
    }

    public static class VniRangeStruct {
        public String start_vlan;
        public String end_vlan;
        public String start_vxlan;
        public String end_vxlan;
        public String access_mode;
    }

    public static class H3cVniRangeStruct {
        public String type;
        public String id;
        public String name;
        public List<VniRangeStruct> vlan_map_list;
    }
    public static class GetH3cVniRangeRsp extends H3cRsp {
        public List<H3cVniRangeStruct> domains;
    }


    public static class GetH3cTenantsCmd extends H3cCmd {
    }
    public static class H3cTenantStruct {
        public String id;
        public String name;
        public String type;
    }
    public static class GetH3cTenantsRsp extends H3cRsp {
        public List<H3cTenantStruct> tenants;
    }

    public static class GetH3cTeamLederIpCmd extends H3cCmd {
    }

    public static class GetH3cTeamLederIpReply {
        public String ip;
    }
}

