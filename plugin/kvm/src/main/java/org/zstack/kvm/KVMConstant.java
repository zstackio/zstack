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
    String KVM_UPDATE_HOST_CONFIGURATION_PATH = "/host/update/configuration";
    String KVM_ECHO_PATH = "/host/echo";
    String KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH = "/network/checkphysicalnetworkinterface";
    String KVM_ADD_INTERFACE_TO_BRIDGE_PATH = "/network/bridge/addif";
    String KVM_HOST_CAPACITY_PATH = "/host/capacity";
    String KVM_HOST_FACT_PATH = "/host/fact";
    String KVM_HOST_CHECK_FILE_PATH = "/host/checkfile";
    String KVM_HOST_REPORT_DEVICE_EVENT_PATH = "/host/reportdeviceevent";
    String KVM_HOST_NUMA_PATH = "/numa/topology";
    String KVM_REALIZE_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/createbridge";
    String KVM_CHECK_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/checkbridge";
    String KVM_REALIZE_L2VLAN_NETWORK_PATH = "/network/l2vlan/createbridge";
    String KVM_CHECK_L2VLAN_NETWORK_PATH = "/network/l2vlan/checkbridge";
    String KVM_DELETE_L2NOVLAN_NETWORK_PATH = "/network/l2novlan/deletebridge";
    String KVM_DELETE_L2VLAN_NETWORK_PATH = "/network/l2vlan/deletebridge";
    String KVM_REALIZE_OVSDPDK_NETWORK_PATH = "/network/ovsdpdk/createbridge";
    String KVM_CHECK_OVSDPDK_NETWORK_PATH = "/network/ovsdpdk/checkbridge";
    String KVM_DELETE_OVSDPDK_NETWORK_PATH = "/network/ovsdpdk/deletebridge";
    String KVM_GENERATE_VDPA_PATH = "/network/ovsdpdk/generatevdpa";
    String KVM_DELETE_VDPA_PATH = "/network/ovsdpdk/deletevdpa";
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
    String KVM_VM_ONLINE_INCREASE_CPU = "/vm/increase/cpu";
    String KVM_VM_ONLINE_INCREASE_MEMORY = "/vm/increase/mem";
    String KVM_VM_SYNC_PATH = "/vm/vmsync";
    String KVM_ATTACH_VOLUME = "/vm/attachdatavolume";
    String KVM_DETACH_VOLUME = "/vm/detachdatavolume";
    String KVM_ATTACH_NIC_PATH = "/vm/attachnic";
    String KVM_DETACH_NIC_PATH = "/vm/detachnic";
    String KVM_UPDATE_NIC_PATH = "/vm/updatenic";
    String KVM_VM_CHECK_STATE = "/vm/checkstate";
    String KVM_VM_UPDATE_PRIORITY_PATH = "/vm/priority";
    String KVM_TAKE_VOLUME_SNAPSHOT_PATH = "/vm/volume/takesnapshot";
    String KVM_CHECK_VOLUME_SNAPSHOT_PATH = "/vm/volume/checksnapshot";
    String KVM_MERGE_SNAPSHOT_PATH = "/vm/volume/mergesnapshot";
    String KVM_LOGOUT_ISCSI_PATH = "/iscsi/target/logout";
    String KVM_LOGIN_ISCSI_PATH = "/iscsi/target/login";
    String KVM_HARDEN_CONSOLE_PATH = "/vm/console/harden";
    String KVM_DELETE_CONSOLE_FIREWALL_PATH = "/vm/console/deletefirewall";
    String KVM_UPDATE_HOST_OS_PATH = "/host/updateos";
    String KVM_HOST_UPDATE_DEPENDENCY_PATH = "/host/updatedependency";
    String HOST_SHUTDOWN = "/host/shutdown";
    String HOST_UPDATE_SPICE_CHANNEL_CONFIG_PATH = "/host/updateSpiceChannelConfig";
    String KVM_GET_VM_FIRST_BOOT_DEVICE_PATH = "/vm/getfirstbootdevice";
    String GET_VM_DEVICE_ADDRESS_PATH = "/vm/getdeviceaddress";
    String KVM_SCAN_VM_PORT_STATUS = "/host/vm/scanport";
    String GET_DEV_CAPACITY = "/host/dev/capacity";
    String KVM_CONFIG_PRIMARY_VM_PATH = "/primary/vm/config";
    String KVM_CONFIG_SECONDARY_VM_PATH = "/secondary/vm/config";
    String KVM_START_COLO_SYNC_PATH = "/start/colo/sync";
    String KVM_REGISTER_PRIMARY_VM_HEARTBEAT = "/register/primary/vm/heartbeat";

    String ISO_TO = "kvm.isoto";
    String ANSIBLE_PLAYBOOK_NAME = "kvm.py";
    String ANSIBLE_MODULE_PATH = "ansible/kvm";

    String MIN_LIBVIRT_LIVESNAPSHOT_VERSION = "1.0.0";
    String MIN_QEMU_LIVESNAPSHOT_VERSION = "1.3.0";
    String MIN_LIBVIRT_LIVE_BLOCK_COMMIT_VERSION = "1.2.7";
    String MIN_LIBVIRT_VIRTIO_SCSI_VERSION = "1.0.4";

    String KVM_REPORT_VM_STATE = "/kvm/reportvmstate";
    String KVM_RECONNECT_ME = "/kvm/reconnectme";
    String KVM_REPORT_PS_STATUS = "/kvm/reportstoragestatus";
    String KVM_REPORT_SELF_FENCER = "/kvm/reportselffencer";
    String KVM_REQUEST_MAINTAIN_HOST = "/kvm/requestmaintainhost";
    String KVM_ANSIBLE_LOG_PATH_FROMAT = "/kvm/ansiblelog/{uuid}";
    String KVM_REPORT_VM_SHUTDOWN_EVENT = "/kvm/reportvmshutdown";
    String KVM_REPORT_VM_REBOOT_EVENT = "/kvm/reportvmreboot";
    String KVM_REPORT_VM_CRASH_EVENT = "/kvm/reportvmcrash";

    String KVM_TRANSMIT_VM_OPERATION_TO_MN = "/host/transmitvmoperation";
    String KVM_HOST_PHYSICAL_NIC_ALARM_EVENT = "/host/physicalNic/alarm";

    String KVM_AGENT_OWNER = "kvm";

    String ALI_REPO = "ali";
    String NETEASE_REPO = "163";

    String KVM_HOST_ADDONS = "kvmHostAddons";

    String CPU_MODE_NONE = "none";
    String CPU_MODE_CUSTOM = "custom";
    String CPU_MODE_HOST_MODEL = "host-model";
    String CPU_MODE_HOST_PASSTHROUGH = "host-passthrough";

    String IPTABLES_COMMENTS = "kvmagent.allow.port";

    Integer DEFAULT_MAX_NIC_QUEUE_NUMBER = 12;

    String CONNECT_HOST_PRIMARYSTORAGE_ERROR = "psError";

    enum KvmVmState {
        NoState,
        Running,
        Paused,
        Shutdown,
        Crashed,
        Suspended;

        public static KvmVmState fromVmInstanceState(VmInstanceState state) {
            if (state == VmInstanceState.Running) {
                return Running;
            } else if (state == VmInstanceState.Stopped) {
                return null;
            } else if (state == VmInstanceState.Paused) {
                return Paused;
            } else {
                return null;
            }
        }

        public VmInstanceState toVmInstanceState() {
            if (this == Running) {
                return VmInstanceState.Running;
            } else if (this == Shutdown) {
                return VmInstanceState.Stopped;
            } else if (this == Paused) {
                return VmInstanceState.Paused;
            } else if (this == Crashed) {
                return VmInstanceState.Crashed;
            } else {
                return VmInstanceState.Unknown;
            }
        }
    }
}
