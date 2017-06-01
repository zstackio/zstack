package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.service.NetworkServiceProviderType;

@PythonClass
public interface VirtualRouterConstant {
    @PythonClass
	public static final String VIRTUAL_ROUTER_PROVIDER_TYPE = "VirtualRouter";
    @PythonClass
	public static final String VIRTUAL_ROUTER_VM_TYPE = "VirtualRouter";
    @PythonClass
	public static final String VIRTUAL_ROUTER_OFFERING_TYPE = "VirtualRouter";

	public static final String ACTION_CATEGORY = "virtualRouter";

    public static final NetworkServiceProviderType PROVIDER_TYPE = new NetworkServiceProviderType(VIRTUAL_ROUTER_PROVIDER_TYPE);

	public static final String VR_RESULT_VM = "ResultVirtualRouterVm";
	public static final String VR_PORT_FORWARDING_RULE = "PortForwardingRule";

	public static final String SERVICE_ID = "virtualRouter";

	public static final String VR_ECHO_PATH = "/echo";
	public static final String VR_CONFIGURE_NIC_PATH = "/configurenic";
	public static final String VR_REMOVE_NIC_PATH = "/removenic";
	public static final String VR_ADD_DHCP_PATH = "/adddhcp";
	public static final String VR_REMOVE_DHCP_PATH = "/removedhcp";
	public static final String VR_SET_SNAT_PATH = "/setsnat";
    public static final String VR_SYNC_SNAT_PATH = "/syncsnat";
	public static final String VR_REMOVE_SNAT_PATH = "/removesnat";
	public static final String VR_REMOVE_DNS_PATH = "/removedns";
	public static final String VR_SET_DNS_PATH = "/setdns";
	public static final String VR_CREATE_PORT_FORWARDING = "/createportforwarding";
	public static final String VR_REVOKE_PORT_FORWARDING = "/revokeportforwarding";
	public static final String VR_SYNC_PORT_FORWARDING = "/syncportforwarding";
    public static final String VR_CREATE_EIP = "/createeip";
    public static final String VR_REMOVE_EIP = "/removeeip";
    public static final String VR_SYNC_EIP = "/synceip";
    public static final String VR_INIT = "/init";
	public static final String VR_PING = "/ping";

	public static final String VR_CREATE_VIP = "/createvip";
	public static final String VR_REMOVE_VIP = "/removevip";

	public static final String VR_KVM_CREATE_BOOTSTRAP_ISO_PATH = "/virtualrouter/createbootstrapiso";
	public static final String VR_KVM_DELETE_BOOTSTRAP_ISO_PATH = "/virtualrouter/deletebootstrapiso";

	public static final String VR_VIP = "VirtualRouterVip";
    public static final String VR_VIP_L3NETWORK = "VirtualRouterVipL3Network";

    public static final String ANSIBLE_PLAYBOOK_NAME = "virtualrouter.py";
    public static final String ANSIBLE_MODULE_PATH = "ansible/virtualrouter";

    public static enum Param {
        VR,
        VR_UUID,
        IS_NEW_CREATED,
        IS_RECONNECT,
        VIPS,
    }
}
