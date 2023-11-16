package org.zstack.kvm;

import org.zstack.core.upgrade.GrayUpgradeAgent;
import org.zstack.core.upgrade.GrayVersion;
import org.zstack.core.validation.ConditionalValidation;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.CancelCommand;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.VmNicRedirectConfig;
import org.zstack.header.log.NoLogging;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.network.securitygroup.RuleTO;
import org.zstack.network.securitygroup.SecurityGroupMembersTO;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.network.securitygroup.VmNicSecurityTO;
import org.zstack.network.service.MtuGetter;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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

    public static class AgentResponse extends GrayUpgradeAgent implements ConditionalValidation {
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

    public static class AgentCommand extends GrayUpgradeAgent {
        public LinkedHashMap kvmHostAddons;
    }

    public static class CheckVmStateCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public List<String> vmUuids;
        @GrayVersion(value = "5.0.0")
        public String hostUuid;
    }

    public static class CheckVmStateRsp extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public Map<String, String> states;
    }

    public static class UpdateVmPriorityCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public List<PriorityConfigStruct> priorityConfigStructs;
    }

    public static class UpdateVmPriorityRsp extends AgentResponse {
    }

    public static class ChangeVmNicStateCommand extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private String state;
        @GrayVersion(value = "5.0.0")
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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static class ChangeVmNicStateRsp extends AgentResponse {

    }

    public static class DetachNicCommand extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private NicTO nic;
        @GrayVersion(value = "5.0.0")
        private String accountUuid;
        @GrayVersion(value = "5.0.0")
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

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    public static class AttachNicResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class UpdateNicCmd extends AgentCommand implements VmAddOnsCmd {
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
        private List<KVMAgentCommands.NicTO> nics;
        @GrayVersion(value = "5.0.0")
        private Map<String, Object> addons = new HashMap<>();
        @GrayVersion(value = "5.0.0")
        private String accountUuid;

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

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
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
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String osDistribution;
        @GrayVersion(value = "5.0.0")
        private String osVersion;
        @GrayVersion(value = "5.0.0")
        private String osRelease;
        @GrayVersion(value = "5.0.0")
        private String qemuImgVersion;
        @GrayVersion(value = "5.0.0")
        private String libvirtVersion;
        @GrayVersion(value = "5.0.0")
        private String hvmCpuFlag;
        @GrayVersion(value = "5.0.0")
        private String eptFlag;
        @GrayVersion(value = "5.0.0")
        private String cpuArchitecture;
        @GrayVersion(value = "5.0.0")
        private String cpuModelName;
        @GrayVersion(value = "5.0.0")
        private String cpuGHz;
        @GrayVersion(value = "5.0.0")
        private String cpuProcessorNum;
        @GrayVersion(value = "5.0.0")
        private String powerSupplyModelName;
        @GrayVersion(value = "5.0.0")
        private String powerSupplyManufacturer;
        @GrayVersion(value = "5.0.0")
        private String ipmiAddress;
        @GrayVersion(value = "5.0.0")
        private String powerSupplyMaxPowerCapacity;
        @GrayVersion(value = "5.0.0")
        private String hostCpuModelName;
        @GrayVersion(value = "5.0.0")
        private String systemProductName;
        @GrayVersion(value = "5.0.0")
        private String systemSerialNumber;
        @GrayVersion(value = "5.0.0")
        private String systemManufacturer;
        @GrayVersion(value = "5.0.0")
        private String systemUUID;
        @GrayVersion(value = "5.0.0")
        private String biosVendor;
        @GrayVersion(value = "5.0.0")
        private String biosVersion;
        @GrayVersion(value = "5.0.0")
        private String biosReleaseDate;
        @GrayVersion(value = "5.0.0")
        private String bmcVersion;
        @GrayVersion(value = "5.0.0")
        private String uptime;
        @GrayVersion(value = "5.0.0")
        private String memorySlotsMaximum;
        @GrayVersion(value = "5.0.0")
        private String cpuCache;
        @GrayVersion(value = "5.0.0")
        private List<String> ipAddresses;
        @GrayVersion(value = "5.0.0")
        private List<String> libvirtCapabilities;
        @GrayVersion(value = "5.0.0")
        private VirtualizerInfoTO virtualizerInfo;

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

        public void setEptFlag(String eptFlag) {
            this.eptFlag = eptFlag;
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

        public void setCpuGHz(String cpuGHz) {
            this.cpuGHz = cpuGHz;
        }

        public String getCpuProcessorNum() {
            return cpuProcessorNum;
        }

        public void setCpuProcessorNum(String cpuProcessorNum) {
            this.cpuProcessorNum = cpuProcessorNum;
        }

        public String getCpuCache() {
            return cpuCache;
        }

        public void setCpuCache (String cpuCache) {
            this.cpuCache = cpuCache;
        }


        public String getPowerSupplyModelName() {
            return powerSupplyModelName;
        }

        public void setPowerSupplyModelName(String powerSupplyModelName) {
            this.powerSupplyModelName = powerSupplyModelName;
        }


        public String getPowerSupplyManufacturer() {
            return powerSupplyManufacturer;
        }

        public void setPowerSupplyManufacturer(String powerSupplyManufacturer) {
            this.powerSupplyManufacturer = powerSupplyManufacturer;
        }

        public String getIpmiAddress() {return ipmiAddress;}

        public void setIpmiAddress(String ipmiAddress) {
            this.ipmiAddress = ipmiAddress;
        }

        public String getPowerSupplyMaxPowerCapacity() {
            return powerSupplyMaxPowerCapacity;
        }

        public void setPowerSupplyMaxPowerCapacity(String powerSupplyMaxPowerCapacity) {
            this.powerSupplyMaxPowerCapacity = powerSupplyMaxPowerCapacity;
        }

        public String getCpuArchitecture() {
            return cpuArchitecture;
        }

        public String getHostCpuModelName() {
            return hostCpuModelName;
        }

        public void setHostCpuModelName (String hostCpuModelName) {
            this.hostCpuModelName = hostCpuModelName;
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

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;

        }

        public String getSystemManufacturer() {
            return systemManufacturer;
        }

        public void setSystemManufacturer(String systemManufacturer) {
            this.systemManufacturer = systemManufacturer;
        }

        public String getSystemUUID() {
            return systemUUID;
        }

        public void setSystemUUID(String systemUUID) {
            this.systemUUID = systemUUID;
        }

        public String getBiosVendor() {
            return biosVendor;
        }

        public void setBiosVendor(String biosVendor) {
            this.biosVendor = biosVendor;
        }

        public String getBiosVersion() {
            return biosVersion;
        }

        public void setBiosVersion(String biosVersion) {
            this.biosVersion = biosVersion;
        }

        public String getBiosReleaseDate() {
            return biosReleaseDate;
        }

        public void setBiosReleaseDate(String biosReleaseDate) {
            this.biosReleaseDate = biosReleaseDate;
        }

        public String getBmcVersion() {
            return bmcVersion;
        }

        public void setBmcVersion(String bmcVersion) {
            this.bmcVersion = bmcVersion;
        }

        public String getUptime() {
            return uptime;
        }

        public void setUptime(String uptime) {
            this.uptime = uptime;
        }

        public String getMemorySlotsMaximum() {
            return memorySlotsMaximum;
        }

        public void setMemorySlotsMaximum(String memorySlotsMaximum) {
            this.memorySlotsMaximum = memorySlotsMaximum;
        }
    }

    public static class HostCapacityCmd extends AgentCommand {
    }

    public static class HostCapacityResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private long cpuNum;
        @GrayVersion(value = "5.0.0")
        private long cpuSpeed;
        @GrayVersion(value = "5.0.0")
        private long usedCpu;
        @GrayVersion(value = "5.0.0")
        private long totalMemory;
        @GrayVersion(value = "5.0.0")
        private long usedMemory;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String physicalInterfaceName;
        @GrayVersion(value = "5.0.0")
        private String bridgeName;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String physicalInterfaceName;
        @GrayVersion(value = "5.0.0")
        private String bridgeName;
        @GrayVersion(value = "5.0.0")
        private String l2NetworkUuid;
        @GrayVersion(value = "5.0.0")
        private Boolean disableIptables;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String physicalInterfaceName;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String physicalInterfaceName;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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

    public static class NicTO extends BaseVirtualDeviceTO {
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
        private DeviceAddress pci;
        private String type;
        private String state;

        // only for vf nic
        private String vlanId;
        private String pciDeviceAddress;
        // only for tf nic
        private String ipForTf;
        private String l2NetworkUuid;

        // for vDPA & dpdkvhostuserclient nic
        private String srcPath;
        
        private Boolean cleanTraffic;

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

        public DeviceAddress getPci() {
            return pci;
        }

        public void setPci(DeviceAddress pci) {
            this.pci = pci;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public Boolean getCleanTraffic() {
            return cleanTraffic;
        }

        public void setCleanTraffic(Boolean cleanTraffic) {
            this.cleanTraffic = cleanTraffic;
        }

        public String getIpForTf() {
            return ipForTf;
        }

        public void setIpForTf(String ipForTf) {
            this.ipForTf = ipForTf;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public static NicTO fromVmNicInventory(VmNicInventory nic) {
            KVMAgentCommands.NicTO to = new KVMAgentCommands.NicTO();
            to.setMac(nic.getMac());
            to.setUuid(nic.getUuid());
            to.setDeviceId(nic.getDeviceId());
            to.setNicInternalName(nic.getInternalName());
            to.setType(nic.getType());

            return to;
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
        @GrayVersion(value = "5.0.0")
        private VolumeTO volume;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private VolumeTO volume;
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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

    public static class CdRomTO extends BaseVirtualDeviceTO {
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
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        public List<String> vdpaPaths;

        public List<String> getVdpaPaths() {
            return vdpaPaths;
        }

        public void setVdpaPaths(List<String> vdpaPaths) {
            this.vdpaPaths = vdpaPaths;
        }
    }

    public static class DeleteVdpaCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
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

    public static class GenerateVHostUserClientCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
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

    public static class GenerateVHostUserClientResponse extends AgentResponse {

    }

    public static class DeleteVHostUserClientCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
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

    public static class DeleteVHostUserClientRsp extends AgentResponse {

    }
    public static class CleanVmFirmwareFlashCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
    }

    public static class HardenVmConsoleCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
        public Long vmInternalId;
        @GrayVersion(value = "5.0.0")
        public String hostManagementIp;
    }

    public static class DeleteVmConsoleFirewallCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
        public Long vmInternalId;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String consoleMode;
        @GrayVersion(value = "5.0.0")
        private String videoType;
        @GrayVersion(value = "5.0.0")
        private String soundType;
        @GrayVersion(value = "5.0.0")
        private String spiceStreamingMode;
        @GrayVersion(value = "5.0.0")
        private Integer VDIMonitorNumber;
        @NoLogging
        @GrayVersion(value = "5.0.0")
        private String consolePassword;
        @GrayVersion(value = "5.0.0")
        private Map<String, String> qxlMemory;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private List<VmNicRedirectConfig> configs;
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String hostUuid;
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
        private Integer heartbeatPort;
        @GrayVersion(value = "5.0.0")
        private String targetHostIp;
        @GrayVersion(value = "5.0.0")
        private boolean coloPrimary;
        @GrayVersion(value = "5.0.0")
        private Integer redirectNum;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
        private Integer blockReplicationPort;
        @GrayVersion(value = "5.0.0")
        private Integer nbdServerPort;
        @GrayVersion(value = "5.0.0")
        private String secondaryVmHostIp;
        @GrayVersion(value = "5.0.0")
        private Long checkpointDelay;
        @GrayVersion(value = "5.0.0")
        private boolean fullSync;
        @GrayVersion(value = "5.0.0")
        private List<VolumeTO> volumes = new ArrayList<>();
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
        private Integer mirrorPort;
        @GrayVersion(value = "5.0.0")
        private Integer secondaryInPort;
        @GrayVersion(value = "5.0.0")
        private Integer nbdServerPort;
        @GrayVersion(value = "5.0.0")
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
        private String accountUuid;
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;
        @GrayVersion(value = "5.0.0")
        private long vmInternalId;
        @GrayVersion(value = "5.0.0")
        private String vmName;
        @GrayVersion(value = "5.0.0")
        private String imagePlatform;
        @GrayVersion(value = "5.0.0")
        private String imageArchitecture;
        @GrayVersion(value = "5.0.0")
        private long memory;
        @GrayVersion(value = "5.0.0")
        private long maxMemory;
        @GrayVersion(value = "5.0.0")
        private long reservedMemory;
        @GrayVersion(value = "5.0.0")
        private int cpuNum;
        @GrayVersion(value = "5.0.0")
        private int maxVcpuNum;
        @GrayVersion(value = "5.0.0")
        private long cpuSpeed;
        // cpu topology
        @GrayVersion(value = "5.0.0")
        private Integer socketNum;
        @GrayVersion(value = "5.0.0")
        private Integer cpuOnSocket;
        // set thread per core default 1 to keep backward compatibility
        @GrayVersion(value = "5.0.0")
        private Integer threadsPerCore = 1;
        @GrayVersion(value = "5.0.0")
        private List<String> bootDev;
        @GrayVersion(value = "5.0.0")
        private VolumeTO rootVolume;
        @GrayVersion(value = "5.0.0")
        private VirtualDeviceInfo memBalloon;
        @GrayVersion(value = "5.0.0")
        private List<IsoTO> bootIso = new ArrayList<>();
        @GrayVersion(value = "5.0.0")
        private List<CdRomTO> cdRoms = new ArrayList<>();
        @GrayVersion(value = "5.0.0")
        private List<VolumeTO> dataVolumes;
        @GrayVersion(value = "5.0.0")
        private List<VolumeTO> cacheVolumes;
        @GrayVersion(value = "5.0.0")
        private List<VolumeTO> Volumes;
        @GrayVersion(value = "5.0.0")
        private List<NicTO> nics;
        @GrayVersion(value = "5.0.0")
        private long timeout;
        @GrayVersion(value = "5.0.0")
        private Map<String, Object> addons;
        @GrayVersion(value = "5.0.0")
        private boolean instanceOfferingOnlineChange;
        @GrayVersion(value = "5.0.0")
        private String nestedVirtualization;
        @GrayVersion(value = "5.0.0")
        private String hostManagementIp;
        @GrayVersion(value = "5.0.0")
        private String clock;
        @GrayVersion(value = "5.0.0")
        private String clockTrack;
        @GrayVersion(value = "5.0.0")
        private boolean useNuma;
        @GrayVersion(value = "5.0.0")
        private String MemAccess;
        @GrayVersion(value = "5.0.0")
        private boolean usbRedirect;
        @GrayVersion(value = "5.0.0")
        private boolean enableSecurityElement;
        @GrayVersion(value = "5.0.0")
        private boolean useBootMenu;
        @GrayVersion(value = "5.0.0")
        private Integer bootMenuSplashTimeout;
        @GrayVersion(value = "5.0.0")
        private boolean createPaused;
        @GrayVersion(value = "5.0.0")
        private boolean kvmHiddenState;
        @GrayVersion(value = "5.0.0")
        private boolean vmPortOff;
        @GrayVersion(value = "5.0.0")
        private String vmCpuModel;
        @GrayVersion(value = "5.0.0")
        private boolean emulateHyperV;

        // hyperv features
        @GrayVersion(value = "5.0.0")
        private boolean hypervClock;
        @GrayVersion(value = "5.0.0")
        private String vendorId;

        // suspend features
        @GrayVersion(value = "5.0.0")
        private boolean suspendToRam;
        @GrayVersion(value = "5.0.0")
        private boolean suspendToDisk;

        @GrayVersion(value = "5.0.0")
        private boolean additionalQmp;
        @GrayVersion(value = "5.0.0")
        private boolean isApplianceVm;
        @GrayVersion(value = "5.0.0")
        private String systemSerialNumber;
        @GrayVersion(value = "5.0.0")
        private String bootMode;
        // used when bootMode == 'UEFI'
        @GrayVersion(value = "5.0.0")
        private boolean secureBoot;
        @GrayVersion(value = "5.0.0")
        private boolean fromForeignHypervisor;
        @GrayVersion(value = "5.0.0")
        private String machineType;
        @GrayVersion(value = "5.0.0")
        private Integer pciePortNums;
        @GrayVersion(value = "5.0.0")
        private Integer predefinedPciBridgeNum;
        @GrayVersion(value = "5.0.0")
        private boolean useHugePage;
        @GrayVersion(value = "5.0.0")
        private String chassisAssetTag;
        @GrayVersion(value = "5.0.0")
        private PriorityConfigStruct priorityConfigStruct;
        @GrayVersion(value = "5.0.0")
        private String memorySnapshotPath;
        @GrayVersion(value = "5.0.0")
        private boolean coloPrimary;
        @GrayVersion(value = "5.0.0")
        private boolean coloSecondary;
        @GrayVersion(value = "5.0.0")
        private boolean consoleLogToFile;
        @GrayVersion(value = "5.0.0")
        private boolean acpi;
        @GrayVersion(value = "5.0.0")
        private boolean x2apic = true;
        // cpuid hypervisor feature
        @GrayVersion(value = "5.0.0")
        private boolean cpuHypervisorFeature = true;
        @GrayVersion(value = "5.0.0")
        private List<String> oemStrings = new ArrayList<>();

        // TODO: only for test
        @GrayVersion(value = "5.0.0")
        private boolean useColoBinary;

        public void setSocketNum(Integer socketNum) {
            this.socketNum = socketNum;
        }

        public void setCpuOnSocket(Integer cpuOnSocket) {
            this.cpuOnSocket = cpuOnSocket;
        }

        public void setThreadsPerCore(Integer threadsPerCore) {
            this.threadsPerCore = threadsPerCore;
        }

        public int getMaxVcpuNum() {
            return maxVcpuNum;
        }

        public void setMaxVcpuNum(int maxVcpuNum) {
            this.maxVcpuNum = maxVcpuNum;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public List<String> getOemStrings() {
            return oemStrings;
        }

        public void setOemStrings(List<String> oemStrings) {
            this.oemStrings = oemStrings.stream().distinct().collect(Collectors.toList());;
        }

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

        public boolean isSecureBoot() {
            return secureBoot;
        }

        public void setSecureBoot(boolean secureBoot) {
            this.secureBoot = secureBoot;
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

        public Integer getBootMenuSplashTimeout() {
            return bootMenuSplashTimeout;
        }

        public void setBootMenuSplashTimeout(Integer bootMenuSplashTimeout) {
            this.bootMenuSplashTimeout = bootMenuSplashTimeout;
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

        public boolean isEnableSecurityElement() {
            return enableSecurityElement;
        }

        public void setEnableSecurityElement(boolean enableSecurityElement) {
            this.enableSecurityElement = enableSecurityElement;
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

        public long getReservedMemory() {
            return reservedMemory;
        }

        public void setReservedMemory(long reservedMemory) {
            this.reservedMemory = reservedMemory;
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

        public Integer getSocketNum() {
            return socketNum;
        }

        public void setSocketNum(int socketNum) {
            this.socketNum = socketNum;
        }

        public Integer getCpuOnSocket() {
            return cpuOnSocket;
        }

        public void setCpuOnSocket(int cpuOnSocket) {
            this.cpuOnSocket = cpuOnSocket;
        }

        public Integer getThreadsPerCore() {
            return threadsPerCore;
        }

        public void setThreadsPerCore(int threadsPerCore) {
            this.threadsPerCore = threadsPerCore;
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

        public VirtualDeviceInfo getMemBalloon() {
            return memBalloon;
        }

        public void setMemBalloon(VirtualDeviceInfo memBalloon) {
            this.memBalloon = memBalloon;
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

        public boolean getAcpi() {
            return acpi;
        }

        public void setAcpi(boolean acpi) {
            this.acpi = acpi;
        }

        public boolean getX2apic() {
            return x2apic;
        }

        public void setX2apic(boolean x2apic) {
            this.x2apic = x2apic;
        }

        public boolean isCpuHypervisorFeature() {
            return cpuHypervisorFeature;
        }

        public void setCpuHypervisorFeature(boolean cpuHypervisorFeature) {
            this.cpuHypervisorFeature = cpuHypervisorFeature;
        }

        public boolean isHypervClock() {
            return hypervClock;
        }

        public void setHypervClock(boolean hypervClock) {
            this.hypervClock = hypervClock;
        }

        public String getVendorId() {
            return vendorId;
        }

        public void setVendorId(String vendorId) {
            this.vendorId = vendorId;
        }

        public boolean isSuspendToRam() {
            return suspendToRam;
        }

        public void setSuspendToRam(boolean suspendToRam) {
            this.suspendToRam = suspendToRam;
        }

        public boolean isSuspendToDisk() {
            return suspendToDisk;
        }

        public void setSuspendToDisk(boolean suspendToDisk) {
            this.suspendToDisk = suspendToDisk;
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

    public static class StartVmResponse extends VmDevicesInfoResponse {
    }

    public static class VmDevicesInfoResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private List<VmNicInfo> nicInfos;
        @GrayVersion(value = "5.0.0")
        private List<VirtualDeviceInfo> virtualDeviceInfoList;
        @GrayVersion(value = "5.0.0")
        private VirtualDeviceInfo memBalloonInfo;
        @GrayVersion(value = "5.0.0")
        private VirtualizerInfoTO virtualizerInfo;

        public VirtualDeviceInfo getMemBalloonInfo() {
            return memBalloonInfo;
        }

        public void setMemBalloonInfo(VirtualDeviceInfo memBalloonInfo) {
            this.memBalloonInfo = memBalloonInfo;
        }

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

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;
        }
    }

    public static class SyncVmDeviceInfoCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmInstanceUuid;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class SyncVmDeviceInfoResponse extends VmDevicesInfoResponse {
    }

    public static class VmNicInfo {
        private String macAddress;
        private DeviceAddress deviceAddress;

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public DeviceAddress getDeviceAddress() {
            return deviceAddress;
        }

        public void setDeviceAddress(DeviceAddress deviceAddress) {
            this.deviceAddress = deviceAddress;
        }
    }

    public static class ChangeCpuMemoryCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private int cpuNum;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private int cpuNum;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private int cpuNum;

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }
    }

    public static class IncreaseMemoryCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private long memorySize;

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class ScanVmPortCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String ip;
        @GrayVersion(value = "5.0.0")
        private String brname;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private Map<String, String> portStatus;

        public Map<String, String> getPortStatus() {
            return portStatus;
        }

        public void setPortStatus(Map<String, String> portStatus) {
            this.portStatus = portStatus;
        }
    }

    public static class GetVncPortCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class GetVncPortResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private int port;
        @GrayVersion(value = "5.0.0")
        private String protocol;
        @GrayVersion(value = "5.0.0")
        private Integer vncPort;
        @GrayVersion(value = "5.0.0")
        private Integer spicePort;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
        private String type;
        @GrayVersion(value = "5.0.0")
        private long timeout;
        private List<VmNicInventory> vmNics;

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

        public List<VmNicInventory> getVmNics() {
            return vmNics;
        }

        public void setVmNics(List<VmNicInventory> vmNics) {
            this.vmNics = vmNics;
        }
    }

    public static class StopVmResponse extends AgentResponse {
    }

    public static class PauseVmCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
        private long timeout;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private VirtualizerInfoTO virtualizerInfo;

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;
        }
    }

    public static class DestroyVmCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
        private List<VmNicInventory> vmNics;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public List<VmNicInventory> getVmNics() {
            return vmNics;
        }

        public void setVmNics(List<VmNicInventory> vmNics) {
            this.vmNics = vmNics;
        }

    }

    public static class DestroyVmResponse extends AgentResponse {
    }

    public static class GetVmFirstBootDeviceCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class GetVmFirstBootDeviceResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private String firstBootDevice;

        public String getFirstBootDevice() {
            return firstBootDevice;
        }

        public void setFirstBootDevice(String firstBootDevice) {
            this.firstBootDevice = firstBootDevice;
        }
    }

    public static class GetVmDeviceAddressCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String uuid;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
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

    public static class GetVirtualizerInfoCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private List<String> vmUuids;

        public List<String> getVmUuids() {
            return vmUuids;
        }

        public void setVmUuids(List<String> vmUuids) {
            this.vmUuids = vmUuids;
        }
    }

    public static class GetVirtualizerInfoRsp extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private VirtualizerInfoTO hostInfo;
        @GrayVersion(value = "5.0.0")
        private List<VirtualizerInfoTO> vmInfoList;

        public VirtualizerInfoTO getHostInfo() {
            return hostInfo;
        }

        public void setHostInfo(VirtualizerInfoTO hostInfo) {
            this.hostInfo = hostInfo;
        }

        public List<VirtualizerInfoTO> getVmInfoList() {
            return vmInfoList;
        }

        public void setVmInfoList(List<VirtualizerInfoTO> vmInfoList) {
            this.vmInfoList = vmInfoList;
        }
    }

    public static class VirtualizerInfoTO {
        private String uuid;
        private String virtualizer;
        private String version;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public void setVirtualizer(String virtualizer) {
            this.virtualizer = virtualizer;
        }

        public String getVirtualizer() {
            return virtualizer;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
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
        @GrayVersion(value = "5.0.0")
        private HashMap<String, String> states;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private List<VmNicSecurityTO> vmNicTOs;
        @GrayVersion(value = "5.0.0")
        private Map<String, List<RuleTO>> ruleTOs;
        @GrayVersion(value = "5.0.0")
        private Map<String, List<RuleTO>> ip6RuleTOs;

        public RefreshAllRulesOnHostCmd() {
            vmNicTOs = new ArrayList<VmNicSecurityTO>();
            ruleTOs = new HashMap<String, List<RuleTO>>();
            ip6RuleTOs = new HashMap<String, List<RuleTO>>();
        }

        public List<VmNicSecurityTO> getVmNicTOs() {
            return vmNicTOs;
        }

        public void setVmNicTOs(List<VmNicSecurityTO> vmNicTOs) {
            this.vmNicTOs = vmNicTOs;
        }

        public Map<String, List<RuleTO>> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(Map<String, List<RuleTO>> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public Map<String, List<RuleTO>> getIp6RuleTOs() {
            return ip6RuleTOs;
        }

        public void setIp6RuleTOs(Map<String, List<RuleTO>> ip6RuleTOs) {
            this.ip6RuleTOs = ip6RuleTOs;
        }
    }

    public static class RefreshAllRulesOnHostResponse extends AgentResponse {
    }

    public static class CheckDefaultSecurityGroupCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        Boolean skipIpv6;
    }

    public static class CheckDefaultSecurityGroupResponse extends AgentResponse {

    }

    public static class UpdateGroupMemberCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        Boolean skipIpv6;
    }

    public static class CleanupUnusedRulesOnHostResponse extends AgentResponse {
    }


    public static class ApplySecurityGroupRuleCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private List<VmNicSecurityTO> vmNicTOs;
        @GrayVersion(value = "5.0.0")
        private Map<String, List<RuleTO>> ruleTOs;
        @GrayVersion(value = "5.0.0")
        private Map<String, List<RuleTO>> ip6RuleTOs;

        public ApplySecurityGroupRuleCmd() {
            vmNicTOs = new ArrayList<VmNicSecurityTO>();
            ruleTOs = new HashMap<String, List<RuleTO>>();
            ip6RuleTOs = new HashMap<String, List<RuleTO>>();
        }

        public List<VmNicSecurityTO> getVmNicTOs() {
            return vmNicTOs;
        }

        public void setVmNicTOs(List<VmNicSecurityTO> vmNicTOs) {
            this.vmNicTOs = vmNicTOs;
        }

        public Map<String, List<RuleTO>> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(Map<String, List<RuleTO>> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public Map<String, List<RuleTO>> getIp6RuleTOs() {
            return ip6RuleTOs;
        }

        public void setIp6RuleTOs(Map<String, List<RuleTO>> ip6RuleTOs) {
            this.ip6RuleTOs = ip6RuleTOs;
        }
    }

    public static class ApplySecurityGroupRuleResponse extends AgentResponse {
    }

    public static class MigrateVmCmd extends AgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private String destHostIp;
        @GrayVersion(value = "5.0.0")
        private String destHostManagementIp;
        @GrayVersion(value = "5.0.0")
        private String storageMigrationPolicy;
        @GrayVersion(value = "5.0.0")
        private String srcHostIp;
        @GrayVersion(value = "5.0.0")
        private boolean useNuma;
        @GrayVersion(value = "5.0.0")
        private boolean migrateFromDestination;
        @GrayVersion(value = "5.0.0")
        private boolean autoConverge;
        @GrayVersion(value = "5.0.0")
        private Integer downTime;
        @GrayVersion(value = "5.0.0")
        private boolean xbzrle;
        @GrayVersion(value = "5.0.0")
        private List<String> vdpaPaths;
        @GrayVersion(value = "5.0.0")
        private List<NicTO> nics;
        @GrayVersion(value = "5.0.0")
        private Map<String, VolumeTO> disks;  // A map from old install path to new volume
        @GrayVersion(value = "5.0.0")
        private boolean reload;
        @GrayVersion(value = "5.0.0")
        private long bandwidth;

        public Integer getDownTime() {
            return downTime;
        }

        public void setDownTime(Integer downTime) {
            this.downTime = downTime;
        }

        public boolean isReload() {
            return reload;
        }

        public void setReload(boolean reload) {
            this.reload = reload;
        }

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

        public Map<String, VolumeTO> getDisks() {
            return disks;
        }

        public void setDisks(Map<String, VolumeTO> disks) {
            this.disks = disks;
        }

        public String getDestHostManagementIp() {
            return destHostManagementIp;
        }

        public void setDestHostManagementIp(String destHostManagementIp) {
            this.destHostManagementIp = destHostManagementIp;
        }

        public long getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(long bandwidth) {
            this.bandwidth = bandwidth;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class MigrateVmResponse extends AgentResponse {
    }

    public static class VmGetCpuXmlCmd extends AgentCommand{
    }

    public static class VmGetCpuXmlResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        private String cpuXml;
        @GrayVersion(value = "5.0.0")
        private String cpuModelName;

        public String getCpuModelName() {
            return cpuModelName;
        }

        public void setCpuModelName(String cpuModelName) {
            this.cpuModelName = cpuModelName;
        }

        public String getCpuXml() {
            return cpuXml;
        }

        public void setCpuXml(String cpuXml) {
            this.cpuXml = cpuXml;
        }
    }

    public static class VmCompareCpuFunctionCmd extends AgentCommand{
        @GrayVersion(value = "5.0.0")
        private String cpuXml;

        public String getCpuXml() {
            return cpuXml;
        }

        public void setCpuXml(String cpuXml) {
            this.cpuXml = cpuXml;
        }
    }

    public static class VmCompareCpuFunctionResponse extends AgentResponse {
    }

    public static class MergeSnapshotRsp extends AgentResponse {
    }

    public static class MergeSnapshotCmd extends AgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private VolumeTO volume;
        @GrayVersion(value = "5.0.0")
        private String srcPath;
        @GrayVersion(value = "5.0.0")
        private String destPath;
        @GrayVersion(value = "5.0.0")
        private boolean fullRebase;

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
    }

    public static class CheckSnapshotCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private String volumeUuid;
        @GrayVersion(value = "5.0.0")
        private String currentInstallPath;
        @GrayVersion(value = "5.0.0")
        private List<String> excludeInstallPaths;
        @GrayVersion(value = "5.0.0")
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

    public static class BlockCommitVolumeCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private String volumeUuid;
        private VolumeTO volume;
        private String top;
        private String base;

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

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getTop() {
            return top;
        }

        public void setTop(String top) {
            this.top = top;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }
    }

    public static class BlockCommitVolumeResponse extends AgentResponse {
        @Validation
        private String newVolumeInstallPath;
        @Validation(notZero = true)
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class TakeSnapshotCmd extends AgentCommand implements HasThreadContext {
        @GrayVersion(value = "5.0.0")
        private String vmUuid;
        @GrayVersion(value = "5.0.0")
        private String volumeUuid;
        @GrayVersion(value = "5.0.0")
        private VolumeTO volume;
        @GrayVersion(value = "5.0.0")
        private String installPath;
        @GrayVersion(value = "5.0.0")
        private boolean fullSnapshot;
        @GrayVersion(value = "5.0.0")
        private String volumeInstallPath;
        @GrayVersion(value = "5.0.0")
        private String newVolumeUuid;
        @GrayVersion(value = "5.0.0")
        private String newVolumeInstallPath;
        @GrayVersion(value = "5.0.0")
        private boolean online;
        private long timeout;

        // for baremetal2 instance
        private boolean isBaremetal2InstanceOnlineSnapshot;

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

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
        @GrayVersion(value = "5.0.0")
        private String newVolumeInstallPath;
        @Validation
        @GrayVersion(value = "5.0.0")
        private String snapshotInstallPath;
        @Validation(notZero = true)
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String hostname;
        @GrayVersion(value = "5.0.0")
        private int port;
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        private String hostname;
        @GrayVersion(value = "5.0.0")
        private int port;
        @GrayVersion(value = "5.0.0")
        private String target;
        @GrayVersion(value = "5.0.0")
        private String chapUsername;
        @NoLogging
        @GrayVersion(value = "5.0.0")
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
        @GrayVersion(value = "5.0.0")
        public IsoTO iso;
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
    }

    public static class AttachIsoRsp extends AgentResponse {
    }

    public static class DetachIsoCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
        @GrayVersion(value = "5.0.0")
        public String isoUuid;
        @GrayVersion(value = "5.0.0")
        public int deviceId;
    }

    public static class DetachIsoRsp extends AgentResponse {

    }

    public static class UpdateHostOSCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String hostUuid;
        @GrayVersion(value = "5.0.0")
        public String excludePackages;
        @GrayVersion(value = "5.0.0")
        public String updatePackages;
        @GrayVersion(value = "5.0.0")
        public String releaseVersion;
        @GrayVersion(value = "5.0.0")
        public boolean enableExpRepo;
    }

    public static class UpdateHostOSRsp extends AgentResponse {
    }

    public static class UpdateDependencyCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String hostUuid;
        @GrayVersion(value = "5.0.0")
        public boolean enableExpRepo;
        @GrayVersion(value = "5.0.0")
        public String excludePackages;
        @GrayVersion(value = "5.0.0")
        public String updatePackages;
        @GrayVersion(value = "5.0.0")
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

    public static class ReportHostStopEventCmd {
        public String hostIp;
    }

    public static class ShutdownHostCmd extends AgentCommand {
    }

    public static class RebootHostCmd extends AgentCommand {
    }

    public static class ShutdownHostResponse extends AgentResponse {
    }

    public static class RebootHostResponse extends AgentResponse {
    }

    public static class UpdateSpiceChannelConfigCmd extends AgentCommand {
    }

    public static class UpdateSpiceChannelConfigResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public boolean restartLibvirt = false;
    }

    public static class PrimaryStorageCommand extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String primaryStorageUuid;
    }

    public static class CancelCmd extends AgentCommand implements CancelCommand {
        @GrayVersion(value = "5.0.0")
        private String cancellationApiId;
        @GrayVersion(value = "5.0.0")
        private Integer times;
        @GrayVersion(value = "5.0.0")
        private Integer interval;

        public Integer getTimes() {
            return times;
        }

        public void setTimes(Integer times) {
            this.times = times;
        }

        public Integer getInterval() {
            return interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

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
        @GrayVersion(value = "5.0.0")
        public String dirPath;
    }

    public static class GetDevCapacityResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public long totalSize;
        @GrayVersion(value = "5.0.0")
        public long availableSize;
        @GrayVersion(value = "5.0.0")
        public long dirSize;
    }

    public static class CheckFileOnHostCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public Set<String> paths;
        @GrayVersion(value = "5.0.0")
        public boolean md5Return;
    }

    public static class CheckFileOnHostResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public Map<String, String> existPaths;
    }

    public static class GetHostNUMATopologyCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String HostUuid;

        public void setHostUuid(String hostUuid) {
            HostUuid = hostUuid;
        }

        public String getHostUuid() {
            return HostUuid;
        }
    }

    public static class GetHostNUMATopologyResponse extends AgentResponse {
        @GrayVersion(value = "5.0.0")
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

    public static class AttachVolumeCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumePrimaryStorageUuid;
        @GrayVersion(value = "5.0.0")
        public String volumeInstallPath;
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String device;
    }

    public static class AttachVolumeRsp extends AgentResponse {
        @GrayVersion(value = "5.0.0")
        public String device;
    }

    public static class DetachVolumeCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String volumeInstallPath;
        @GrayVersion(value = "5.0.0")
        public String mountPath;
        @GrayVersion(value = "5.0.0")
        public String device;
    }

    public static class DetachVolumeRsp extends AgentResponse {
    }

    public static class VmFstrimCmd extends AgentCommand {
        @GrayVersion(value = "5.0.0")
        public String vmUuid;
    }

    public static class VmFstrimRsp extends AgentResponse {
    }
    public static class TakeVmConsoleScreenshotCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class TakeVmConsoleScreenshotRsp extends AgentResponse {
        private String imageData;

        public String getImageData() {
            return imageData;
        }

        public void setImageData(String imageData) {
            this.imageData = imageData;
        }
    }
}
