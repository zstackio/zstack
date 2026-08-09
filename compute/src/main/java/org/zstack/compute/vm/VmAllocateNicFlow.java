package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.NetworkDependencyAdmissionExtensionPoint;
import org.zstack.header.network.NetworkDependencyAdmissionRequest;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.taskProgress;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateNicFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Autowired
    protected ResourceConfigFacade rcf;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        taskProgress("create nics");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicSpec> l3Networks = spec.getL3Networks();
        for (VmNicSpec l3Network : l3Networks) {
            for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                for (L3NetworkInventory l3Inv : l3Network.getL3Invs()) {
                    ext.vmPreAttachL3Network(spec.getVmInventory(), l3Inv);
                }
            }
        }
        final Map<String, NicIpAddressInfo> nicNetworkInfoMap =
                Optional.ofNullable(data.get(VmInstanceConstant.Params.VmAllocateNicFlow_nicNetworkInfo.toString()))
                .map(obj -> (Map<String, NicIpAddressInfo>) obj)
                .orElse(new StaticIpOperator().getNicNetworkInfoByVmUuid(spec.getVmInventory().getUuid()));

        final List<String> disableL3Networks = new ArrayList<>();
        if (spec.getDisableL3Networks() != null && !spec.getDisableL3Networks().isEmpty()) {
            disableL3Networks.addAll(spec.getDisableL3Networks());
        }

        // it's unlikely a vm having more than 512 nics
        final BitSet deviceIdBitmap = new BitSet(512);
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            deviceIdBitmap.set(nic.getDeviceId());
        }

        List<VmNicInventory> nics = new ArrayList<>();
        data.put(VmInstanceConstant.Params.VmAllocateNicFlow_nics.toString(), nics);
        List<ErrorCode> errs = new ArrayList<>();
        List<String> vmSystemTags = spec.getMessage() instanceof APIMessage ? ((APIMessage) spec.getMessage()).getSystemTags() : null;

        new While<>(VmNicSpec.getFirstL3NetworkInventoryOfSpec(spec.getL3Networks())).each((nicSpec, wcomp) -> {
            L3NetworkInventory nw = nicSpec.getL3Invs().get(0);
            int deviceId = deviceIdBitmap.nextClearBit(0);
            deviceIdBitmap.set(deviceId);
            MacOperator mo = new MacOperator();
            final String customMac = mo.getMac(spec.getVmInventory().getUuid(), nw.getUuid());
            final String mac = allocateMac(customMac, deviceId);
            CustomNicOperator nicOperator = new CustomNicOperator(spec.getVmInventory().getUuid(),nw.getUuid());
            final String customNicUuid = nicOperator.getCustomNicId();

            // choose vnic factory based on enableSRIOV system tag & enableVhostUser globalConfig
            VmNicType type = nicManager.getVmNicType(spec.getVmInventory().getUuid(), nw, vmSystemTags);
            if (type == null) {
                errs.add(Platform.operr(ORG_ZSTACK_COMPUTE_VM_10068, "there is no available nicType on L3 network [%s]", nw.getUuid()));
                wcomp.allDone();
                return;
            }
            VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(type);

            VmNicInventory nic = buildNicInventory(spec, nicSpec, nw, mac, customNicUuid, deviceId, disableL3Networks);
            if (mo.checkDuplicateMac(nic.getHypervisorType(), nic.getL3NetworkUuid(), nic.getMac())) {
                errs.add(operr(ORG_ZSTACK_COMPUTE_VM_10069, "Duplicate mac address [%s]", nic.getMac()));
                wcomp.allDone();
                return;
            }

            ErrorCode admissionError = admitDependency(nw.getUuid(),
                    NetworkDependencyAdmissionRequest.DEPENDENCY_VM_NIC, null,
                    NetworkDependencyAdmissionRequest.OPERATION_CREATE_VM_NIC);
            if (admissionError != null) {
                errs.add(admissionError);
                wcomp.allDone();
                return;
            }

            // Persist VmNicVO first so that ResourceVO entry exists before extensions
            // (e.g. SDN controllers) attempt to create SystemTags referencing the NIC UUID.
            VmNicVO nicVO = vnicFactory.createVmNic(nic, spec);

            callBeforeAllocateVmNicExtensions(nic, spec, new Completion(wcomp) {
                @Override
                public void success() {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            persistStaticIpIfNeeded(nic, nicVO, nw, nicNetworkInfoMap, spec);
                            nics.add(nic);
                            VmNicVO updated = dbf.updateAndRefresh(nicVO);
                            addVmNicConfig(updated, spec, nicSpec);
                        }
                    }.execute();
                    if (customMac != null) {
                        mo.deleteCustomMacSystemTag(spec.getVmInventory().getUuid(), nw.getUuid(), customMac);
                    }
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    try {
                        dbf.removeByPrimaryKey(nicVO.getUuid(), VmNicVO.class);
                    } catch (Throwable t) {
                        logger.warn(String.format("failed to remove VmNicVO[uuid:%s] after before allocate extension failure",
                                nicVO.getUuid()), t);
                    }
                    errs.add(errorCode);
                    wcomp.allDone();
                }
            });

        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() > 0) {
                    trigger.fail(errs.get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    private String allocateMac(String customMac, int deviceId) {
        if (customMac != null) {
            return customMac.toLowerCase();
        }
        return MacOperator.generateMacWithDeviceId((short) deviceId);
    }

    private VmNicInventory buildNicInventory(VmInstanceSpec spec, VmNicSpec nicSpec,
                                              L3NetworkInventory nw, String mac, String customNicUuid,
                                              int deviceId, List<String> disableL3Networks) {
        VmNicInventory nic = new VmNicInventory();
        nic.setUuid(customNicUuid != null ? customNicUuid : Platform.getUuid());
        /* the first ip is ipv4 address for dual stack nic */
        nic.setVmInstanceUuid(spec.getVmInventory().getUuid());
        nic.setL3NetworkUuid(nw.getUuid());
        nic.setMac(mac);
        nic.setHypervisorType(spec.getDestHost() == null ?
                spec.getVmInventory().getHypervisorType() : spec.getDestHost().getHypervisorType());

        if (!StringUtils.isEmpty(nicSpec.getNicDriverType())) {
            nic.setDriverType(nicSpec.getNicDriverType());
        } else {
            boolean vmImageHasVirtio = VmSystemTags.VIRTIO.hasTag(spec.getVmInventory().getUuid());
            nicManager.setNicDriverType(nic, vmImageHasVirtio,
                    ImagePlatform.valueOf(spec.getVmInventory().getPlatform()).isParaVirtualization(),
                    spec.getVmInventory());
        }

        nic.setDeviceId(deviceId);
        nic.setInternalName(VmNicVO.generateNicInternalName(spec.getVmInventory().getInternalId(), nic.getDeviceId()));
        nic.setState(disableL3Networks.contains(nic.getL3NetworkUuid()) ? VmNicState.disable.toString() : VmNicState.enable.toString());
        return nic;
    }

    private void persistStaticIpIfNeeded(VmNicInventory nic, VmNicVO nicVO,
                                          L3NetworkInventory nw, Map<String, NicIpAddressInfo> nicNetworkInfoMap,
                                          VmInstanceSpec spec) {
        if (nw.enableIpAddressAllocation() || nicNetworkInfoMap == null
                || !nicNetworkInfoMap.containsKey(nw.getUuid())
                || !spec.getVmInventory().getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
            return;
        }

        NicIpAddressInfo nicIpAddressInfo = nicNetworkInfoMap.get(nic.getL3NetworkUuid());
        if (!nicIpAddressInfo.ipv6Address.isEmpty()) {
            UsedIpVO vo = new UsedIpVO();
            vo.setUuid(Platform.getUuid());
            vo.setIp(IPv6NetworkUtils.getIpv6AddressCanonicalString(nicIpAddressInfo.ipv6Address));
            vo.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(nicIpAddressInfo.ipv6Address + "/" + nicIpAddressInfo.ipv6Prefix));
            vo.setGateway(nicIpAddressInfo.ipv6Gateway.isEmpty() ? "" : IPv6NetworkUtils.getIpv6AddressCanonicalString(nicIpAddressInfo.ipv6Gateway));
            vo.setIpVersion(IPv6Constants.IPv6);
            vo.setVmNicUuid(nic.getUuid());
            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
            vo.setIpRangeUuid(new StaticIpOperator().getIpRangeUuid(nic.getL3NetworkUuid(), vo.getIp()));
            nic.setUsedIpUuid(vo.getUuid());
            nicVO.setUsedIpUuid(vo.getUuid());
            nicVO.setIp(vo.getIp());
            nicVO.setNetmask(vo.getNetmask());
            nicVO.setGateway(vo.getGateway());
            dbf.persist(vo);
        }
        if (!nicIpAddressInfo.ipv4Address.isEmpty()) {
            UsedIpVO vo = new UsedIpVO();
            vo.setUuid(Platform.getUuid());
            vo.setIp(nicIpAddressInfo.ipv4Address);
            vo.setGateway(nicIpAddressInfo.ipv4Gateway);
            vo.setNetmask(nicIpAddressInfo.ipv4Netmask);
            vo.setIpVersion(IPv6Constants.IPv4);
            vo.setVmNicUuid(nic.getUuid());
            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(vo.getIp()));
            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
            vo.setIpRangeUuid(new StaticIpOperator().getIpRangeUuid(nic.getL3NetworkUuid(), vo.getIp()));
            nic.setUsedIpUuid(vo.getUuid());
            nicVO.setUsedIpUuid(vo.getUuid());
            nicVO.setIp(vo.getIp());
            nicVO.setNetmask(vo.getNetmask());
            nicVO.setGateway(vo.getGateway());
            dbf.persist(vo);
        }
    }

    private void addVmNicConfig(VmNicVO vmNicVO, VmInstanceSpec vmSpec, VmNicSpec nicSpec) {
        if (nicSpec == null) {
            return;
        }

        List<VmNicParam> vmNicParms = nicSpec.getVmNicParams();
        if (CollectionUtils.isEmpty(vmNicParms)) {
            return;
        }

        VmNicParam vmNicParm = vmNicParms.get(0);

        if (vmNicParm.getInboundBandwidth() != null || vmNicParm.getOutboundBandwidth() != null) {
            ErrorCode admissionError = admitDependency(vmNicVO.getL3NetworkUuid(),
                    NetworkDependencyAdmissionRequest.DEPENDENCY_VM_NIC_QOS, null,
                    NetworkDependencyAdmissionRequest.OPERATION_ADD_VM_NIC_QOS);
            if (admissionError != null) {
                throw new org.zstack.header.exception.CloudRuntimeException(admissionError.getDetails());
            }
        }

        // add vmnic bandwidth systemtag
        if (vmNicParm.getInboundBandwidth() != null || vmNicParm.getOutboundBandwidth() != null) {
            VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmSpec.getVmInventory().getType());
            backend.addNicQos(vmSpec.getVmInventory().getUuid(), vmNicVO.getUuid(), vmNicParm.getInboundBandwidth(), vmNicParm.getOutboundBandwidth());
        }

        //add vmnic multiqueue config
        if (vmNicParm.getMultiQueueNum() != null) {
            ResourceConfig multiQueues = rcf.getResourceConfig(VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM.getIdentity());
            Integer queues = vmNicParm.getMultiQueueNum();
            multiQueues.updateValue(vmNicVO.getUuid(), queues.toString());
        }
    }

    private ErrorCode admitDependency(String resourceUuid, String dependencyType, String operationUuid, String operationStep) {
        NetworkDependencyAdmissionRequest request = new NetworkDependencyAdmissionRequest(
                resourceUuid, dependencyType, operationUuid, operationStep);
        for (NetworkDependencyAdmissionExtensionPoint extension :
                pluginRgty.getExtensionList(NetworkDependencyAdmissionExtensionPoint.class)) {
            ErrorCode errorCode = extension.admit(request);
            if (errorCode != null) {
                return errorCode;
            }
        }
        return null;
    }

    private void callBeforeAllocateVmNicExtensions(VmNicInventory nic, VmInstanceSpec spec, Completion completion) {
        List<BeforeAllocateVmNicExtensionPoint> exts = pluginRgty.getExtensionList(BeforeAllocateVmNicExtensionPoint.class);
        if (exts.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(exts).each((ext, wcomp) -> {
            ext.beforeAllocateVmNic(nic, spec, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> destNics = spec.getDestNics();
        if (destNics == null || destNics.isEmpty()) {
            chain.rollback();
            return;
        }
        logger.debug(String.format("%s nic need for delete", destNics.size()));
        for (VmNicInventory vmNic : destNics) {
            for (VmDetachNicExtensionPoint ext : pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class)) {
                ext.afterDetachNic(vmNic);
            }

            VmNicType type = VmNicType.valueOf(vmNic.getType());
            VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(type);
            vnicFactory.releaseVmNic(vmNic);
        }
        dbf.removeByPrimaryKeys(destNics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()), VmNicVO.class);

        chain.rollback();
    }
}
