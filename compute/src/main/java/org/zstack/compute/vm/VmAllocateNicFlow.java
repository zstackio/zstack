package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkInfo;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.taskProgress;

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

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        taskProgress("create nics");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final Map<String, NetworkInfo> nicNetworkInfoMap =
                Optional.ofNullable(data.get(VmInstanceConstant.Params.VmAllocateNicFlow_nicNetworkInfo.toString()))
                .map(obj -> (Map<String, NetworkInfo>) obj)
                .orElse(new StaticIpOperator().getNicNetworkInfoByVmUuid(spec.getVmInventory().getUuid()));

        final List<String> disableL3Networks = new ArrayList<>();
        if (spec.getDisableL3Networks() != null && !spec.getDisableL3Networks().isEmpty()) {
            disableL3Networks.addAll(spec.getDisableL3Networks());
        }

        Boolean allowDuplicatedAddress = (Boolean)data.get(VmInstanceConstant.Params.VmAllocateNicFlow_allowDuplicatedAddress.toString());

        // it's unlikely a vm having more than 512 nics
        final BitSet deviceIdBitmap = new BitSet(512);
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            deviceIdBitmap.set(nic.getDeviceId());
        }

        List<VmNicInventory> nics = new ArrayList<>();
        data.put(VmInstanceConstant.Params.VmAllocateNicFlow_nics.toString(), nics);
        List<ErrorCode> errs = new ArrayList<>();

        new While<>(VmNicSpec.getFirstL3NetworkInventoryOfSpec(spec.getL3Networks())).each((nicSpec, wcomp) -> {
            L3NetworkInventory nw = nicSpec.getL3Invs().get(0);
            int deviceId = deviceIdBitmap.nextClearBit(0);
            deviceIdBitmap.set(deviceId);
            MacOperator mo = new MacOperator();
            String customMac = mo.getMac(spec.getVmInventory().getUuid(), nw.getUuid());
            if (customMac != null){
                mo.deleteCustomMacSystemTag(spec.getVmInventory().getUuid(), nw.getUuid(), customMac);
                customMac = customMac.toLowerCase();
            } else {
                customMac = NetworkUtils.generateMacWithDeviceId((short) deviceId);
            }
            final String mac = customMac;

            // choose vnic factory based on enableSRIOV system tag & enableVhostUser globalConfig
            VmInstanceNicFactory vnicFactory;
            boolean enableSriov = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                    .eq(SystemTagVO_.resourceUuid, spec.getVmInventory().getUuid())
                    .eq(SystemTagVO_.tag, String.format("enableSRIOV::%s", nw.getUuid()))
                    .isExists();
            logger.debug(String.format("create %s on l3 network[uuid:%s] inside VmAllocateNicFlow",
                    enableSriov ? "vf nic" : "vnic", nw.getUuid()));
            boolean enableVhostUser = NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class);

            L2NetworkVO l2nw =  dbf.findByUuid(nw.getL2NetworkUuid(), L2NetworkVO.class);
            VSwitchType vSwitchType = VSwitchType.valueOf(l2nw.getvSwitchType());
            VmNicType type = vSwitchType.getVmNicTypeWithCondition(enableSriov, enableVhostUser);
            if (type == null) {
                throw new OperationFailureException(Platform.operr("there is no available nicType on L2 network [%s]", l2nw.getUuid()));
            }
            vnicFactory = vmMgr.getVmInstanceNicFactory(type);


            VmNicInventory nic = new VmNicInventory();
            nic.setUuid(Platform.getUuid());
            /* the first ip is ipv4 address for dual stack nic */
            nic.setVmInstanceUuid(spec.getVmInventory().getUuid());
            nic.setL3NetworkUuid(nw.getUuid());
            nic.setMac(mac);
            nic.setHypervisorType(spec.getDestHost() == null ?
                    spec.getVmInventory().getHypervisorType() : spec.getDestHost().getHypervisorType());
            if (mo.checkDuplicateMac(nic.getHypervisorType(), nic.getMac())) {
                trigger.fail(operr("Duplicate mac address [%s]", nic.getMac()));
                return;
            }

            if (nicSpec.getNicDriverType() != null) {
                nic.setDriverType(nicSpec.getNicDriverType());
            } else {
                boolean imageHasVirtio = false;
                try {
                    imageHasVirtio = spec.getImageSpec().getInventory().getVirtio();
                } catch (Exception e) {
                    logger.debug(String.format("there is no image spec for vm %s", spec.getVmInventory().getUuid()));
                }

                nicManager.setNicDriverType(nic, imageHasVirtio,
                        ImagePlatform.valueOf(spec.getVmInventory().getPlatform()).isParaVirtualization());
            }

            nic.setDeviceId(deviceId);
            nic.setInternalName(VmNicVO.generateNicInternalName(spec.getVmInventory().getInternalId(), nic.getDeviceId()));
            nic.setState(disableL3Networks.contains(nic.getL3NetworkUuid()) ? VmNicState.disable.toString() : VmNicState.enable.toString());
            new SQLBatch() {
                @Override
                protected void scripts() {
                    vnicFactory.createVmNic(nic, spec);
                    if (nicNetworkInfoMap != null && nicNetworkInfoMap.containsKey(nic.getL3NetworkUuid())) {
                        NetworkInfo nicNetworkInfo = nicNetworkInfoMap.get(nic.getL3NetworkUuid());
                        if (!nicNetworkInfo.ipv4Address.isEmpty()) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIp(nicNetworkInfo.ipv4Address);
                            vo.setGateway(nicNetworkInfo.ipv4Gateway);
                            vo.setNetmask(nicNetworkInfo.ipv4Netmask);
                            vo.setIpVersion(IPv6Constants.IPv4);
                            vo.setVmNicUuid(nic.getUuid());
                            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
                            if (nic.getUsedIpUuid() == null) {
                                nic.setUsedIpUuid(vo.getUuid());
                            }
                            dbf.persist(vo);
                        }
                        if (!nicNetworkInfo.ipv6Address.isEmpty()) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIp(IPv6NetworkUtils.getIpv6AddressCanonicalString(nicNetworkInfo.ipv6Address));
                            vo.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(nicNetworkInfo.ipv6Address+"/"+nicNetworkInfo.ipv6Prefix));
                            vo.setGateway(nicNetworkInfo.ipv6Gateway.isEmpty() ? "" : IPv6NetworkUtils.getIpv6AddressCanonicalString(nicNetworkInfo.ipv6Gateway));
                            vo.setIpVersion(IPv6Constants.IPv6);
                            vo.setVmNicUuid(nic.getUuid());
                            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
                            if (nic.getUsedIpUuid() == null) {
                                nic.setUsedIpUuid(vo.getUuid());
                            }
                            dbf.persist(vo);
                        }
                    }
                    nics.add(nic);
                }
            }.execute();
            wcomp.done();

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

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> destNics = spec.getDestNics();
        if (destNics == null || destNics.isEmpty()) {
            chain.rollback();
            return;
        }
        logger.debug(String.format("%s nic need for delete", destNics.size()));
        dbf.removeByPrimaryKeys(destNics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()), VmNicVO.class);
        chain.rollback();
        return;
    }
}
