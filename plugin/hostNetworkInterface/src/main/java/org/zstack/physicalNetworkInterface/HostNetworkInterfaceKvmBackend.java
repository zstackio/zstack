package org.zstack.physicalNetworkInterface;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.tag.FormTagExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.physicalNetworkInterface.header.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class HostNetworkInterfaceKvmBackend implements  FormTagExtensionPoint {
    private static CLogger logger = Utils.getLogger(HostNetworkInterfaceKvmBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private AccountManager acntMgr;

    public static final String GET_HOST_VIRTUAL_NETWORK_INTERFACE = "/hostvirtualnetworkinterface/get";
    public static final String GENERATE_HOST_VIRTUAL_NETWORK_INTERFACE = "/hostvirtualnetworkinterface/generate";
    public static final String UNGENERATE_HOST_VIRTUAL_NETWORK_INTERFACE = "/hostvirtualnetworkinterface/ungenerate";

    public static class GetHostVirtualNetworkInterfacesCmd extends KVMAgentCommands.AgentCommand {
        public String filterString = "'\\w\\w:\\w\\w\\.\\w' -A1";
        public Boolean enableIommu = false;
        public Boolean skipGrubConfig = false;
    }

    public static class GetHostVirtualNetworkInterfacesRsp extends KVMAgentCommands.AgentResponse {
        public Boolean updateCache = false;
        public List<HostVirtualNetworkInterfaceTO> hostVirtualNetworkInterfaceInfo = new ArrayList<>();
        public Boolean hostIommuStatus;

        public List<HostVirtualNetworkInterfaceTO> getHostVirtualNetworkInterfaceInfo() {
            return hostVirtualNetworkInterfaceInfo;
        }

        public void setHostVirtualNetworkInterfaceInfo(List<HostVirtualNetworkInterfaceTO> hostVirtualNetworkInterfaceInfo) {
            this.hostVirtualNetworkInterfaceInfo = hostVirtualNetworkInterfaceInfo;
        }
    }

    public static class GenerateHostVirtualNetworkInterfaceCmd extends KVMAgentCommands.AgentCommand {
        public String pciDeviceType;
        public String pciDeviceAddress;
        public Integer virtPartNum;
        public Boolean reSplite;

        public String getPciDeviceType() {
            return pciDeviceType;
        }

        public void setPciDeviceType(String pciDeviceType) {
            this.pciDeviceType = pciDeviceType;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public Integer getVirtPartNum() {
            return virtPartNum;
        }

        public void setVirtPartNum(Integer virtPartNum) {
            this.virtPartNum = virtPartNum;
        }

        public Boolean getReSplite() {
            return reSplite;
        }

        public void setReSplite(Boolean reSplite) {
            this.reSplite = reSplite;
        }
    }

    public static class GenerateHostVirtualNetworkInterfaceRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class UngenerateHostVirtualNetworkInterfaceCmd extends KVMAgentCommands.AgentCommand {
        public String pciDeviceType;
        public String pciDeviceAddress;

        public String getPciDeviceType() {
            return pciDeviceType;
        }

        public void setPciDeviceType(String pciDeviceType) {
            this.pciDeviceType = pciDeviceType;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }
    }

    public static class UngenerateHostVirtualNetworkInterfaceRsp extends KVMAgentCommands.AgentResponse {

    }

    @Override
    public Map<String, Function<String, String>> getTagMappers(Class clz) {
        if (!AddKVMHostMsg.class.isAssignableFrom(clz)) {
            return new HashMap<>();
        }

        /* TODO
        return Collections.singletonMap("IOMMU", enabled -> {
            Boolean iommuEnabled = PageTableExtensionBackend.getTextBool(enabled, null);
            if (iommuEnabled == null) {
                throw new IllegalArgumentException(String.format("IOMMU invalid value [%s]", enabled));
            }
            PciDeviceState iommuState = iommuEnabled ? PciDeviceState.Enabled : PciDeviceState.Disabled;
            return PciDeviceSystemTags.HOST_IOMMU_STATE.instantiateTag(
                    Collections.singletonMap(PciDeviceSystemTags.HOST_IOMMU_STATE_TOKEN, iommuState)
            );
        }); */
        return new HashMap<>();
    }

    /*
    private void syncPciDeviceFromHost(final String hostUuid, String hostIommuState, boolean noStatusCheck, Completion completion) {
        GetHostVirtualNetworkInterfacesCmd cmd = new GetHostVirtualNetworkInterfacesCmd();
        Boolean enableIommu;
        if (StringUtils.isEmpty(hostIommuState)) {
            enableIommu = PciDeviceGlobalConfig.ENABLE_IOMMU.value(Boolean.class);
            logger.debug(String.format("pciDevice enableIommu config: %s", enableIommu));
        } else {
            if (HostIommuStateType.Enabled.toString().equals(hostIommuState)) {
                enableIommu = Boolean.TRUE;
            } else {
                enableIommu = Boolean.FALSE;
            }
        }

        cmd.enableIommu = enableIommu;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GET_HOST_VIRTUAL_NETWORK_INTERFACE);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    fail();
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                GetHostVirtualNetworkInterfacesRsp rsp = r.toResponse(GetHostVirtualNetworkInterfacesRsp.class);
                if (!rsp.isSuccess()) {
                    fail();
                    return;
                }

                List<HostVirtualNetworkInterfaceTO> tos = rsp.hostVirtualNetworkInterfaceInfo;
                if (tos == null || tos.isEmpty()) {
                    logger.debug("no pci device info received, which is unlikely");
                    completion.success();
                    return;
                }


                HostIommuStatusType hostIommuStatus = HostIommuStatusType.Inactive;
                // sync host iommu
                if (rsp.hostIommuStatus != null) {
                    hostIommuStatus = HostIommuStatusType.valueOf(rsp.hostIommuStatus);
                    syncHostIommuStatus(hostUuid, hostIommuStatus);
                }

                if (hostIommuStatus.equals(HostIommuStatusType.Inactive)) {
                    logger.debug("since host iommu status is inactive, skip sync pci device with db");
                    completion.success();
                    return;
                }

                // sync info of all host virtual network interface in host
                HostVirtualNetworkInterfaceSyncer syncer = new HostVirtualNetworkInterfaceSyncer(hostUuid, tos);
                syncer.syncPciWithDb(completion);
            }

            private void fail() {
                completion.fail(operr(String.format("get pci device info from host[uuid:%s] failed", hostUuid)));
            }
        });
    }

    private void syncHostIommuStatus(String hostUuid, HostIommuStatusType status) {
        if (PciDeviceSystemTags.HOST_IOMMU_STATUS.hasTag(hostUuid)) {
            PciDeviceSystemTags.HOST_IOMMU_STATUS.deleteInherentTag(hostUuid);
        }
        SystemTagCreator creator = PciDeviceSystemTags.HOST_IOMMU_STATUS.newSystemTagCreator(hostUuid);
        creator.ignoreIfExisting = false;
        creator.inherent = true;
        creator.setTagByTokens(
                map(
                        e(PciDeviceSystemTags.HOST_IOMMU_STATUS_TOKEN, status.toString())
                )
        );
        creator.create();
    } */

    private class HostVirtualNetworkInterfaceSyncer {
        final String hostUuid;
        final List<HostVirtualNetworkInterfaceTO> tos;
        final List<HostVirtualNetworkInterfaceVO> vos;

        final List<HostNetworkInterfaceVO> hostNetworkInterfaceVOS;

        public HostVirtualNetworkInterfaceSyncer(String hostUuid, List<HostVirtualNetworkInterfaceTO> tos) {
            this.hostUuid = hostUuid;
            this.tos = tos;
            this.tos.forEach(to -> to.setHostUuid(hostUuid));

            vos = Q.New(HostVirtualNetworkInterfaceVO.class).eq(HostVirtualNetworkInterfaceVO_.hostUuid, hostUuid).list();
            hostNetworkInterfaceVOS = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid).list();
        }

        void doSyncPciDevice() {
            List<HostVirtualNetworkInterfaceTO> dbTos = HostVirtualNetworkInterfaceTO.valueOf(vos);
            //set uuid if virtual network interface db exist
            tos.forEach(to -> dbTos.stream().filter(dt -> dt.equals(to)).findFirst().ifPresent(dto -> to.setUuid(dto.getUuid())));
            List<HostVirtualNetworkInterfaceTO> newTos = tos.stream().filter(to -> !dbTos.contains(to)).collect(Collectors.toList());
            //will update old virtual physical nic
            List<HostVirtualNetworkInterfaceTO> oldTos = tos.stream().filter(to -> to.getUuid() != null).collect(Collectors.toList());

            // 1. create new pci device
            List<HostVirtualNetworkInterfaceVO> newVOs = new ArrayList<>();
            for (HostVirtualNetworkInterfaceTO to : newTos) {
                HostVirtualNetworkInterfaceVO virtualNetworkInterfaceVO = new HostVirtualNetworkInterfaceVO();
                virtualNetworkInterfaceVO.setUuid(Platform.getUuid());
                virtualNetworkInterfaceVO.setHostUuid(hostUuid);
                virtualNetworkInterfaceVO.setDescription(to.getDescription());
                virtualNetworkInterfaceVO.setVendorId(to.getVendorId());
                virtualNetworkInterfaceVO.setDeviceId(to.getDeviceId());
                virtualNetworkInterfaceVO.setSubvendorId(to.getSubvendorId());
                virtualNetworkInterfaceVO.setSubdeviceId(to.getSubdeviceId());
                virtualNetworkInterfaceVO.setPciDeviceAddress(to.getPciDeviceAddress());
                virtualNetworkInterfaceVO.setStatus(HostVirtualNetworkInterfaceStatus.System);
                // we cannot collect info of mdev devices that are generated bypass zstack
                //virtualNetworkInterfaceVO.setVirtStatus(NicVirtStatus.valueOf(to.getVirtStatus()));
                virtualNetworkInterfaceVO.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                to.setUuid(virtualNetworkInterfaceVO.getUuid());

                newVOs.add(virtualNetworkInterfaceVO);
                logger.debug(String.format("create new virtual network interface[uuid:%s]", virtualNetworkInterfaceVO.getUuid()));
            }

            if (!newVOs.isEmpty()) {
                dbf.persistCollection(newVOs);
            }

            // 2. update existing pci devices
            List<HostVirtualNetworkInterfaceVO> oldVOs = new ArrayList<>();
            Map<String, HostVirtualNetworkInterfaceVO> vosMap = vos.stream().collect(Collectors.toMap(HostVirtualNetworkInterfaceVO::getUuid, vo -> vo));

            for (HostVirtualNetworkInterfaceTO to : oldTos) {
                HostVirtualNetworkInterfaceVO hostVirtualNic = vosMap.get(to.getUuid());
                if (hostVirtualNic == null) {
                    logger.warn(String.format("cannot update pci device[uuid:%s] because it doesn't exist", to.getUuid()));
                    continue;
                }

                // if need to update pci device
                if (hostVirtualNic.getVmInstanceUuid() != null && !hostVirtualNic.getVmInstanceUuid().isEmpty()) {
                    hostVirtualNic.setStatus(HostVirtualNetworkInterfaceStatus.Attached);
                    {
                        hostVirtualNic.setStatus(HostVirtualNetworkInterfaceStatus.System);
                    }

                    oldVOs.add(hostVirtualNic);
                    logger.debug(String.format("updated existing host virtual network interface[uuid:%s]", hostVirtualNic.getUuid()));
                }

                if (!oldVOs.isEmpty()) {
                    dbf.updateCollection(oldVOs);
                }

                // 3. delete garbage host virtual network interface
                List<HostVirtualNetworkInterfaceTO> garbageTos = new ArrayList<>(dbTos);
                garbageTos.removeAll(tos);
                List<String> garbageUuids = garbageTos.stream().map(HostVirtualNetworkInterfaceTO::getUuid).collect(Collectors.toList());
                if (!garbageUuids.isEmpty()) {
                    SQL.New(HostVirtualNetworkInterfaceVO.class).in(HostVirtualNetworkInterfaceVO_.uuid, garbageUuids).hardDelete();
                    logger.debug(String.format("delete garbage host virtual network interface[uuids:%s, desc:%s] in host[uuid:%s]",
                            garbageUuids,
                            garbageTos.stream().map(HostVirtualNetworkInterfaceTO::getDescription).collect(Collectors.toList()),
                            garbageTos.get(0).getHostUuid()));
                }

                // 4. TODO:shixin link virtual host network interface with physical pci devices
                List<HostVirtualNetworkInterfaceTO> vTos = new ArrayList<>(); /*newTos.stream()
                        .filter(t -> NicVirtStatus.SRIOV_VIRTUAL.toString().equals(t.getVirtStatus())
                                && StringUtils.isNotBlank(t.getParentAddress()))
                        .collect(Collectors.toList()); */

                List<HostVirtualNetworkInterfaceVO> virtualPcis = new ArrayList<>();
                Map<String, String> physicalNics = new HashMap<>();
                Map<String, HostVirtualNetworkInterfaceVO> newVOsMap = newVOs.stream().collect(Collectors.toMap(HostVirtualNetworkInterfaceVO::getUuid, vo -> vo));
                for (HostVirtualNetworkInterfaceTO vTo : vTos) {
                    String parentUuid;
                    HostVirtualNetworkInterfaceVO virtual = newVOsMap.get(vTo.getUuid());
                    if (physicalNics.containsKey(vTo.getParentAddress())) {
                        parentUuid = physicalNics.get(vTo.getParentAddress());
                    } else {
                        HostNetworkInterfaceVO hostNetworkInterfaceVO = hostNetworkInterfaceVOS.stream().filter(vo -> StringUtils.equals(vo.getPciDeviceAddress(), vTo.getParentAddress())).findFirst().orElse(null);
                        if (hostNetworkInterfaceVO == null) {
                            logger.debug(String.format("cannot find host network interface with virtual network interface[uuid:%s]", vTo.getUuid()));
                            continue;
                        }
                        parentUuid = hostNetworkInterfaceVO.getUuid();

                    }
                    physicalNics.put(vTo.getParentAddress(), parentUuid);


                    if (StringUtils.equals(parentUuid, virtual.getHostNetworkInterfaceUuid())) {
                        continue;
                    }

                    virtual.setHostNetworkInterfaceUuid(parentUuid);
                    virtualPcis.add(virtual);
                    logger.debug(String.format("update host network interface uuid[uuid:%s] to [%s]", vTo.getUuid(), parentUuid));
                }

                if (!virtualPcis.isEmpty()) {
                    dbf.updateCollection(virtualPcis);
                }

                // 6. after sync pci device
                vos.removeIf(vo -> garbageUuids.contains(vo.getUuid()));
            }

        }

        void syncPciWithDb(Completion completion) {
            if (CollectionUtils.isEmpty(tos)) {
                completion.success();
                return;
            }


            doSyncPciDevice();
            completion.success();
        }
    }
}
