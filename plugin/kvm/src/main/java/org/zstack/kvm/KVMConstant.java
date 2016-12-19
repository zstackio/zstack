package org.zstack.kvm;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.vm.VmInstanceState;

@PythonClass
public interface KVMConstant {
    String SERVICE_ID = "kvm";

    @PythonClass
    String KVM_HYPERVISOR_TYPE = "KVM";

    String KVM_CONNECT_PATH = "/host/connect";
    String KVM_PING_PATH = "/host/ping";
    String KVM_ECHO_PATH = "/host/echo";
    String KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH = "/network/checkphysicalnetworkinterface";
    String KVM_HOST_CAPACITY_PATH = "/host/capacity";
    String KVM_HOST_FACT_PATH = "/host/fact";
    String KVM_REALIZE_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/createbridge";
    String KVM_CHECK_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/checkbridge";
    String KVM_REALIZE_L2VLAN_NETWORK_PATH = "/network/l2vlan/createbridge";
    String KVM_CHECK_L2VLAN_NETWORK_PATH = "/network/l2vlan/checkbridge";
    String KVM_ATTACH_ISO_PATH = "/vm/iso/attach";
    String KVM_DETACH_ISO_PATH = "/vm/iso/detach";
    String KVM_START_VM_PATH = "/vm/start";
    String KVM_STOP_VM_PATH = "/vm/stop";
    String KVM_PAUSE_VM_PATH = "/vm/pause";
    String KVM_RESUME_VM_PATH = "/vm/resume";
    String KVM_REBOOT_VM_PATH = "/vm/reboot";
    String KVM_DESTROY_VM_PATH = "/vm/destroy";
    String KVM_MIGRATE_VM_PATH = "/vm/migrate";
    String KVM_GET_VNC_PORT_PATH = "/vm/getvncport";
    String KVM_VM_ONLINE_CHANGE_CPUMEMORY = "/vm/online/changecpumem";
    String KVM_VM_SYNC_PATH = "/vm/vmsync";
    String KVM_ATTACH_VOLUME = "/vm/attachdatavolume";
    String KVM_DETACH_VOLUME = "/vm/detachdatavolume";
    String KVM_ATTACH_NIC_PATH = "/vm/attachnic";
    String KVM_DETACH_NIC_PATH = "/vm/detachnic";
    String KVM_VM_CHECK_STATE = "/vm/checkstate";
    String KVM_TAKE_VOLUME_SNAPSHOT_PATH = "/vm/volume/takesnapshot";
    String KVM_MERGE_SNAPSHOT_PATH = "/vm/volume/mergesnapshot";
    String KVM_LOGOUT_ISCSI_PATH = "/iscsi/target/logout";
    String KVM_LOGIN_ISCSI_PATH = "/iscsi/target/login";
    String KVM_HARDEN_CONSOLE_PATH = "/vm/console/harden";
    String KVM_DELETE_CONSOLE_FIREWALL_PATH = "/vm/console/deletefirewall";
    String ISO_TO = "kvm.isoto";
    String ANSIBLE_PLAYBOOK_NAME = "kvm.py";
    String ANSIBLE_MODULE_PATH = "ansible/kvm";

    String MIN_LIBVIRT_LIVESNAPSHOT_VERSION = "1.0.0";
    String MIN_QEMU_LIVESNAPSHOT_VERSION = "1.3.0";
    String MIN_LIBVIRT_LIVE_BLOCK_COMMIT_VERSION = "1.2.7";
    String MIN_LIBVIRT_VIRTIO_SCSI_VERSION = "1.0.4";

    String KVM_REPORT_VM_STATE = "/kvm/reportvmstate";
    String KVM_RECONNECT_ME = "/kvm/reconnectme";
    String KVM_ANSIBLE_LOG_PATH_FROMAT = "/kvm/ansiblelog/{uuid}";

    String KVM_AGENT_OWNER = "kvm";

    enum KvmVmState {
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
            } else if (this == Paused) {
                return VmInstanceState.Paused;
            } else {
                return VmInstanceState.Unknown;
            }
        }
    }
}
