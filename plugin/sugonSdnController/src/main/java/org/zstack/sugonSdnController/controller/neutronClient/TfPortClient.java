package org.zstack.sugonSdnController.controller.neutronClient;

import org.apache.commons.collections.CollectionUtils;
import org.bouncycastle.util.IPAddress;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;
import org.zstack.sugonSdnController.controller.SugonSdnControllerGlobalProperty;
import org.zstack.sugonSdnController.controller.api.*;
import org.zstack.sugonSdnController.controller.api.types.*;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.*;


public class TfPortClient {
    private static final CLogger logger = Utils.getLogger(TfPortClient.class);

    private ApiConnector apiConnector;

    private String tenantId;

    public TfPortClient(){
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(
                SdnControllerVO_.vendorType,
                SdnControllerConstant.TF_CONTROLLER).find();
        if (sdn == null){
            throw new RuntimeException("Can not find a tf sdn controller.");
        }
        apiConnector = ApiConnectorFactory.build(sdn.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
        if (apiConnector == null) {
            throw new RuntimeException(String.format("Can not connect to tf sdn controller: %s.", sdn.getIp()));
        }
        tenantId = StringDSL.transToTfUuid(sdn.getAccountUuid());
    }

    public TfPortResponse createPort(String l2Id, String l3Id, String mac, String ip,
                                     String vmInventeryId, String tfPortUuid, String vmName) {
        TfPortRequestResource requestPortResourceEntity = new TfPortRequestResource();
        requestPortResourceEntity.setNetworkId(l2Id);
        requestPortResourceEntity.setSubnetId(l3Id);
        requestPortResourceEntity.setTenantId(tenantId);
        requestPortResourceEntity.setMacAddress(mac);
        requestPortResourceEntity.setDeviceId(vmInventeryId);

        TfPortIpEntity ipEntity = new TfPortIpEntity();
        ipEntity.setIpAddress(ip);
        ipEntity.setSubnetId(l3Id);
        List<TfPortIpEntity> ipEntities = new ArrayList<>();
        ipEntities.add(ipEntity);
        requestPortResourceEntity.setFixdIps(ipEntities);
        try {
            VirtualNetwork netObj = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, l2Id);
            if (netObj == null) {
                throw new RuntimeException(String.format("Can not find tf virtualnetwork: %s.", l2Id));
            }
            //if mac-address is specified, check against the exisitng ports
            //to see if there exists a port with the same mac-address
            if (!Objects.isNull(mac)) {
                List<VirtualMachineInterface> ports = (List<VirtualMachineInterface>) apiConnector.list(
                        VirtualMachineInterface.class, Arrays.asList("default-domain", tenantId));

                for (VirtualMachineInterface port : ports) {
                    MacAddressesType macAddressesType = null;
                    if (port != null && port.getMacAddresses() != null) {
                        macAddressesType = port.getMacAddresses();
                        List<String> macAddresses = macAddressesType.getMacAddress();
                        for (String macAddress : macAddresses) {
                            if (macAddress.equals(mac)) {
                                throw new RuntimeException("MacAddressInUse: " + mac);
                            }
                        }
                    }
                }
            }
            // initialize port object
            VirtualMachineInterface port;
            try {
                port = getTfPortObject(requestPortResourceEntity, netObj, tfPortUuid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PermType2 perms2 = new PermType2();
            perms2.setOwner(tenantId);
            port.setPerms2(perms2);
            port.setDisplayName(vmName);
            // always request for v4 and v6 ip object and handle the failure
            // create the object
            Status result = apiConnector.create(port);
            if (!result.isSuccess()) {
                throw new RuntimeException(String.format("Failed to create tf VirtualMachineInterface: %s, reason: %s",
                        port.getUuid(), result.getMsg()));
            }
            // add support, nova boot --nic subnet-id=subnet_uuid
            VirtualMachineInterface realPort = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, port.getUuid());

            if (ip != null) {
                try {
                    portCreateInstanceIp(netObj, realPort, l3Id, ip);
                } catch (Exception e) {
                    try {
                        apiConnector.delete(realPort);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    throw new RuntimeException(e);
                }
            } else if (netObj.getNetworkIpam() != null && netObj.getNetworkIpam().size() > 0) {
                String errmsg = "Bad request trying to create IP instance.";
                boolean ipv4PortDelete = false;
                try {
                    Status ip_result = portCreateInstanceIp(netObj, realPort, l3Id, ip);
                    if (!ip_result.isSuccess()) {
                        ipv4PortDelete = true;
                        logger.error("Tf instance ip create failed.");
                    }
                } catch (Exception e) {
                    ipv4PortDelete = true;
                    // failure in creating the instance ip. Roll back.
                    try {
                        apiConnector.delete(realPort);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    throw new RuntimeException(errmsg);
                }
                if (ipv4PortDelete) {
                    try {
                        apiConnector.delete(realPort);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            VirtualMachineInterface newestPort = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, realPort.getUuid());
            return getPortResponse(newestPort);
        } catch (Exception e) {
            throw new RuntimeException("TF api call failed: " + e);
        }
    }

    private TfPortResponse getPortResponse(VirtualMachineInterface portObj) throws IOException {
        TfPortResponse tfPortResponse = new TfPortResponse();
        tfPortResponse.setPortId(portObj.getUuid());
        tfPortResponse.setCode(200);
        tfPortResponse.setMacAddress(portObj.getMacAddresses().getMacAddress().get(0));
        TfPortIpEntity ipEntity = new TfPortIpEntity();
        List<ObjectReference<ApiPropertyBase>> ipBackRefs = portObj.getInstanceIpBackRefs();
        if (ipBackRefs != null && ipBackRefs.size() > 0) {
            for (ObjectReference<ApiPropertyBase> ipBackRef : ipBackRefs) {
                InstanceIp ipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, ipBackRef.getUuid());
                String ipAddr = ipObj != null ? ipObj.getAddress() : null;
                String subnetId = Objects.requireNonNull(ipObj).getSubnetUuid();
                ipEntity.setIpAddress(ipAddr);
                ipEntity.setSubnetId(subnetId);
            }
        }
        tfPortResponse.setFixedIps(Collections.singletonList(ipEntity));

        return tfPortResponse;
    }

    private Status portCreateInstanceIp(VirtualNetwork virtualNetwork, VirtualMachineInterface port,
                                      String subnetId, String ip) throws IOException {
        InstanceIp ipObj = new InstanceIp();
        String ipFamily = "v4";
        if (ip != null) {
            if (IPAddress.isValidIPv6(ip)) {
                ipFamily = "v6";
            }
            ipObj.setAddress(ip);
        }

        String ipName = String.valueOf(UUID.randomUUID());
        ipObj.setUuid(ipName);
        ipObj.setName(ipName);
        ipObj.setSubnetUuid(subnetId);
        ipObj.setVirtualMachineInterface(port);
        List<String> fqName = virtualNetwork.getQualifiedName();
        fqName.add(ipName);
        ipObj.setVirtualNetwork(virtualNetwork);
        ipObj.setFamily(ipFamily);
        // set instance ip ownership to real tenant
        PermType2 permType2 = new PermType2();
        permType2.setOwner(tenantId);
        ipObj.setPerms2(permType2);
        return apiConnector.create(ipObj);
    }

    private VirtualMachineInterface getTfPortObject(TfPortRequestResource requestPortResourceEntity, VirtualNetwork virtualNetwork, String tfPortUUid) throws IOException {
        String projectId = requestPortResourceEntity.getTenantId();
        Project projectObj = (Project) apiConnector.findById(Project.class, projectId);
        IdPermsType idPermsType = new IdPermsType();
        idPermsType.setEnable(true);
        String portUuid = String.valueOf(UUID.randomUUID());
        if (Objects.nonNull(tfPortUUid)){
            portUuid = tfPortUUid;
        }
        VirtualMachineInterface portObj = new VirtualMachineInterface();
        portObj.setUuid(portUuid);
        portObj.setName(portUuid);
        portObj.setIdPerms(idPermsType);
        portObj.setParent(projectObj);
        portObj.setVirtualNetwork(virtualNetwork);
        if (requestPortResourceEntity.getMacAddress() != null) {
            MacAddressesType macAddressesType = new MacAddressesType();
            macAddressesType.addMacAddress(requestPortResourceEntity.getMacAddress());
            portObj.setMacAddresses(macAddressesType);
        }
        if (requestPortResourceEntity.getDeviceId() != null) {
            VirtualMachine instanceObj = ensureInstanceExists(requestPortResourceEntity.getDeviceId(), projectId, false);
            portObj.setVirtualMachine(instanceObj);
        }
        String fixIp = requestPortResourceEntity.getFixdIps() == null ? null: requestPortResourceEntity.getFixdIps().get(0).getIpAddress();
        if (Objects.nonNull(fixIp)) {
            String netId = requestPortResourceEntity.getNetworkId();

            if (ipInUseCheck(fixIp, netId)) {
                throw new RuntimeException("IpAddressInUse: " + fixIp);
            }
        }
        return portObj;
    }

    public boolean ipInUseCheck(String ipAddr, String netId) throws IOException {
        VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, netId);
        if (virtualNetwork != null) {
            List<ObjectReference<ApiPropertyBase>> instanceIpBackRefs = virtualNetwork.getInstanceIpBackRefs();
            if (instanceIpBackRefs != null) {
                List<InstanceIp> ipObjects = (List<InstanceIp>) apiConnector.getObjects(InstanceIp.class, instanceIpBackRefs);
                if (ipObjects != null) {
                    for (InstanceIp ipObj : ipObjects) {
                        if (ipObj.getAddress().equals(ipAddr)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private VirtualMachine ensureInstanceExists(String deviceId, String projectId, boolean baremeetal) throws IOException {
        VirtualMachine instanceObj = null;
        try {
            instanceObj = (VirtualMachine) apiConnector.findById(VirtualMachine.class, deviceId);
            if (instanceObj != null) {
                return instanceObj;
            }
            instanceObj = new VirtualMachine();
            instanceObj.setName(deviceId);
            instanceObj.setUuid(deviceId);
            PermType2 permType2 = new PermType2();
            permType2.setOwner(projectId);
            instanceObj.setPerms2(permType2);
            if (baremeetal) {
                instanceObj.setServerType("baremetal-server");
            } else {
                instanceObj.setServerType("virtual-server");
            }
            apiConnector.create(instanceObj);
        } catch (Exception e) { // Exception......
            VirtualMachine dbInstanceObj = null;
            if (instanceObj.getUuid() != null) {
                dbInstanceObj = (VirtualMachine) apiConnector.findById(VirtualMachine.class, instanceObj.getUuid());
            } else {
                dbInstanceObj = (VirtualMachine) apiConnector.findByFQN(VirtualMachine.class, instanceObj.getName());

            }
            if (dbInstanceObj != null) {
                if (baremeetal && !Objects.equals(dbInstanceObj.getServerType(), "baremetal-server")) {
                    apiConnector.update(instanceObj);
                } else {
                    instanceObj = dbInstanceObj;
                }
            }
        }
        return instanceObj;
    }

    public TfPortResponse getVirtualMachineInterface(String portId) {
        try {
            VirtualMachineInterface port = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, portId);
            if (port != null){
                return getPortResponse(port);
            }else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<VirtualMachineInterface> getVirtualMachineInterfaceDetail() {
        try {
            List<VirtualMachineInterface> result = new ArrayList<>();
            List<VirtualMachineInterface> ports = (List<VirtualMachineInterface>) apiConnector.list(
                    VirtualMachineInterface.class, Arrays.asList("default-domain", tenantId));

            for (VirtualMachineInterface port : ports) {
                VirtualMachineInterface detail = (VirtualMachineInterface) apiConnector.findById(
                        VirtualMachineInterface.class, port.getUuid());
                if (detail == null) {
                    continue;
                }
                result.add(detail);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TfPortResponse deletePort(String portId) {
        TfPortResponse response = new TfPortResponse();
        try {
            VirtualMachineInterface portObj = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portId);
            if (portObj == null) {
                response.setCode(200);
                return response;
            }
            // release instance IP address
            List<ObjectReference<ApiPropertyBase>> iipBackRefs = portObj.getInstanceIpBackRefs();
            if (iipBackRefs != null && iipBackRefs.size() > 0) {
                for (ObjectReference<ApiPropertyBase> iipBackRef : iipBackRefs) {
                    InstanceIp iipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, iipBackRef.getUuid());
                    // in case of shared ip only delete the link to the VMI
                    if (iipObj != null) {
                        iipObj.removeVirtualMachineInterface(portObj);
                        List<ObjectReference<ApiPropertyBase>> virtualMachineInterface = iipObj.getVirtualMachineInterface();
                        if (virtualMachineInterface == null || virtualMachineInterface.size() == 0) {
                            Status delResult = apiConnector.delete(InstanceIp.class, iipBackRef.getUuid());
                            if (!delResult.isSuccess()) {
                                throw new RuntimeException("Tf instance ip delete failed: " + iipBackRef.getUuid());
                            }
                        } else {
                            apiConnector.update(iipObj);
                        }
                    }
                }
            }
            // disassociate any floating IP used by instance
            List<ObjectReference<ApiPropertyBase>> fipBackRefs = portObj.getFloatingIpBackRefs();
            if (CollectionUtils.isNotEmpty(fipBackRefs)) {
                for (ObjectReference<ApiPropertyBase> fipBackRef : fipBackRefs) {
                    FloatingIp fipObj = getTfFloatingipObject(fipBackRef.getUuid());
                    if (fipObj != null) {
                        apiConnector.update(fipObj);
                    }
                }
            }

            apiConnector.delete(VirtualMachineInterface.class, portId);
            // delete VirtualMachine if this was the last port
            String instanceId;
            if (Objects.equals(portObj.getParentType(), "virtual-machine")) {
                instanceId = portObj.getParentUuid();
            } else {
                List<ObjectReference<ApiPropertyBase>> vmRefs = portObj.getVirtualMachine();
                if (vmRefs != null && vmRefs.size() > 0) {
                    instanceId = vmRefs.get(0).getUuid();
                } else {
                    instanceId = null;
                }
            }
            if (instanceId != null) {
                VirtualMachine vm = (VirtualMachine) apiConnector.findById(VirtualMachine.class, instanceId);
                if (CollectionUtils.isEmpty(vm.getVirtualMachineInterfaceBackRefs())) {
                    apiConnector.delete(VirtualMachine.class, instanceId);
                }
            }
            response.setCode(200);
            return response;

        } catch (Exception e) {
            response.setCode(500);
            response.setMsg(String.format("Delete tf virtualMachineInterface %s failed, reason: %s",
                    portId, e.getMessage()));
            return response;
        }
    }

    private FloatingIp getTfFloatingipObject(String uuid) throws IOException {
        FloatingIp fipObj = (FloatingIp) apiConnector.findById(FloatingIp.class, uuid);
        if (fipObj == null) {
            return null;
        }
        fipObj.clearVirtualMachineInterface();
        fipObj.setFixedIpAddress(null);
        return fipObj;
    }

    /**
     * 检查ip是否在已经被占用
     *
     * @param ipAddr   IPv4地址
     * @param subnetId (三层网络)的UUID
     * @return 占用-true; 未占用-false
     */
    public boolean checkTfIpAvailability(String ipAddr, String subnetId) throws IOException {
        L3NetworkVO l3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, subnetId).find();
        VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(
                VirtualNetwork.class, StringDSL.transToTfUuid(l3Network.getL2NetworkUuid()));

        if (Objects.isNull(virtualNetwork)) {
            return false;
        }
        List<ObjectReference<ApiPropertyBase>> instanceIpBackRefs = virtualNetwork.getInstanceIpBackRefs();
        List<ObjectReference<VnSubnetsType>> subnetListRefs = virtualNetwork.getNetworkIpam();

        if (Objects.nonNull(virtualNetwork.getRouterExternal()) && virtualNetwork.getRouterExternal()) {
            // if external network, floating ips.
            List<ObjectReference<ApiPropertyBase>> floatingIpPools = virtualNetwork.getFloatingIpPools();
            if (CollectionUtils.isNotEmpty(floatingIpPools)) {
                for (ObjectReference<ApiPropertyBase> floatingIpPool : floatingIpPools) {
                    FloatingIpPool floatingIpPoolObj = (FloatingIpPool) apiConnector.findById(FloatingIpPool.class, floatingIpPool.getUuid());
                    List<ObjectReference<ApiPropertyBase>> floatingIps = floatingIpPoolObj.getFloatingIps();
                    if (CollectionUtils.isNotEmpty(floatingIps)) {
                        for (ObjectReference<ApiPropertyBase> fip : floatingIps) {
                            FloatingIp fipObj = (FloatingIp) apiConnector.findById(FloatingIp.class, fip.getUuid());
                            if (fipObj.getAddress().equals(ipAddr)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } else { // else instance ips.
            if (CollectionUtils.isNotEmpty(instanceIpBackRefs)) {
                List<InstanceIp> ipObjects = (List<InstanceIp>) apiConnector.getObjects(InstanceIp.class, instanceIpBackRefs);
                // check all instance ips.
                if (CollectionUtils.isNotEmpty(ipObjects)) {
                    for (InstanceIp ipObj : ipObjects) {
                        if (ipObj.getAddress().equals(ipAddr)) {
                            return true;
                        }
                    }
                }
            }
        }

        // check all subnets' gateway/dns service address/host routes/dhcp relay servers .eg
        if (CollectionUtils.isNotEmpty(subnetListRefs)) {
            for (ObjectReference<VnSubnetsType> subnetListRef : subnetListRefs) {
                List<IpamSubnetType> ipamSubnets = subnetListRef.getAttr().getIpamSubnets();
                if (CollectionUtils.isNotEmpty(ipamSubnets)) {
                    for (IpamSubnetType ipamSubnet : ipamSubnets) {
                        List<String> ipamSubnetIpUseList = new ArrayList<>();
                        ipamSubnetIpUseList.add(ipamSubnet.getDefaultGateway());
                        ipamSubnetIpUseList.add(ipamSubnet.getDnsServerAddress());
                        List<String> dnsNameservers = ipamSubnet.getDnsNameservers();
                        if (CollectionUtils.isNotEmpty(dnsNameservers)) {
                            ipamSubnetIpUseList.addAll(dnsNameservers);
                        }
                        List<String> dhcpRelayServer = ipamSubnet.getDhcpRelayServer();
                        if (CollectionUtils.isNotEmpty(dhcpRelayServer)) {
                            ipamSubnetIpUseList.addAll(dhcpRelayServer);
                        }
                        RouteTableType routeTableType = ipamSubnet.getHostRoutes();
                        if (Objects.nonNull(routeTableType)) {
                            List<RouteType> routeTypes = routeTableType.getRoute();
                            if (CollectionUtils.isNotEmpty(routeTypes)) {
                                for (RouteType routeType : routeTypes) {
                                    String ip = routeType.getNextHop();
                                    if (Objects.nonNull(ip)) {
                                        ipamSubnetIpUseList.add(ip);
                                    }
                                }
                            }
                        }
                        if (ipamSubnetIpUseList.contains(ipAddr)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void updateTfPort(String tfPortUUid, String accountId, String deviceId) {
        try {
            VirtualMachineInterface port = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, tfPortUUid);
            VirtualMachine vm = ensureInstanceExists(deviceId, accountId, false);
            port.setVirtualMachine(vm);
            apiConnector.update(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


