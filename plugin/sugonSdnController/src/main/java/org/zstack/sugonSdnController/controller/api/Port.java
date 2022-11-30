package org.zstack.sugonSdnController.controller.api;

import java.util.List;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("serial")
public class Port extends VRouterApiObjectBase  {

    private static final String NONE = "None";

    @SerializedName("id") private String id  = NONE; // VMI id
    @SerializedName("instance-id") private String instance_id = NONE ; // VM id
    @SerializedName("display-name") private String display_name = NONE; // VM display name
    @SerializedName("vn-id") private String vn_id = NONE; // VN id
    @SerializedName("ip-address") private String ip_address = NONE;
    @SerializedName("mac-address") private String mac_address = NONE;
    @SerializedName("vm-project-id") private String vm_project_id = NONE;
    @SerializedName("rx-vlan-id") private short rx_vlan_id = -1;
    @SerializedName("tx-vlan-id") private short tx_vlan_id = -1;
    @SerializedName("system-name") private String system_name = NONE; // tap interface name, required
    @SerializedName("type") private int type = 2; // must be set to 2 for Vcenter port
    @SerializedName("ip6-address") private String ip6_address = NONE;

    @Override
    public String getObjectType() {
        return "port";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain", "default-project");
    }

    @Override
    public String getDefaultParentType() {
        return "project";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstance_id() {
        return instance_id;
    }

    public void setInstance_id(String instance_id) {
        this.instance_id = instance_id;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getVn_id() {
        return vn_id;
    }

    public void setVn_id(String vn_id) {
        this.vn_id = vn_id;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }
    public String getMac_address() {
        return mac_address;
    }

    public void setMac_address(String mac_address) {
        this.mac_address = mac_address;
    }
    public String getVm_project_id() {
        return vm_project_id;
    }

    public void setVm_project_id(String vm_project_id) {
        this.vm_project_id = vm_project_id;
    }

    public short getRx_vlan_id() {
        return rx_vlan_id;
    }

    public void setRx_vlan_id(short rx_vlan_id) {
        this.rx_vlan_id = rx_vlan_id;
    }

    public short getTx_vlan_id() {
        return tx_vlan_id;
    }

    public void setTx_vlan_id(short tx_vlan_id) {
        this.tx_vlan_id = tx_vlan_id;
    }

    public String getSystem_name() {
        return system_name;
    }

    public void setSystem_name(String system_name) {
        this.system_name = system_name;
    }

    public String getIp6_address() {
        return ip6_address;
    }

    public void setIp6_address(String ip6_address) {
        this.ip6_address = ip6_address;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
