package org.zstack.sugonSdnController.controller.neutronClient;

import org.apache.commons.collections.CollectionUtils;
import org.bouncycastle.util.IPAddress;
import org.zstack.core.db.Q;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;
import org.zstack.sugonSdnController.controller.SugonSdnControllerGlobalProperty;
import org.zstack.sugonSdnController.controller.api.*;
import org.zstack.sugonSdnController.controller.api.types.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.*;

public class TfPortClient {
    private static final CLogger logger = Utils.getLogger(TfPortClient.class);

    private ApiConnector apiConnector;

    public ApiConnector getApiConnector() {
        if (Objects.isNull(apiConnector)) {
            SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SdnControllerConstant.TF_CONTROLLER).find();
            apiConnector = ApiConnectorFactory.build(sdn.getIp(), SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
        }
        return apiConnector;
    }

    public TfPortResponse createPort(String l2Id, String l3Id, String mac, String ip, String tenantId, String vmInventeryId,String tfPortUuid) {
        TfPortRequestBody portRequestBodyEO = new TfPortRequestBody();
        TfPortRequestData portRequestDataEO = new TfPortRequestData();
        TfPortRequestContext portRequestContextEO = new TfPortRequestContext();
        portRequestContextEO.setOperation("CREATE");
        portRequestContextEO.setIs_admin("True");
        portRequestContextEO.setTenant_id(tenantId);
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
        portRequestDataEO.setResource(requestPortResourceEntity);
        portRequestBodyEO.setData(portRequestDataEO);
        portRequestBodyEO.setContext(portRequestContextEO);
        VirtualNetwork netObj;
        try {
            netObj = networkRead(l2Id);
        } catch (Exception e) {
            throw new RuntimeException("NetworkNotFound: " + l2Id);
        }
        //if mac-address is specified, check against the exisitng ports
        //to see if there exists a port with the same mac-address
        if (!Objects.isNull(mac)) {
            Map<String, List<String>> macDict = new HashMap<>();
            List<String> macList = new ArrayList<>();
            macList.add(mac);
            macDict.put("mac_address", macList);
            Map<String, Map<String, List<String>>> filters = new HashMap<>();
            filters.put("virtual_machine_interface_mac_addresses", macDict);
            List<VirtualMachineInterface> ports;
            try {
                ports = virtualMachineInterfaceList(tenantId, filters);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ApiConnector apiConnector = getApiConnector();
            for (VirtualMachineInterface port : ports) {
                VirtualMachineInterface byId;
                try {
                    byId = (VirtualMachineInterface) (apiConnector != null ? apiConnector.findById(VirtualMachineInterface.class, port.getUuid()) : null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                MacAddressesType macAddressesType = null;
                if (byId != null) {
                    macAddressesType = byId.getMacAddresses();
                }
                List<String> macAddresses = null;
                if (macAddressesType != null) {
                    macAddresses = macAddressesType.getMacAddress();
                }
                if (macAddresses != null) {
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
            port = portNeutronToVnc(requestPortResourceEntity, netObj , tfPortUuid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PermType2 perms2 = new PermType2();
        perms2.setOwner(tenantId);
        port.setPerms2(perms2);
        ApiConnector apiConnector = getApiConnector();
        // always request for v4 and v6 ip object and handle the failure
        // create the object
        if (apiConnector != null) {
            try {
                apiConnector.create(port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("can not create port~");
        }
        //  add support, nova boot --nic subnet-id=subnet_uuid
        VirtualMachineInterface realPort;
        try {
            realPort = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, port.getUuid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (requestPortResourceEntity.getFixdIps() != null && requestPortResourceEntity.getFixdIps().get(0).getIpAddress() != null) {
            try {
                portCreateInstanceIp(netObj, realPort, requestPortResourceEntity);
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
                portCreateInstanceIp(netObj, realPort, requestPortResourceEntity); // ipv4
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
        VirtualMachineInterface virtualMachineInterface;
        try {
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, realPort.getUuid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            return portVncToNeutorn(virtualMachineInterface);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TfPortResponse portVncToNeutorn(VirtualMachineInterface portObj) throws IOException {
        ApiConnector apiConnector = getApiConnector();
        TfPortResponse tfPortResponse = new TfPortResponse();
        tfPortResponse.setPortId(portObj.getUuid());
        tfPortResponse.setCode(200);
        tfPortResponse.setMacAddress(portObj.getMacAddresses().getMacAddress().get(0));
        TfPortIpEntity ipEntity = new TfPortIpEntity();
        AllowedAddressPairs allowedAddressPairs = portObj.getAllowedAddressPairs();
        if (allowedAddressPairs != null && allowedAddressPairs.getAllowedAddressPair().size() > 0) {
            for (AllowedAddressPair allowedAddressPair : allowedAddressPairs.getAllowedAddressPair()) {
                allowedAddressPair.getIp().getIpPrefix();
            }
        }
        List<ObjectReference<ApiPropertyBase>> ipBackRefs = portObj.getInstanceIpBackRefs();
        if (ipBackRefs != null && ipBackRefs.size() > 0) {
            for (ObjectReference<ApiPropertyBase> ipBackRef : ipBackRefs) {
                String iipUuid = ipBackRef.getUuid();
                InstanceIp ipObj = null;
                if (apiConnector != null) {
                    ipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, iipUuid);
                }
                String ipAddr = ipObj != null ? ipObj.getAddress() : null;
                String subnetId = Objects.requireNonNull(ipObj).getSubnetUuid();
                ipEntity.setIpAddress(ipAddr);
                ipEntity.setSubnetId(subnetId);
            }
        }
        tfPortResponse.setFixedIps(Collections.singletonList(ipEntity));

        return tfPortResponse;
    }

    private void portCreateInstanceIp(VirtualNetwork virtualNetwork, VirtualMachineInterface port, TfPortRequestResource requestPortResourceEntity) throws IOException {
        // 1. find existing ips on port
        // 2. add new ips on port from update body
        // 3. delete old/stale ips on port
        List<ObjectReference<ApiPropertyBase>> instanceIpBackRefs = port.getInstanceIpBackRefs();
        List<TfPortIpEntity> fixdIps = requestPortResourceEntity.getFixdIps();
        Map<String, String> staleIpIds = new HashMap<>();
        ApiConnector apiConnector = getApiConnector();
        if (Objects.nonNull(instanceIpBackRefs)) {
            for (ObjectReference<ApiPropertyBase> iip : instanceIpBackRefs) {
                if (apiConnector != null) {
                    InstanceIp iipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, iip.getUuid());
                    String ipaddr = iipObj.getAddress();
                    staleIpIds.put(ipaddr, iip.getUuid());
                }
            }
        }

        String createdIipIds = "";
        for (TfPortIpEntity fixedIp : fixdIps) {
            String ipaddr = fixedIp.getIpAddress();
            String ipFamily = "v4";
            if (ipaddr != null) {
                // this ip survives to next gen
                staleIpIds.remove(ipaddr);

                if (IPAddress.isValidIPv6(fixedIp.getIpAddress())) {
                    ipFamily = "v6";
                }
            }
            String subnetId = fixedIp.getSubnetId();
            // _self._create_instance_ip
            String ipName = String.valueOf(UUID.randomUUID());
            InstanceIp ipObj = new InstanceIp();
            ipObj.setUuid(ipName);
            if (subnetId != null) {
                ipObj.setSubnetUuid(subnetId);
            }
            ipObj.setVirtualMachineInterface(port);
            ipObj.setVirtualNetwork(virtualNetwork);
            ipObj.setFamily(ipFamily);
            if (ipaddr != null) {
                ipObj.setAddress(ipaddr);
            }
            // set instance ip ownership to real tenant
            PermType2 permType2 = new PermType2();
            String tenantId = requestPortResourceEntity.getTenantId();
            permType2.setOwner(tenantId);
            ipObj.setPerms2(permType2);

            List<String> fqName = virtualNetwork.getQualifiedName();
            fqName.add(ipName);
            ipObj.setName(ipName);

            if (apiConnector != null) {
                apiConnector.create(ipObj); // ipName is id
            }
            createdIipIds.concat(ipName);
        }
        for (String key : staleIpIds.keySet()) {
            apiConnector.delete(InstanceIp.class, staleIpIds.get(key));
        }
    }

    private VirtualMachineInterface portNeutronToVnc(TfPortRequestResource requestPortResourceEntity, VirtualNetwork virtualNetwork,String tfPortUUid) throws IOException {
        String projectId = requestPortResourceEntity.getTenantId();
        Project projectObj = getProjectObj(requestPortResourceEntity);
        IdPermsType idPermsType = new IdPermsType();
        idPermsType.setEnable(true);
        String portUuid = String.valueOf(UUID.randomUUID());
        if(Objects.nonNull(tfPortUUid)){
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
            List<ObjectReference<ApiPropertyBase>> vmRefs = portObj.getVirtualMachine();
            List<ObjectReference<ApiPropertyBase>> deleteVmList = new ArrayList<>();
            if (Objects.nonNull(vmRefs)) {
                for (ObjectReference<ApiPropertyBase> vmRef : vmRefs) {
                    if (!vmRef.getReferredName().get(0).equals(requestPortResourceEntity.getDeviceId())) {
                        deleteVmList.add(vmRef);
                    }
                }
            }
            if (requestPortResourceEntity.getDeviceId() != null) {
                try {
                    VirtualMachine instanceObj = ensureInstanceExists(requestPortResourceEntity.getDeviceId(), projectId, false);
                    portObj.setVirtualMachine(instanceObj);
                } catch (Exception e) {
                    throw new RuntimeException("BadRequest port.");
                }
            }
            if (deleteVmList.size() > 0) {
                ApiConnector apiConnector = getApiConnector();
                if (apiConnector != null) {
                    apiConnector.update(portObj);
                }
                for (ObjectReference<ApiPropertyBase> vmRef : vmRefs) {
                    if (apiConnector != null) {
                        apiConnector.delete(VirtualMachine.class, vmRef.getUuid());
                    }
                }
            }
        }
        if (Objects.nonNull(requestPortResourceEntity.getFixdIps()) && Objects.nonNull(requestPortResourceEntity.getFixdIps().get(0).getIpAddress())) {
            String netId = requestPortResourceEntity.getNetworkId();
            String portObjIps = "";
            for (TfPortIpEntity fixedIp : requestPortResourceEntity.getFixdIps()) {
                if (Objects.nonNull(fixedIp.getIpAddress())) {
                    //read instance ip addrs on port only once
                    if (portObjIps.length() == 0) {
                        List<ObjectReference<ApiPropertyBase>> ipBackRefs = portObj.getInstanceIpBackRefs();
                        if (ipBackRefs != null && ipBackRefs.size() > 0) {
                            for (ObjectReference<ApiPropertyBase> ipBackRef : ipBackRefs) {
                                try {
                                    ApiConnector apiConnector = getApiConnector();
                                    InstanceIp ipObj = null;
                                    if (apiConnector != null) {
                                        ipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, ipBackRef.getUuid());
                                    }
                                    portObjIps.concat(ipObj != null ? ipObj.getAddress() : null);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    String ipAddr = fixedIp.getIpAddress();
                    if (portObjIps.contains(ipAddr)) {
                        continue;
                    }

                    if (ipAddrInNetId(ipAddr, netId)) {
                        throw new RuntimeException("IpAddressInUse");
                    }
                }
            }
        }
        return portObj;
    }

    private boolean ipAddrInNetId(String ipAddr, String netId) throws IOException {
        ApiConnector apiConnector = getApiConnector();

        VirtualNetwork virtualNetwork = null;
        if (apiConnector != null) {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, netId);
        }
        List<ObjectReference<ApiPropertyBase>> instanceIpBackRefs = null;
        if (virtualNetwork != null) {
            instanceIpBackRefs = virtualNetwork.getInstanceIpBackRefs();
        }
        List<InstanceIp> ipObjects = null;
        if (apiConnector != null) {
            ipObjects = (List<InstanceIp>) apiConnector.getObjects(InstanceIp.class, instanceIpBackRefs);
        }
        if (ipObjects != null) {
            for (InstanceIp ipObj : ipObjects) {
                if (ipObj.getAddress().equals(ipAddr)) {
                    return true;
                }
            }
        }
        return false;
    }

    private VirtualMachine ensureInstanceExists(String deviceId, String projectId, boolean baremeetal) throws IOException {
        VirtualMachine instanceObj = new VirtualMachine();
        try {
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
            ApiConnector apiConnector = getApiConnector();
            if (apiConnector != null) {
                try {
                    apiConnector.create(instanceObj);
                } catch (Exception e) {
                    throw new RuntimeException("can not create VirtualMachine~");
                }
            }
        } catch (Exception e) { // Exception......
            ApiConnector apiConnector = getApiConnector();
            VirtualMachine dbInstanceObj = null;
            if (instanceObj.getUuid() != null) {
                if (apiConnector != null) {
                    dbInstanceObj = (VirtualMachine) apiConnector.findById(VirtualMachine.class, instanceObj.getUuid());
                }
            } else {
                if (apiConnector != null) {
                    dbInstanceObj = (VirtualMachine) apiConnector.findByFQN(VirtualMachine.class, instanceObj.getName());
                }
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

    private Project getProjectObj(TfPortRequestResource requestPortResourceEntity) throws IOException {
        ApiConnector apiConnector = getApiConnector();

        if (apiConnector != null) {
            return (Project) apiConnector.findById(Project.class, requestPortResourceEntity.getTenantId());
        } else {
            throw new RuntimeException("can not get project~");
        }
    }

    private List<VirtualMachineInterface> virtualMachineInterfaceList(String tenantId, Map<String, Map<String, List<String>>> filters) throws IOException {
        ApiConnector apiConnector = getApiConnector();

        if (apiConnector != null) {
            return (List<VirtualMachineInterface>) apiConnector.list(VirtualMachineInterface.class, Arrays.asList("default-domain", tenantId));
        } else {
            throw new RuntimeException("can not get apiConnector~");
        }

    }

    private VirtualNetwork networkRead(String l2Id) throws IOException {
        return virtualNetworkRead(l2Id);
    }

    private VirtualNetwork virtualNetworkRead(String l2Id) throws IOException {
        ApiConnector apiConnector = getApiConnector();

        if (apiConnector != null) {
            return (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, l2Id);
        } else {
            throw new RuntimeException("can not get apiConnector~");
        }
    }

    public TfPortResponse deletePort(String portId, String accountId) {
        TfPortResponse response = new TfPortResponse();
        TfPortRequestBody portRequestBodyEO = new TfPortRequestBody();
        TfPortRequestData portRequestDataEO = new TfPortRequestData();
        TfPortRequestContext portRequestContextEO = new TfPortRequestContext();
        portRequestContextEO.setOperation("DELETE");
        portRequestContextEO.setIs_admin("True");
        portRequestContextEO.setTenant_id(accountId);
        portRequestDataEO.setId(portId);
        portRequestBodyEO.setData(portRequestDataEO);
        portRequestBodyEO.setContext(portRequestContextEO);
// begin
        ApiConnector apiConnector = getApiConnector();
        VirtualMachineInterface portObj = null;
        try {
            if (apiConnector != null) {
                portObj = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        List<ObjectReference<ApiPropertyBase>> routerBackRefs = portObj.getLogicalRouterBackRefs();
        if (routerBackRefs != null && routerBackRefs.size() > 0) {
            throw new RuntimeException("Port In Use~");
        }
        // release instance IP address
        List<ObjectReference<ApiPropertyBase>> iipBackRefs = portObj.getInstanceIpBackRefs();
        if (iipBackRefs != null && iipBackRefs.size() > 0) {
            for (ObjectReference<ApiPropertyBase> iipBackRef : iipBackRefs) {
                InstanceIp iipObj = null;
                try {
                    if (apiConnector != null) {
                        iipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, iipBackRef.getUuid());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // in case of shared ip only delete the link to the VMI
                if (iipObj != null) {
                    iipObj.removeVirtualMachineInterface(portObj);
                }
                List<ObjectReference<ApiPropertyBase>> virtualMachineInterface = iipObj != null ? iipObj.getVirtualMachineInterface() : null;
                if (virtualMachineInterface == null || virtualMachineInterface.size() == 0) {
                    try {
                        if (apiConnector != null) {
                            apiConnector.delete(InstanceIp.class, iipBackRef.getUuid());
                        }
                    } catch (Exception e) {
                        try {
                            apiConnector.update(iipObj);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } else {
                    try {
                        apiConnector.update(iipObj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        //  disassociate any floating IP used by instance

        List<ObjectReference<ApiPropertyBase>> fipBackRefs = portObj.getFloatingIpBackRefs();
        if (CollectionUtils.isNotEmpty(fipBackRefs)){
            for (ObjectReference<ApiPropertyBase> fipBackRef : fipBackRefs){
                try {
                    floatingipUpdate(fipBackRef.getUuid());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            if (apiConnector != null) {
                apiConnector.delete(VirtualMachineInterface.class, portId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // delete instance if this was the last port
        if (instanceId != null) {
            try {
                if (apiConnector != null) {
                    apiConnector.delete(VirtualMachine.class, instanceId);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        response.setCode(200);
        return response;
        // end
    }

    private void floatingipUpdate(String uuid) throws IOException {
        FloatingIp fipObj = floatingipNeutronToVnc(uuid);
        apiConnector.update(fipObj);
    }

    private FloatingIp floatingipNeutronToVnc(String uuid) throws IOException {
        FloatingIp fipObj = (FloatingIp) apiConnector.findById(FloatingIp.class, uuid);
        List<ObjectReference<ApiPropertyBase>> portRefs = fipObj.getVirtualMachineInterface();
        fipObj.clearVirtualMachineInterface();
        if (CollectionUtils.isEmpty(portRefs)){
            fipObj.setFixedIpAddress(null);
        } else {
            VirtualMachineInterface  portObj = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class, portRefs.get(0).getUuid());
            List<ObjectReference<ApiPropertyBase>> iipRefs = portObj.getInstanceIpBackRefs();
            if (CollectionUtils.isNotEmpty(iipRefs) && iipRefs.size() > 1){
                String msg = "Port "+ portObj.getUuid() + " has multiple fixed IP addresses.  Must provide a specific IP address when assigning a floating IP.";
                throw new RuntimeException(msg);
            }
            if (CollectionUtils.isNotEmpty(iipRefs)){
                InstanceIp iipObj = (InstanceIp) apiConnector.findById(InstanceIp.class, iipRefs.get(0).getUuid());
                checkPortFipAssoc(portObj, iipObj.getAddress(), fipObj);
                fipObj.setFixedIpAddress(iipObj.getAddress());
            }
        }
        return fipObj;
    }

    private void checkPortFipAssoc(VirtualMachineInterface portObj, String address, FloatingIp fipObj) throws IOException {
        // check if port already has floating ip associated
        List<ObjectReference<ApiPropertyBase>> fipRefs = portObj.getFloatingIpBackRefs();
        List<String> fipIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fipRefs)){
            for (ObjectReference<ApiPropertyBase> ref : fipRefs){
                if (!Objects.equals(fipObj.getUuid(), ref.getUuid())){
                    fipIds.add(ref.getUuid());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(fipIds)){
            for (String fipId : fipIds) {
                FloatingIp fipInstance = (FloatingIp) apiConnector.findById(FloatingIp.class, fipId);
                if (fipInstance.getFixedIpAddress().equals(address)){
                    throw new RuntimeException("FloatingIPPortAlreadyAssociated: " + fipInstance.getAddress() + " && " + portObj);
                }
            }
        }
    }
}
