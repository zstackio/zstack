package org.zstack.kvm;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.vm.VmInstanceState;

@PythonClass
public interface KVMConstant {
    public static final String SERVICE_ID = "kvm";

    @PythonClass
    public static final String KVM_HYPERVISOR_TYPE = "KVM";

    public static final String KVM_CONNECT_PATH = "/host/connect";
    public static final String KVM_PING_PATH = "/host/ping";
    public static final String KVM_ECHO_PATH = "/host/echo";
    public static final String KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH = "/network/checkphysicalnetworkinterface";
    public static final String KVM_HOST_CAPACITY_PATH = "/host/capacity";
    public static final String KVM_HOST_FACT_PATH = "/host/fact";
    public static final String KVM_REALIZE_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/createbridge";
    public static final String KVM_CHECK_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/checkbridge";
    public static final String KVM_REALIZE_L2VLAN_NETWORK_PATH = "/network/l2vlan/createbridge";
    public static final String KVM_CHECK_L2VLAN_NETWORK_PATH = "/network/l2vlan/checkbridge";
    public static final String KVM_ATTACH_ISO_PATH = "/vm/iso/attach";
    public static final String KVM_DETACH_ISO_PATH = "/vm/iso/detach";
    public static final String KVM_START_VM_PATH = "/vm/start";
    public static final String KVM_STOP_VM_PATH = "/vm/stop";
    public static final String KVM_REBOOT_VM_PATH = "/vm/reboot";
    public static final String KVM_DESTROY_VM_PATH = "/vm/destroy";
    public static final String KVM_MIGRATE_VM_PATH = "/vm/migrate";
    public static final String KVM_GET_VNC_PORT_PATH = "/vm/getvncport";
    public static final String KVM_VM_CHANGE_CPUMEMORY = "/vm/changecpumem";
    public static final String KVM_VM_SYNC_PATH = "/vm/vmsync";
    public static final String KVM_ATTACH_VOLUME = "/vm/attachdatavolume";
    public static final String KVM_DETACH_VOLUME = "/vm/detachdatavolume";
    public static final String KVM_ATTACH_NIC_PATH = "/vm/attachnic";
    public static final String KVM_DETACH_NIC_PATH = "/vm/detachnic";
    public static final String KVM_VM_CHECK_STATE = "/vm/checkstate";
    public static final String KVM_VM_CHANGE_PASSWORD_PATH = "/vm/changepasswd";
    public static final String KVM_TAKE_VOLUME_SNAPSHOT_PATH = "/vm/volume/takesnapshot";
    public static final String KVM_MERGE_SNAPSHOT_PATH = "/vm/volume/mergesnapshot";
    public static final String KVM_LOGOUT_ISCSI_PATH = "/iscsi/target/logout";
    public static final String KVM_LOGIN_ISCSI_PATH = "/iscsi/target/login";
    public static final String KVM_HARDEN_CONSOLE_PATH = "/vm/console/harden";
    public static final String KVM_DELETE_CONSOLE_FIREWALL_PATH = "/vm/console/deletefirewall";
    public static final String ISO_TO = "kvm.isoto";
    public static final String ANSIBLE_PLAYBOOK_NAME = "kvm.py";
    public static final String ANSIBLE_MODULE_PATH = "ansible/kvm";

    public static final String MIN_LIBVIRT_LIVESNAPSHOT_VERSION = "1.0.0";
    public static final String MIN_QEMU_LIVESNAPSHOT_VERSION = "1.3.0";
    public static final String MIN_LIBVIRT_LIVE_BLOCK_COMMIT_VERSION = "1.2.7";
    public static final String MIN_LIBVIRT_VIRTIO_SCSI_VERSION = "1.0.4";

    public static final String KVM_REPORT_VM_STATE = "/kvm/reportvmstate";
    public static final String KVM_RECONNECT_ME = "/kvm/reconnectme";
    public static final String KVM_ANSIBLE_LOG_PATH_FROMAT = "/kvm/ansiblelog/{uuid}";

    public static final String KVM_AGENT_OWNER = "kvm";

    public static enum KvmVmState {
    	NoState,
    	Running,
    	Paused,
    	Shutdown,
    	Crashed,
    	Suspended;

    	public VmInstanceState toVmInstanceState() {
    		if (this == Running) {
    			return VmInstanceState.Running;
    		} else if (this == Shutdown) {
    			return VmInstanceState.Stopped;
    		} else {
    			return VmInstanceState.Unknown;
    		}
    	}
    }
}
