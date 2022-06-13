package org.zstack.kvm;

import org.zstack.header.vm.devices.PciAddressConfig;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.core.validation.ConditionalValidation;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.CancelCommand;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.VmNicRedirectConfig;
import org.zstack.header.log.NoLogging;
import org.zstack.header.vm.PriorityConfigStruct;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmPriorityConfigVO;
import org.zstack.network.securitygroup.SecurityGroupMembersTO;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;

import java.io.Serializable;
import java.util.*;

public class KVMAgentCommands {
    public enum BootDev {
        hd(VmBootDevice.HardDisk),
        cdrom(VmBootDevice.CdRom),
        network(VmBootDevice.Network);

        private VmBootDevice device;

        BootDev(VmBootDevice dev) {
            device = dev;
        }

        public VmBootDevice toVmBootDevice() {
            return device;
        }
    }

    public static class AgentResponse implements ConditionalValidation {
        private boolean success = true;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
            this.success = false;
        }

        @Override
        public boolean needValidation() {
            return success;
        }
    }

    public static class AgentCommand {
        public LinkedHashMap kvmHostAddons;
    }

    public static class CheckVmStateCmd extends AgentCommand {
        public List<String> vmUuids;
        public String hostUuid;
    }

    public static class CheckVmStateRsp extends AgentResponse {
        public Map<String, String> states;
    }

    public static class UpdateVmPriorityCmd extends AgentCommand {
        public List<PriorityConfigStruct> priorityConfigStructs;
    }

    public static class UpdateVmPriorityRsp extends AgentResponse {
    }

    public static class DetachNicCommand extends AgentCommand {
        private String vmUuid;
        private NicTO nic;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public NicTO getNic() {
            return nic;
        }

        public void setNic(NicTO nic) {
            this.nic = nic;
        }
    }

    public static class DetachNicRsp extends AgentResponse {

    }

    public static class AttachNicCommand extends AgentCommand {
        private String vmUuid;
        private NicTO nic;
        private Map addons = new HashMap();

        public Map getAddons() {
            return addons;
        }

        public void setAddons(Map addons) {
            this.addons = addons;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public NicTO getNic() {
            return nic;
        }

        public void setNic(NicTO nic) {
            this.nic = nic;
        }
    }

    public static class AttachNicResponse extends AgentResponse {
        List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class UpdateNicCmd extends AgentCommand implements VmAddOnsCmd {
        private String vmInstanceUuid;
        private List<KVMAgentCommands.NicTO> nics;
        private Map<String, Object> addons = new HashMap<>();

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }

        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }

        @Override
        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class UpdateNicRsp extends AgentResponse {

    }

    public static class ConnectCmd extends AgentCommand {
        private String hostUuid;
        private String sendCommandUrl;
        private List<String> iptablesRules;
        private boolean ignoreMsrs;
        private boolean pageTableExtensionDisabled;
        private int tcpServerPort;
        private String version;

        public boolean isIgnoreMsrs() {
            return ignoreMsrs;
        }

        public void setIgnoreMsrs(boolean ignoreMsrs) {
            this.ignoreMsrs = ignoreMsrs;
        }

        public List<String> getIptablesRules() {
            return iptablesRules;
        }

        public void setIptablesRules(List<String> iptablesRules) {
            this.iptablesRules = iptablesRules;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public boolean isPageTableExtensionDisabled() {
            return pageTableExtensionDisabled;
        }

        public void setPageTableExtensionDisabled(boolean pageTableExtensionDisabled) {
            this.pageTableExtensionDisabled = pageTableExtensionDisabled;
        }

        public int getTcpServerPort() {
            return tcpServerPort;
        }

        public void setTcpServerPort(int tcpServerPort) {
            this.tcpServerPort = tcpServerPort;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ConnectResponse extends AgentResponse {
        private String libvirtVersion;
        private String qemuVersion;
        private boolean iptablesSucc;

        public boolean isIptablesSucc() {
            return iptablesSucc;
        }

        public void setIptablesSucc(boolean iptablesSucc) {
            this.iptablesSucc = iptablesSucc;
        }

        public String getLibvirtVersion() {
            return libvirtVersion;
        }

        public void setLibvirtVersion(String libvirtVersion) {
            this.libvirtVersion = libvirtVersion;
        }

        public String getQemuVersion() {
            return qemuVersion;
        }

        public void setQemuVersion(String qemuVersion) {
            this.qemuVersion = qemuVersion;
        }
    }

    public static class PingCmd extends AgentCommand {
        public String hostUuid;
    }

    public static class PingResponse extends AgentResponse {
        private String hostUuid;
        private String sendCommandUrl;
        private String version;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class UpdateHostConfigurationCmd extends AgentCommand {
        public String hostUuid;
        public String sendCommandUrl;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }
    }

    public static class UpdateHostConfigurationResponse extends AgentResponse {

    }

    public static class CheckPhysicalNetworkInterfaceCmd extends AgentCommand {
        private List<String> interfaceNames = new ArrayList<>(2);

        public CheckPhysicalNetworkInterfaceCmd addInterfaceName(String name) {
            interfaceNames.add(name);
            return this;
        }

        public List<String> getInterfaceNames() {
            return interfaceNames;
        }

        public void setInterfaceNames(List<String> interfaceNames) {
            this.interfaceNames = interfaceNames;
        }
    }

    public static class CheckPhysicalNetworkInterfaceResponse extends AgentResponse {
        private List<String> failedInterfaceNames;

        public List<String> getFailedInterfaceNames() {
            if (failedInterfaceNames == null) {
                failedInterfaceNames = new ArrayList<String>(0);
            }
            return failedInterfaceNames;
        }

        public void setFailedInterfaceNames(List<String> failedInterfaceNames) {
            this.failedInterfaceNames = failedInterfaceNames;
        }
    }

    public static class HostFactCmd extends AgentCommand {
    }

    public static class HostFactResponse extends AgentResponse {
        private String osDistribution;
        private String osVersion;
        private String osRelease;
        private String qemuImgVersion;
        private String libvirtVersion;
        private String hvmCpuFlag;
        private String eptFlag;
        private String cpuArchitecture;
        private String cpuModelName;
        private String cpuGHz;
        private String hostCpuModelName;
        private String systemProductName;
        private String systemSerialNumber;
        private List<String> ipAddresses;
        private List<String> libvirtCapabilities;

        public String getOsDistribution() {
            return osDistribution;
        }

        public void setOsDistribution(String osDistribution) {
            this.osDistribution = osDistribution;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getOsRelease() {
            return osRelease;
        }

        public void setOsRelease(String osRelease) {
            this.osRelease = osRelease;
        }

        public String getHvmCpuFlag() {
            return hvmCpuFlag;
        }

        public void setHvmCpuFlag(String hvmCpuFlag) {
            this.hvmCpuFlag = hvmCpuFlag;
        }

        public String getEptFlag() {
            return eptFlag;
        }

        public String getLibvirtVersion() {
            return libvirtVersion;
        }

        public void setLibvirtVersion(String libvirtVersion) {
            this.libvirtVersion = libvirtVersion;
        }

        public String getQemuImgVersion() {
            return qemuImgVersion;
        }

        public void setQemuImgVersion(String qemuImgVersion) {
            this.qemuImgVersion = qemuImgVersion;
        }

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }

        public String getCpuModelName() {
            return cpuModelName;
        }

        public void setCpuModelName(String cpuModelName) {
            this.cpuModelName = cpuModelName;
        }

        public String getCpuGHz() {
            return cpuGHz;
        }

        public String getCpuArchitecture() {
            return cpuArchitecture;
        }

        public String getHostCpuModelName() {
            return hostCpuModelName;
        }

        public String getSystemProductName() {
            return systemProductName;
        }

        public void setSystemProductName(String systemProductName) {
            this.systemProductName = systemProductName;
        }

        public String getSystemSerialNumber() {
            return systemSerialNumber;
        }

        public void setSystemSerialNumber(String systemSerialNumber) {
            this.systemSerialNumber = systemSerialNumber;
        }

        public List<String> getLibvirtCapabilities() {
            return libvirtCapabilities;
        }

        public void setLibvirtCapabilities(List<String> libvirtCapabilities) {
            this.libvirtCapabilities = libvirtCapabilities;
        }
    }

    public static class HostCapacityCmd extends AgentCommand {
    }

    public static class HostCapacityResponse extends AgentResponse {
        private long cpuNum;
        private long cpuSpeed;
        private long usedCpu;
        private long totalMemory;
        private long usedMemory;
        private int cpuSockets;

        public int getCpuSockets() {
            return cpuSockets;
        }

        public void setCpuSockets(int cpuSockets) {
            this.cpuSockets = cpuSockets;
        }

        public long getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(long cpuNum) {
            this.cpuNum = cpuNum;
        }

        public long getCpuSpeed() {
            return cpuSpeed;
        }

        public void setCpuSpeed(long cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
        }

        public long getUsedCpu() {
            return usedCpu;
        }

        public void setUsedCpu(long usedCpu) {
            this.usedCpu = usedCpu;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public void setTotalMemory(long totalMemory) {
            this.totalMemory = totalMemory;
        }

        public long getUsedMemory() {
            return usedMemory;
        }

        public void setUsedMemory(long usedMemory) {
            this.usedMemory = usedMemory;
        }
    }

    public static class DeleteBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;
        private String l2NetworkUuid;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }
    }




    public static class CreateBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;
        private String l2NetworkUuid;
        private Boolean disableIptables;
        private Integer mtu;

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public Boolean getDisableIptables() {
            return disableIptables;
        }

        public void setDisableIptables(Boolean disableIptables) {
            this.disableIptables = disableIptables;
        }

        public Integer getMtu() {
            return mtu;
        }

        public void setMtu(Integer mtu) {
            this.mtu = mtu;
        }
    }


    public static class CreateBridgeResponse extends AgentResponse {
    }

    public static class DeleteBridgeResponse extends AgentResponse {
    }
    public static class CheckBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }
    }

    public static class CheckBridgeResponse extends AgentResponse {
    }

    public static class CheckVlanBridgeCmd extends CheckBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }


    public static class CheckVlanBridgeResponse extends CheckBridgeResponse {
    }

    public static class AddInterfaceToBridgeCmd extends KVMAgentCommands.AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }
    }

    public static class AddInterfaceToBridgeResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class DeleteVlanBridgeCmd extends DeleteBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class DeleteVlanBridgeResponse extends DeleteBridgeResponse {
    }


    public static class CreateVlanBridgeCmd extends CreateBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class CreateVlanBridgeResponse extends CreateBridgeResponse {
    }

    public static class VHostAddOn {
        private int queueNum;
        private String rxBufferSize;
        private String txBufferSize;

        public int getQueueNum() {
            return queueNum;
        }

        public void setQueueNum(int queueNum) {
            this.queueNum = queueNum;
        }

        public String getRxBufferSize() {
            return rxBufferSize;
        }

        public void setRxBufferSize(String rxBufferSize) {
            this.rxBufferSize = rxBufferSize;
        }

        public String getTxBufferSize() {
            return txBufferSize;
        }

        public void setTxBufferSize(String txBufferSize) {
            this.txBufferSize = txBufferSize;
        }
    }

    public static class NicTO extends BaseVirtualPciDeviceTO {
        private String mac;
        private List<String> ips;
        private String bridgeName;
        // run `bridge fdb add NicTO.mac dev NicTO.physicalInterface` on vnics to allow vf <-> vnic communication
        private String physicalInterface;
        private String uuid;
        private String nicInternalName;
        private int deviceId;
        private String metaData;
        private Boolean useVirtio;
        private int bootOrder;
        private Integer mtu;
        private String driverType;
        private VHostAddOn vHostAddOn;
        private PciAddressConfig pci;
        private String type;

        // only for vf nic
        private String vlanId;
        private String pciDeviceAddress;

        public List<String> getIps() {
            return ips;
        }

        public void setIps(List<String> ips) {
            this.ips = ips;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public Boolean getUseVirtio() {
            return useVirtio;
        }

        public void setUseVirtio(Boolean useVirtio) {
            this.useVirtio = useVirtio;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public String getMetaData() {
            return metaData;
        }

        public void setMetaData(String metaData) {
            this.metaData = metaData;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }

        public int getBootOrder() {
            return bootOrder;
        }

        public void setBootOrder(int bootOrder) {
            this.bootOrder = bootOrder;
		}

        public String getVlanId() {
            return vlanId;
        }

        public void setVlanId(String vlanId) {
            this.vlanId = vlanId;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public Integer getMtu() {
            return mtu;
        }

        public void setMtu(Integer mtu) {
            this.mtu = mtu;
        }

        public String getDriverType() {
            return driverType;
        }

        public void setDriverType(String driverType) {
            this.driverType = driverType;
        }

        public VHostAddOn getvHostAddOn() {
            return vHostAddOn;
        }

        public void setvHostAddOn(VHostAddOn vHostAddOn) {
            this.vHostAddOn = vHostAddOn;
        }

        public PciAddressConfig getPci() {
            return pci;
        }

        public void setPci(PciAddressConfig pci) {
            this.pci = pci;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class VolumeSnapshotJobTO {
        public String vmInstanceUuid;
        public String installPath;
        public String previousInstallPath;
        public String newVolumeInstallPath;
        public String live;
        public String full;

        public VolumeSnapshotJobTO() {
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getPreviousInstallPath() {
            return previousInstallPath;
        }

        public void setPreviousInstallPath(String previousInstallPath) {
            this.previousInstallPath = previousInstallPath;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public String getLive() {
            return live;
        }

        public void setLive(String live) {
            this.live = live;
        }

        public String getFull() {
            return full;
        }

        public void setFull(String full) {
            this.full = full;
        }
    }

    public static class DetachDataVolumeCmd extends AgentCommand {
        private VolumeTO volume;
        private String vmInstanceUuid;

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getVmUuid() {
            return vmInstanceUuid;
        }

        public void setVmUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class DetachDataVolumeResponse extends AgentResponse {
    }

    public static class AttachDataVolumeCmd extends AgentCommand {
        private VolumeTO volume;
        private String vmInstanceUuid;
        private Map<String, Object> addons;

        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getVmUuid() {
            return vmInstanceUuid;
        }

        public void setVmUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class AttachDataVolumeResponse extends AgentResponse {
        List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class IsoTO {
        private String path;
        private String imageUuid;
        private int deviceId;

        public IsoTO() {
        }

        public IsoTO(IsoTO other) {
            this.path = other.path;
            this.imageUuid = other.imageUuid;
            this.deviceId = other.deviceId;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }
    }

    public static class CdRomTO extends BaseVirtualPciDeviceTO {
        private String path;
        private String imageUuid;
        private int deviceId;
        // unmounted iso
        private boolean isEmpty;
        private int bootOrder;

        public CdRomTO() {
        }

        public CdRomTO(CdRomTO other) {
            this.isEmpty = other.isEmpty;
            this.path = other.path;
            this.imageUuid = other.imageUuid;
            this.deviceId = other.deviceId;
            this.bootOrder = other.bootOrder;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            this.isEmpty = empty;
        }

        public int getBootOrder() {
            return bootOrder;
        }

        public void setBootOrder(int bootOrder) {
            this.bootOrder = bootOrder;
        }
    }

    public static class GenerateVdpaCmd extends AgentCommand {
        public String vmUuid;
        public List<NicTO> nics;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class GenerateVdpaResponse extends AgentResponse {
        public List<String> vdpaPaths;

        public List<String> getVdpaPaths() {
            return vdpaPaths;
        }

        public void setVdpaPaths(List<String> vdpaPaths) {
            this.vdpaPaths = vdpaPaths;
        }
    }

    public static class DeleteVdpaCmd extends AgentCommand {
        public String vmUuid;
        public String nicInternalName;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }
    }

    public static class DeleteVdpaRsp extends AgentResponse {
    }

    public static class HardenVmConsoleCmd extends AgentCommand {
        public String vmUuid;
        public Long vmInternalId;
        public String hostManagementIp;
    }

    public static class DeleteVmConsoleFirewallCmd extends AgentCommand {
        public String vmUuid;
        public Long vmInternalId;
        public String hostManagementIp;
    }

    public interface VmAddOnsCmd {
        Map<String, Object> getAddons();

        String getVmInstanceUuid();
    }

    public static class PriorityConfig {
        private String vmUuid;
        private int cpuShares;
        private int oomScoreAdj;

        PriorityConfig(VmPriorityConfigVO vo, String vmUuid) {
            this.vmUuid = vmUuid;
            this.cpuShares = vo.getCpuShares();
            this.oomScoreAdj = vo.getOomScoreAdj();
        }

        public int getCpuShares() {
            return cpuShares;
        }

        public void setCpuShares(int cpuShares) {
            this.cpuShares = cpuShares;
        }

        public int getOomScoreAdj() {
            return oomScoreAdj;
        }

        public void setOomScoreAdj(int oomScoreAdj) {
            this.oomScoreAdj = oomScoreAdj;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class vdiCmd extends AgentCommand implements Serializable {
        private String consoleMode;
        private String videoType;
        private String soundType;
        private String spiceStreamingMode;
        private Integer VDIMonitorNumber;
        @NoLogging
        private String consolePassword;
        private Map<String, String> qxlMemory;
        private List<String> spiceChannels;

        public String getConsoleMode() {
            return consoleMode;
        }

        public void setConsoleMode(String consoleMode) {
            this.consoleMode = consoleMode;
        }

        public String getVideoType() {
            return videoType;
        }

        public void setVideoType(String videoType) {
            this.videoType = videoType;
        }

        public String getSoundType() {
            return soundType;
        }

        public void setSoundType(String soundType) {
            this.soundType = soundType;
        }

        public String getSpiceStreamingMode() {
            return spiceStreamingMode;
        }

        public void setSpiceStreamingMode(String spiceStreamingMode) {
            this.spiceStreamingMode = spiceStreamingMode;
        }

        public Integer getVDIMonitorNumber() {
            return VDIMonitorNumber;
        }

        public void setVDIMonitorNumber(Integer VDIMonitorNumber) {
            this.VDIMonitorNumber = VDIMonitorNumber;
        }

        public String getConsolePassword() {
            return consolePassword;
        }

        public void setConsolePassword(String consolePassword) {
            this.consolePassword = consolePassword;
        }

        public Map<String, String> getQxlMemory() {
            return qxlMemory;
        }

        public void setQxlMemory(Map<String, String> qxlMemory) {
            this.qxlMemory = qxlMemory;
        }

        public List<String> getSpiceChannels() {
            return spiceChannels;
        }

        public void setSpiceChannels(List<String> spiceChannels) {
            this.spiceChannels = spiceChannels;
        }
    }

    public static class ConfigPrimaryVmCmd extends AgentCommand {
        private List<VmNicRedirectConfig> configs;
        private String vmInstanceUuid;
        private String hostIp;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public List<VmNicRedirectConfig> getConfigs() {
            return configs;
        }

        public void setConfigs(List<VmNicRedirectConfig> configs) {
            this.configs = configs;
        }

        public String getHostIp() {
            return hostIp;
        }

        public void setHostIp(String hostIp) {
            this.hostIp = hostIp;
        }
    }

    public static class RegisterPrimaryVmHeartbeatCmd extends AgentCommand {
        private String hostUuid;
        private String vmInstanceUuid;
        private Integer heartbeatPort;
        private String targetHostIp;
        private boolean coloPrimary;
        private Integer redirectNum;
        private List<VolumeTO> volumes;

        public Integer getRedirectNum() {
            return redirectNum;
        }

        public void setRedirectNum(Integer redirectNum) {
            this.redirectNum = redirectNum;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public Integer getHeartbeatPort() {
            return heartbeatPort;
        }

        public void setHeartbeatPort(Integer heartbeatPort) {
            this.heartbeatPort = heartbeatPort;
        }

        public String getTargetHostIp() {
            return targetHostIp;
        }

        public void setTargetHostIp(String targetHostIp) {
            this.targetHostIp = targetHostIp;
        }

        public boolean isColoPrimary() {
            return coloPrimary;
        }

        public void setColoPrimary(boolean coloPrimary) {
            this.coloPrimary = coloPrimary;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }
    }

    public static class StartColoSyncCmd extends AgentCommand {
        private String vmInstanceUuid;
        private Integer blockReplicationPort;
        private Integer nbdServerPort;
        private String secondaryVmHostIp;
        private Long checkpointDelay;
        private boolean fullSync;
        private List<VolumeTO> volumes = new ArrayList<>();
        private List<NicTO> nics = new ArrayList<>();

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public Integer getBlockReplicationPort() {
            return blockReplicationPort;
        }

        public void setBlockReplicationPort(Integer blockReplicationPort) {
            this.blockReplicationPort = blockReplicationPort;
        }

        public Integer getNbdServerPort() {
            return nbdServerPort;
        }

        public void setNbdServerPort(Integer nbdServerPort) {
            this.nbdServerPort = nbdServerPort;
        }

        public String getSecondaryVmHostIp() {
            return secondaryVmHostIp;
        }

        public void setSecondaryVmHostIp(String secondaryVmHostIp) {
            this.secondaryVmHostIp = secondaryVmHostIp;
        }

        public Long getCheckpointDelay() {
            return checkpointDelay;
        }

        public void setCheckpointDelay(Long checkpointDelay) {
            this.checkpointDelay = checkpointDelay;
        }

        public boolean isFullSync() {
            return fullSync;
        }

        public void setFullSync(boolean fullSync) {
            this.fullSync = fullSync;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class ConfigSecondaryVmCmd extends AgentCommand {
        private String vmInstanceUuid;
        private Integer mirrorPort;
        private Integer secondaryInPort;
        private Integer nbdServerPort;
        private String primaryVmHostIp;

        public Integer getMirrorPort() {
            return mirrorPort;
        }

        public void setMirrorPort(Integer mirrorPort) {
            this.mirrorPort = mirrorPort;
        }

        public Integer getSecondaryInPort() {
            return secondaryInPort;
        }

        public void setSecondaryInPort(Integer secondaryInPort) {
            this.secondaryInPort = secondaryInPort;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getPrimaryVmHostIp() {
            return primaryVmHostIp;
        }

        public void setPrimaryVmHostIp(String primaryVmHostIp) {
            this.primaryVmHostIp = primaryVmHostIp;
        }

        public Integer getNbdServerPort() {
            return nbdServerPort;
        }

        public void setNbdServerPort(Integer nbdServerPort) {
            this.nbdServerPort = nbdServerPort;
        }
    }

    public static class StartVmCmd extends vdiCmd implements VmAddOnsCmd {
        private String vmInstanceUuid;
        private long vmInternalId;
        private String vmName;
        private String imagePlatform;
        private String imageArchitecture;
        private long memory;
        private long maxMemory;
        private int cpuNum;
        private long cpuSpeed;
        private int socketNum;
        private int cpuOnSocket;
        private List<String> bootDev;
        private VolumeTO rootVolume;
        private List<IsoTO> bootIso = new ArrayList<>();
        private List<CdRomTO> cdRoms = new ArrayList<>();
        private List<VolumeTO> dataVolumes;
        private List<VolumeTO> cacheVolumes;
        private List<VolumeTO> Volumes;
        private List<NicTO> nics;
        private long timeout;
        private Map<String, Object> addons;
        private boolean instanceOfferingOnlineChange;
        private String nestedVirtualization;
        private String hostManagementIp;
        private String clock;
        private String clockTrack;
        private boolean useNuma;
        private String MemAccess;
        private boolean usbRedirect;
        private boolean useBootMenu;
        private boolean createPaused;
        private boolean kvmHiddenState;
        private boolean vmPortOff;
        private String vmCpuModel;
        private boolean emulateHyperV;
        private String vendorId;
        private boolean additionalQmp;
        private boolean isApplianceVm;
        private String systemSerialNumber;
        private String bootMode;
        private boolean fromForeignHypervisor;
        private String machineType;
        private Integer pciePortNums;
        private Integer predefinedPciBridgeNum;
        private boolean useHugePage;
        private String chassisAssetTag;
        private PriorityConfigStruct priorityConfigStruct;
        private String memorySnapshotPath;
        private boolean coloPrimary;
        private boolean coloSecondary;
        private boolean consoleLogToFile;
        private boolean acpi;
        private boolean hygonCpu;

        // TODO: only for test
        private boolean useColoBinary;

        public String getChassisAssetTag() {
            return chassisAssetTag;
        }

        public void setChassisAssetTag(String chassisAssetTag) {
            this.chassisAssetTag = chassisAssetTag;
        }

        public PriorityConfigStruct getPriorityConfigStruct() {
            return priorityConfigStruct;
        }

        public void setPriorityConfigStruct(PriorityConfigStruct priorityConfigStruct) {
            this.priorityConfigStruct = priorityConfigStruct;
        }

        public boolean isUseHugePage() {
            return useHugePage;
        }

        public void setUseHugePage(boolean useHugePage) {
            this.useHugePage = useHugePage;
        }

        public boolean isFromForeignHypervisor() {
            return fromForeignHypervisor;
        }

        public void setFromForeignHypervisor(boolean fromForeignHypervisor) {
            this.fromForeignHypervisor = fromForeignHypervisor;
        }

        public String getMachineType() {
            return machineType;
        }

        public void setMachineType(String machineType) {
            this.machineType = machineType;
        }

        public void setPciePortNums(Integer pciePortNums) {
            this.pciePortNums = pciePortNums;
        }

        public Integer getPciePortNums() {
            return pciePortNums;
        }

        public boolean isAdditionalQmp() {
            return additionalQmp;
        }

        public void setAdditionalQmp(boolean additionalQmp) {
            this.additionalQmp = additionalQmp;
        }

        public String getBootMode() {
            return bootMode;
        }

        public void setBootMode(String bootMode) {
            this.bootMode = bootMode;
        }

        public boolean isEmulateHyperV() {
            return emulateHyperV;
        }

        public void setEmulateHyperV(boolean emulateHyperV) {
            this.emulateHyperV = emulateHyperV;
        }

        public boolean isApplianceVm() {
            return isApplianceVm;
        }

        public void setApplianceVm(boolean applianceVm) {
            isApplianceVm = applianceVm;
        }

        public String getSystemSerialNumber() {
            return systemSerialNumber;
        }

        public void setSystemSerialNumber(String systemSerialNumber) {
            this.systemSerialNumber = systemSerialNumber;
        }

        public String getVmCpuModel() {
            return vmCpuModel;
        }

        public void setVmCpuModel(String vmCpuModel) {
            this.vmCpuModel = vmCpuModel;
        }

        public boolean isKvmHiddenState() {
            return kvmHiddenState;
        }

        public void setKvmHiddenState(boolean kvmHiddenState) {
            this.kvmHiddenState = kvmHiddenState;
        }

        public void setVmPortOff(boolean vmPortOff) {
            this.vmPortOff = vmPortOff;
        }

        public boolean isVmPortOff() {
            return vmPortOff;
        }

        public void setUseBootMenu(boolean useBootMenu) {
            this.useBootMenu = useBootMenu;
        }

        public boolean isUseBootMenu() {
            return useBootMenu;
        }

        public void setCreatePaused(boolean createPaused) {
            this.createPaused = createPaused;
        }

        public boolean isCreatePaused() {
            return createPaused;
        }

        public boolean isUsbRedirect() {
            return usbRedirect;
        }

        public void setUsbRedirect(boolean usbRedirect) {
            this.usbRedirect = usbRedirect;
        }

        public boolean isUseNuma() {
            return useNuma;
        }

        public void setUseNuma(boolean useNuma) {
            this.useNuma = useNuma;
        }

        public String getMemAccess() {
            return MemAccess;
        }

        public void setMemAccess(String memAccess) {
            MemAccess = memAccess;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }

        public String getClock() {
            return clock;
        }

        public void setClock(String clock) {
            this.clock = clock;
        }

        public String getClockTrack() {
            return clockTrack;
        }

        public void setClockTrack(String clockTrack) {
            this.clockTrack = clockTrack;
        }

        public int getSocketNum() {
            return socketNum;
        }

        public void setSocketNum(int socketNum) {
            this.socketNum = socketNum;
        }

        public int getCpuOnSocket() {
            return cpuOnSocket;
        }

        public void setCpuOnSocket(int cpuOnSocket) {
            this.cpuOnSocket = cpuOnSocket;
        }

        public String getVmName() {
            return vmName;
        }

        public void setVmName(String vmName) {
            this.vmName = vmName;
        }

        public String getImagePlatform() {
            return imagePlatform;
        }

        public void setImagePlatform(String imagePlatform) {
            this.imagePlatform = imagePlatform;
        }

        public String getImageArchitecture() {
            return imageArchitecture;
        }

        public void setImageArchitecture(String imageArchitecture) {
            this.imageArchitecture = imageArchitecture;
        }

        public long getMemory() {
            return memory;
        }

        public void setMemory(long memory) {
            this.memory = memory;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public long getCpuSpeed() {
            return cpuSpeed;
        }

        public void setCpuSpeed(long cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
        }

        public List<String> getBootDev() {
            return bootDev;
        }

        public void setBootDev(List<String> bootDev) {
            this.bootDev = bootDev;
        }

        public boolean getInstanceOfferingOnlineChange() {
            return instanceOfferingOnlineChange;
        }

        public void setInstanceOfferingOnlineChange(boolean instanceOfferingOnlineChange) {
            this.instanceOfferingOnlineChange = instanceOfferingOnlineChange;
        }

        @Deprecated
        public List<IsoTO> getBootIso() {
            return bootIso;
        }

        @Deprecated
        public void setBootIso(List<IsoTO> bootIso) {
            this.bootIso = bootIso;
        }

        public List<CdRomTO> getCdRoms() {
            return cdRoms;
        }

        public void setCdRoms(List<CdRomTO> cdRoms) {
            this.cdRoms = cdRoms;
        }

        public String getNestedVirtualization() {
            return nestedVirtualization;
        }

        public void setNestedVirtualization(String nestedVirtualization) {
            this.nestedVirtualization = nestedVirtualization;
        }

        public String getHostManagementIp() {
            return hostManagementIp;
        }

        public void setHostManagementIp(String hostManagementIp) {
            this.hostManagementIp = hostManagementIp;
        }

        public VolumeTO getRootVolume() {
            return rootVolume;
        }

        public void setRootVolume(VolumeTO rootVolume) {
            this.rootVolume = rootVolume;
        }

        public List<VolumeTO> getDataVolumes() {
            return dataVolumes;
        }

        public void setDataVolumes(List<VolumeTO> dataVolumes) {
            this.dataVolumes = dataVolumes;
        }

        public List<VolumeTO> getCacheVolumes() {
            return cacheVolumes;
        }

        public void setCacheVolumes(List<VolumeTO> cacheVolumes) {
            this.cacheVolumes = cacheVolumes;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public boolean isUseColoBinary() {
            return useColoBinary;
        }

        public void setUseColoBinary(boolean useColoBinary) {
            this.useColoBinary = useColoBinary;
        }

        public long getVmInternalId() {
            return vmInternalId;
        }

        public void setVmInternalId(long vmInternalId) {
            this.vmInternalId = vmInternalId;
        }

        public Integer getPredefinedPciBridgeNum() {
            return predefinedPciBridgeNum;
        }

        public void setPredefinedPciBridgeNum(Integer predefinedPciBridgeNum) {
            this.predefinedPciBridgeNum = predefinedPciBridgeNum;
        }

        public String getMemorySnapshotPath() {
            return memorySnapshotPath;
        }

        public void setMemorySnapshotPath(String memorySnapshotPath) {
            this.memorySnapshotPath = memorySnapshotPath;
        }

        public boolean isColoPrimary() {
            return coloPrimary;
        }

        public void setColoPrimary(boolean coloPrimary) {
            this.coloPrimary = coloPrimary;
        }

        public boolean isColoSecondary() {
            return coloSecondary;
        }

        public void setColoSecondary(boolean coloSecondary) {
            this.coloSecondary = coloSecondary;
        }

        public boolean isConsoleLogToFile() {
            return consoleLogToFile;
        }

        public void setConsoleLogToFile(boolean consoleLogToFile) {
            this.consoleLogToFile = consoleLogToFile;
        }

        public boolean getHygonCpu() {
            return hygonCpu;
        }

        public void setHygonCpu(boolean hygonCpu) {
            this.hygonCpu = hygonCpu;
        }

        public boolean getAcpi() {
            return acpi;
        }

        public void setAcpi(boolean acpi) {
            this.acpi = acpi;
        }

        public String getVendorId() {
            return vendorId;
        }

        public void setVendorId(String vendorId) {
            this.vendorId = vendorId;
        }

        @Override
        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }
    }

    public static class StartVmResponse extends AgentResponse {
        private List<VmNicInfo> nicInfos;
        private List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VmNicInfo> getNicInfos() {
            return nicInfos;
        }

        public void setNicInfos(List<VmNicInfo> nicInfos) {
            this.nicInfos = nicInfos;
        }

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class VmNicInfo {
        private String macAddress;
        private PciAddressConfig pciInfo;

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public PciAddressConfig getPciInfo() {
            return pciInfo;
        }

        public void setPciInfo(PciAddressConfig pciInfo) {
            this.pciInfo = pciInfo;
        }
    }

    public static class ChangeCpuMemoryCmd extends AgentCommand {
        private String vmUuid;
        private int cpuNum;
        private long memorySize;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class ChangeCpuMemoryResponse extends AgentResponse {
        private int cpuNum;
        private long memorySize;

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class IncreaseCpuCmd extends AgentCommand {
        private String vmUuid;
        private int cpuNum;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }
    }

    public static class IncreaseCpuResponse extends AgentResponse {
        private int cpuNum;

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }
    }

    public static class IncreaseMemoryCmd extends AgentCommand {
        private String vmUuid;
        private long memorySize;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class IncreaseMemoryResponse extends AgentResponse {
        private long memorySize;

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class ScanVmPortCmd extends AgentCommand {
        private String ip;
        private String brname;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getBrname() {
            return brname;
        }

        public void setBrname(String brname) {
            this.brname = brname;
        }
    }

    public static class ScanVmPortResponse extends AgentResponse {
        private Map<String, String> portStatus;

        public Map<String, String> getPortStatus() {
            return portStatus;
        }

        public void setPortStatus(Map<String, String> portStatus) {
            this.portStatus = portStatus;
        }
    }

    public static class GetVncPortCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class GetVncPortResponse extends AgentResponse {
        private int port;
        private String protocol;
        private Integer vncPort;
        private Integer spicePort;
        private Integer spiceTlsPort;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Integer getVncPort() {
            return vncPort;
        }

        public void setVncPort(Integer vncPort) {
            this.vncPort = vncPort;
        }

        public Integer getSpicePort() {
            return spicePort;
        }

        public void setSpicePort(Integer spicePort) {
            this.spicePort = spicePort;
        }

        public Integer getSpiceTlsPort() {
            return spiceTlsPort;
        }

        public void setSpiceTlsPort(Integer spiceTlsPort) {
            this.spiceTlsPort = spiceTlsPort;
        }
    }

    public static class StopVmCmd extends AgentCommand {
        private String uuid;
        private String type;
        private long timeout;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class StopVmResponse extends AgentResponse {
    }

    public static class PauseVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class PauseVmResponse extends AgentResponse {

    }

    public static class ResumeVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class ResumeVmResponse extends AgentResponse {

    }

    public static class RebootVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;
        private List<String> bootDev;

        public List<String> getBootDev() {
            return bootDev;
        }

        public void setBootDev(List<String> bootDev) {
            this.bootDev = bootDev;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class RebootVmResponse extends AgentResponse {
    }

    public static class DestroyVmCmd extends AgentCommand {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class DestroyVmResponse extends AgentResponse {
    }

    public static class GetVmFirstBootDeviceCmd extends AgentCommand {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class GetVmFirstBootDeviceResponse extends AgentResponse {
        private String firstBootDevice;

        public String getFirstBootDevice() {
            return firstBootDevice;
        }

        public void setFirstBootDevice(String firstBootDevice) {
            this.firstBootDevice = firstBootDevice;
        }
    }

    public static class GetVmDeviceAddressCmd extends AgentCommand {
        private String uuid;
        private Map<String, List> deviceTOs = new HashMap<>();

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void putDevice(String type, List deviceTOs) {
            this.deviceTOs.put(type, deviceTOs);
        }

        public Map<String, List> getDeviceTOs() {
            return deviceTOs;
        }

        public void setDeviceTOs(Map<String, List> deviceTOs) {
            this.deviceTOs = deviceTOs;
        }
    }

    public static class GetVmDeviceAddressRsp extends AgentResponse {
        private Map<String, List<VmDeviceAddressTO>> addresses;

        public Map<String, List<VmDeviceAddressTO>> getAddresses() {
            return addresses;
        }

        public List<VmDeviceAddressTO> getAddresses(String resourceType) {
            return addresses.get(resourceType);
        }

        public void setAddresses(Map<String, List<VmDeviceAddressTO>> addresses) {
            this.addresses = addresses;
        }
    }

    public static class VmDeviceAddressTO {
        private String addressType;
        private String address;
        private String deviceType;
        private String uuid;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAddressType() {
            return addressType;
        }

        public void setAddressType(String addressType) {
            this.addressType = addressType;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class VmSyncCmd extends AgentCommand {
    }

    public static class VmSyncResponse extends AgentResponse {
        private HashMap<String, String> states;

        private List<String> vmInShutdowns;

        public HashMap<String, String> getStates() {
            return states;
        }

        public void setStates(HashMap<String, String> states) {
            this.states = states;
        }

        public List<String> getVmInShutdowns() {
            return vmInShutdowns == null ? Collections.emptyList() : vmInShutdowns;
        }

        public void setVmInShutdowns(List<String> vmInShutdowns) {
            this.vmInShutdowns = vmInShutdowns;
        }
    }

    public static class RefreshAllRulesOnHostCmd extends AgentCommand {
        private List<SecurityGroupRuleTO> ruleTOs;
        private List<SecurityGroupRuleTO> ipv6RuleTOs;

        public List<SecurityGroupRuleTO> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(List<SecurityGroupRuleTO> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public List<SecurityGroupRuleTO> getIpv6RuleTOs() {
            return ipv6RuleTOs;
        }

        public void setIpv6RuleTOs(List<SecurityGroupRuleTO> ipv6RuleTOs) {
            this.ipv6RuleTOs = ipv6RuleTOs;
        }
    }

    public static class RefreshAllRulesOnHostResponse extends AgentResponse {
    }

    public static class CheckDefaultSecurityGroupCmd extends AgentCommand {
        Boolean skipIpv6;
    }

    public static class CheckDefaultSecurityGroupResponse extends AgentResponse {

    }

    public static class UpdateGroupMemberCmd extends AgentCommand {
        private List<SecurityGroupMembersTO> updateGroupTOs;

        public void setUpdateGroupTOs(List<SecurityGroupMembersTO> updateGroupTOs) {
            this.updateGroupTOs = updateGroupTOs;
        }

        public List<SecurityGroupMembersTO> getUpdateGroupTOs() {
            return updateGroupTOs;
        }
    }

    public static class UpdateGroupMemberResponse extends AgentResponse {
    }

    public static class CleanupUnusedRulesOnHostCmd extends AgentCommand {
        Boolean skipIpv6;
    }

    public static class CleanupUnusedRulesOnHostResponse extends AgentResponse {
    }


    public static class ApplySecurityGroupRuleCmd extends AgentCommand {
        private List<SecurityGroupRuleTO> ruleTOs;
        private List<SecurityGroupRuleTO> ipv6RuleTOs;

        public List<SecurityGroupRuleTO> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(List<SecurityGroupRuleTO> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public List<SecurityGroupRuleTO> getIpv6RuleTOs() {
            return ipv6RuleTOs;
        }

        public void setIpv6RuleTOs(List<SecurityGroupRuleTO> ipv6RuleTOs) {
            this.ipv6RuleTOs = ipv6RuleTOs;
        }
    }

    public static class ApplySecurityGroupRuleResponse extends AgentResponse {
    }

    public static class MigrateVmCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private String destHostIp;
        private String storageMigrationPolicy;
        private String srcHostIp;
        private boolean useNuma;
        private boolean migrateFromDestination;
        private boolean autoConverge;
        private boolean xbzrle;
        private List<String> vdpaPaths;
        private Long timeout; // in seconds

        public boolean isUseNuma() {
            return useNuma;
        }

        public void setUseNuma(boolean useNuma) {
            this.useNuma = useNuma;
        }

        public String getSrcHostIp() {
            return srcHostIp;
        }

        public void setSrcHostIp(String srcHostIp) {
            this.srcHostIp = srcHostIp;
        }

        public String getStorageMigrationPolicy() {
            return storageMigrationPolicy;
        }

        public void setStorageMigrationPolicy(String storageMigrationPolicy) {
            this.storageMigrationPolicy = storageMigrationPolicy;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getDestHostIp() {
            return destHostIp;
        }

        public void setDestHostIp(String destHostIp) {
            this.destHostIp = destHostIp;
        }

        public boolean isAutoConverge() {
            return autoConverge;
        }

        public void setAutoConverge(boolean autoConverge) {
            this.autoConverge = autoConverge;
        }

        public boolean isXbzrle() {
            return xbzrle;
        }

        public void setXbzrle(boolean xbzrle) {
            this.xbzrle = xbzrle;
        }

        public boolean isMigrateFromDestination() {
            return migrateFromDestination;
        }

        public void setMigrateFromDestination(boolean migrateFromDestination) {
            this.migrateFromDestination = migrateFromDestination;
        }

        public List<String> getVdpaPaths() {
            return vdpaPaths;
        }

        public void setVdpaPaths(List<String> vdpaPaths) {
            this.vdpaPaths = vdpaPaths;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }
    }

    public static class MigrateVmResponse extends AgentResponse {
    }

    public static class MergeSnapshotRsp extends AgentResponse {
    }

    public static class MergeSnapshotCmd extends AgentCommand {
        private String vmUuid;
        private VolumeTO volume;
        private String srcPath;
        private String destPath;
        private boolean fullRebase;
        // timeout seconds
        private long timeout;

        public boolean isFullRebase() {
            return fullRebase;
        }

        public void setFullRebase(boolean fullRebase) {
            this.fullRebase = fullRebase;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDestPath() {
            return destPath;
        }

        public void setDestPath(String destPath) {
            this.destPath = destPath;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class CheckSnapshotCmd extends AgentCommand {
        private String vmUuid;
        private String volumeUuid;
        private String currentInstallPath;
        private List<String> excludeInstallPaths;
        private Map<String, Integer> volumeChainToCheck;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public Map<String, Integer> getVolumeChainToCheck() {
            return volumeChainToCheck;
        }

        public void setVolumeChainToCheck(Map<String, Integer> volumeChainToCheck) {
            this.volumeChainToCheck = volumeChainToCheck;
        }

        public String getCurrentInstallPath() {
            return currentInstallPath;
        }

        public void setCurrentInstallPath(String currentInstallPath) {
            this.currentInstallPath = currentInstallPath;
        }

        public List<String> getExcludeInstallPaths() {
            return excludeInstallPaths;
        }

        public void setExcludeInstallPaths(List<String> excludeInstallPaths) {
            this.excludeInstallPaths = excludeInstallPaths;
        }
    }

    public static class TakeSnapshotCmd extends AgentCommand {
        private String vmUuid;
        private String volumeUuid;
        private VolumeTO volume;
        private String installPath;
        private boolean fullSnapshot;
        private String volumeInstallPath;
        private String newVolumeUuid;
        private String newVolumeInstallPath;

        // for baremetal2 instance
        private boolean isBaremetal2InstanceOnlineSnapshot;

        private long timeout;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getVolumeInstallPath() {
            return volumeInstallPath;
        }

        public void setVolumeInstallPath(String volumeInstallPath) {
            this.volumeInstallPath = volumeInstallPath;
        }

        public boolean isFullSnapshot() {
            return fullSnapshot;
        }

        public void setFullSnapshot(boolean fullSnapshot) {
            this.fullSnapshot = fullSnapshot;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public String getNewVolumeUuid() {
            return newVolumeUuid;
        }

        public void setNewVolumeUuid(String newVolumeUuid) {
            this.newVolumeUuid = newVolumeUuid;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public boolean isBaremetal2InstanceOnlineSnapshot() {
            return isBaremetal2InstanceOnlineSnapshot;
        }

        public void setBaremetal2InstanceOnlineSnapshot(boolean baremetal2InstanceOnlineSnapshot) {
            isBaremetal2InstanceOnlineSnapshot = baremetal2InstanceOnlineSnapshot;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class CheckSnapshotResponse extends AgentResponse {

    }

    public static class TakeSnapshotResponse extends AgentResponse {
        @Validation
        private String newVolumeInstallPath;
        @Validation
        private String snapshotInstallPath;
        @Validation(notZero = true)
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class LogoutIscsiTargetCmd extends AgentCommand {
        private String hostname;
        private int port;
        private String target;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }

    public static class LogoutIscsiTargetRsp extends AgentResponse {
    }

    public static class LoginIscsiTargetCmd extends AgentCommand implements Serializable {
        private String hostname;
        private int port;
        private String target;
        private String chapUsername;
        @NoLogging
        private String chapPassword;

        public String getChapUsername() {
            return chapUsername;
        }

        public void setChapUsername(String chapUsername) {
            this.chapUsername = chapUsername;
        }

        public String getChapPassword() {
            return chapPassword;
        }

        public void setChapPassword(String chapPassword) {
            this.chapPassword = chapPassword;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }

    public static class LoginIscsiTargetRsp extends AgentResponse {
    }

    public static class AttachIsoCmd extends AgentCommand {
        public IsoTO iso;
        public String vmUuid;
    }

    public static class AttachIsoRsp extends AgentResponse {
    }

    public static class DetachIsoCmd extends AgentCommand {
        public String vmUuid;
        public String isoUuid;
        public int deviceId;
    }

    public static class DetachIsoRsp extends AgentResponse {

    }

    public static class UpdateHostOSCmd extends AgentCommand {
        public String hostUuid;
        public String excludePackages;
        public String updatePackages;
        public String releaseVersion;
        public boolean enableExpRepo;
    }

    public static class UpdateHostOSRsp extends AgentResponse {
    }

    public static class UpdateDependencyCmd extends AgentCommand {
        public String hostUuid;
        public boolean enableExpRepo;
        public String excludePackages;
        public String updatePackages;
        public String zstackRepo;
    }

    public static class UpdateDependencyRsp extends AgentResponse {
    }

    public static class ReportVmStateCmd {
        public String hostUuid;
        public String vmUuid;
        public String vmState;
    }

    public static class ReconnectMeCmd {
        public String hostUuid;
        public String reason;
    }

    public static class ReportPsStatusCmd {
        public String hostUuid;
        public List<String> psUuids;
        public String psStatus;
        public String reason;
    }

    public static class ReportSelfFencerCmd {
        public String hostUuid;
        public List<String> psUuids;
        public String reason;
        public String vmUuidsString;
        public boolean fencerFailure = true;
    }

    public static class ReportHostMaintainCmd {
        public String hostUuid;
    }

    public static class ReportFailoverCmd {
        public String vmInstanceUuid;
        public String hostUuid;
        public String reason;
        public boolean primaryVmFailure;
    }

    public static class ReportHostDeviceEventCmd {
        public String hostUuid;
    }

    public static class ReportVmShutdownEventCmd {
        public String vmUuid;
    }

    public static class ReportVmRebootEventCmd {
        public String vmUuid;
    }

    public static class ReportVmCrashEventCmd {
        public String vmUuid;
    }

    public static class ShutdownHostCmd extends AgentCommand {
    }

    public static class ShutdownHostResponse extends AgentResponse {
    }

    public static class UpdateSpiceChannelConfigCmd extends AgentCommand {
    }

    public static class UpdateSpiceChannelConfigResponse extends AgentResponse {
        public boolean restartLibvirt = false;
    }

    public static class PrimaryStorageCommand extends AgentCommand {
        public String primaryStorageUuid;
    }

    public static class CancelCmd extends AgentCommand implements CancelCommand {
        private String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    public static class CancelRsp extends AgentResponse {
    }

    public static class TransmitVmOperationToMnCmd {
        public String uuid;
        public String operation;
    }

    public static class GetDevCapacityCmd extends AgentCommand {
        public String dirPath;
    }

    public static class GetDevCapacityResponse extends AgentResponse {
        public long totalSize;
        public long availableSize;
        public long dirSize;
    }

    public static class CheckFileOnHostCmd extends AgentCommand {
        public Set<String> paths;
        public boolean md5Return;
    }

    public static class CheckFileOnHostResponse extends AgentResponse {
        public Map<String, String> existPaths;
    }

    public static class GetHostNUMATopologyCmd extends AgentCommand {
        public String HostUuid;

        public void setHostUuid(String hostUuid) {
            HostUuid = hostUuid;
        }

        public String getHostUuid() {
            return HostUuid;
        }
    }

    public static class GetHostNUMATopologyResponse extends AgentResponse {
        public Map<String, HostNUMANode> topology;


        public void setTopology(Map<String, HostNUMANode> topology) {
            this.topology = topology;
        }

        public Map<String, HostNUMANode> getTopology() {
            return topology;
        }
    }

    public static class PhysicalNicAlarmEventCmd {
        public String host;
        public String nic;
        public String ip;
        public String bond;
        public String status;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getNic() {
            return nic;
        }

        public void setNic(String nic) {
            this.nic = nic;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getBond() {
            return bond;
        }

        public void setBond(String bond) {
            this.bond = bond;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
