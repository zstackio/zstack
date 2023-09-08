package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vm.cdrom.*;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO_;
import org.zstack.header.volume.*;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.getDuplicateElementsOfList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class VmInstanceApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmInstanceApiInterceptor.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    private PluginRegistry pluginRgty;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmInstanceMessage) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDestroyVmInstanceMsg) {
            validate((APIDestroyVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeMsg) {
            validate((APICreateVmInstanceFromVolumeMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            validate((APIGetVmAttachableDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            validate((APIDetachL3NetworkFromVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicStateMsg) {
            validate((APIChangeVmNicStateMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicNetworkMsg) {
            validate((APIChangeVmNicNetworkMsg) msg);
        } else if (msg instanceof APIGetCandidateL3NetworksForChangeVmNicNetworkMsg)
            validate((APIGetCandidateL3NetworksForChangeVmNicNetworkMsg) msg);
        else if (msg instanceof APIAttachVmNicToVmMsg) {
            validate((APIAttachVmNicToVmMsg) msg);
        } else if (msg instanceof APICreateVmNicMsg) {
            validate((APICreateVmNicMsg) msg);
        } else if (msg instanceof APIAttachIsoToVmInstanceMsg) {
            validate((APIAttachIsoToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachIsoFromVmInstanceMsg) {
            validate((APIDetachIsoFromVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmBootOrderMsg) {
            validate((APISetVmBootOrderMsg) msg);
        } else if (msg instanceof APISetVmBootVolumeMsg) {
            validate((APISetVmBootVolumeMsg) msg);
        } else if (msg instanceof APIDeleteVmStaticIpMsg) {
            validate((APIDeleteVmStaticIpMsg) msg);
        } else if (msg instanceof APISetVmStaticIpMsg) {
            validate((APISetVmStaticIpMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            validate((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksImagesMsg) {
            validate((APIGetInterdependentL3NetworksImagesMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksBackupStoragesMsg) {
            validate((APIGetInterdependentL3NetworksBackupStoragesMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMsg) {
            validate((APIUpdateVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmConsolePasswordMsg) {
            validate((APISetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIChangeInstanceOfferingMsg) {
            validate((APIChangeInstanceOfferingMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIGetCandidatePrimaryStoragesForCreatingVmMsg) {
            validate((APIGetCandidatePrimaryStoragesForCreatingVmMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmNicMsg) {
            validate((APIAttachL3NetworkToVmNicMsg) msg);
        } else if (msg instanceof APIDeleteVmCdRomMsg) {
            validate((APIDeleteVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmCdRomMsg) {
            validate((APIUpdateVmCdRomMsg) msg);
        } else if (msg instanceof APISetVmInstanceDefaultCdRomMsg) {
            validate((APISetVmInstanceDefaultCdRomMsg) msg);
        } else if (msg instanceof APICreateVmCdRomMsg) {
            validate((APICreateVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmNicDriverMsg) {
            validate((APIUpdateVmNicDriverMsg) msg);
        } else if (msg instanceof APIGetCandidateZonesClustersHostsForCreatingVmMsg) {
            validate((APIGetCandidateZonesClustersHostsForCreatingVmMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIGetInterdependentL3NetworksBackupStoragesMsg msg) {
        if (msg.getL3NetworkUuids() == null && msg.getBackupStorageUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("either l3NetworkUuids or backupStorageUuid must be set"));
        }
    }

    private void validate(APIGetCandidateL3NetworksForChangeVmNicNetworkMsg msg) {
        String vmUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).select(VmNicVO_.vmInstanceUuid).findValue();
        msg.setVmInstanceUuid(vmUuid);
    }

    private void validate(APIChangeVmNicNetworkMsg msg) {
        List<Map<String, String>> networkServices = new ArrayList<>();
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();

        //l2 must use same vswitch
        long count  = SQL.New("select count(distinct l2.vSwitchType) from L2NetworkVO l2, L3NetworkVO l3 where l2.uuid = l3.l2NetworkUuid" +
                " and l3.uuid in :l3Uuids")
                .param("l3Uuids", Arrays.asList(nicVO.getL3NetworkUuid(), msg.getDestL3NetworkUuid()))
                .find();
        if (count > 1) {
            throw new ApiMessageInterceptionException(operr("could not change to L3 network, the l2 of l3[uuid:%s, %s] use different vswitch",
                    nicVO.getL3NetworkUuid(), msg.getDestL3NetworkUuid()));
        }
        for (VmNicChangeNetworkExtensionPoint extension : pluginRgty.getExtensionList(VmNicChangeNetworkExtensionPoint.class)) {
            Map<String, String> ret = extension.getVmNicAttachedNetworkService(VmNicInventory.valueOf(nicVO));
            if (ret == null) {
                continue;
            }
            networkServices.add(ret);
        }

        if (!networkServices.isEmpty()) {
            String error = "vm nic [%s] attached network services, please detach manually/n" + networkServices.toString();
            throw new ApiMessageInterceptionException(operr(error, msg.getVmNicUuid()));
        }

        String sql = "select vm.uuid, vm.state, vm.type, nic.l3NetworkUuid from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getVmNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);
        String type = t.get(2, String.class);
        String srcL3Uuid = t.get(3, String.class);
        msg.setVmInstanceUuid(vmUuid);

        if (!VmInstanceState.Stopped.equals(state) && !VmInstanceState.Running.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getDestL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.getEnableIPAM() && l3NetworkVO.getIpRanges().isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getDestL3NetworkUuid()));
        }

        List<String> newAddedL3Uuids = new ArrayList<>(Collections.singletonList(msg.getDestL3NetworkUuid()));

        /* all l3 must be on same l2 */
        List<String> l2Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid, newAddedL3Uuids).select(L3NetworkVO_.l2NetworkUuid).listValues();
        l2Uuids = l2Uuids.stream().distinct().collect(Collectors.toList());
        if(l2Uuids.size() > 1) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] are belonged to different l2 networks [uuids:%s]",
                    newAddedL3Uuids, l2Uuids));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuids.get(0))
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] are belonged to l2 networks [uuids:%s] that have not been attached to any cluster",
                    newAddedL3Uuids, l2Uuids));
        }

        sql = "select ip.l3NetworkUuid from UsedIpVO ip, VmNicVO nic where ip.vmNicUuid = nic.uuid and nic.vmInstanceUuid = :vmUuid and ip.l3NetworkUuid in (:l3Uuids)";
        List<String> attachedL3Uuids = SQL.New(sql, String.class)
                .param("vmUuid", msg.getVmInstanceUuid())
                .param("l3Uuids", newAddedL3Uuids)
                .list();
        if (attachedL3Uuids != null && !attachedL3Uuids.isEmpty()) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }

            List<String> attachedNonGuestL3Uuids = Q.New(L3NetworkVO.class).select(L3NetworkVO_.uuid).
                    notEq(L3NetworkVO_.category, L3NetworkCategory.Private).in(L3NetworkVO_.uuid, attachedL3Uuids).listValues();
            if (attachedNonGuestL3Uuids != null && !attachedNonGuestL3Uuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("unable to change to a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }
        }

        for (String l3Uuid : newAddedL3Uuids) {
            L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3Vo.getState() == L3NetworkState.Disabled) {
                throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is disabled", l3Uuid));
            }
            if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3Vo.isSystem()) {
                throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                        l3Uuid));
            }
        }

        Map<String, List<String>> staticIps = new StaticIpOperator().getStaticIpbySystemTag(msg.getSystemTags());
        if (msg.getRequiredIpMap() != null) {
            staticIps.computeIfAbsent(msg.getDestL3NetworkUuid(), k -> new ArrayList<>()).add(msg.getStaticIp());
            SimpleQuery<NormalIpRangeVO> iprq = dbf.createQuery(NormalIpRangeVO.class);
            iprq.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, msg.getDestL3NetworkUuid());
            List<NormalIpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (NormalIpRangeVO ipr : iprs) {
                if (!ipr.getIpVersion().equals(NetworkUtils.getIpversion(msg.getStaticIp()))) {
                    continue;
                }

                if (NetworkUtils.isInRange(msg.getStaticIp(), ipr.getStartIp(), ipr.getEndIp())) {
                    found = true;
                    break;
                }
            }

            if (!l3NetworkVO.getEnableIPAM()) {
                found = true;
            }

            if (!found) {
                throw new ApiMessageInterceptionException(argerr("the static IP[%s] is not in any IP range of the L3 network[uuid:%s]", msg.getStaticIp(), msg.getDestL3NetworkUuid()));
            }

            SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
            uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getDestL3NetworkUuid());
            uq.add(UsedIpVO_.ip, Op.EQ, msg.getStaticIp());
            if (uq.isExists()) {
                throw new ApiMessageInterceptionException(operr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", msg.getStaticIp(), msg.getDestL3NetworkUuid()));
            }
        }

        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            if (!newAddedL3Uuids.contains(e.getKey())) {
                throw new ApiMessageInterceptionException(argerr("static ip l3 uuid[%s] is not included in nic l3 [%s]", e.getKey(), newAddedL3Uuids));
            }

            String l3Uuid = e.getKey();
            List<String> ips = e.getValue();
            SimpleQuery<NormalIpRangeVO> iprq = dbf.createQuery(NormalIpRangeVO.class);
            iprq.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, l3Uuid);
            List<NormalIpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (String staticIp : ips) {
                int ipVersion = IPv6Constants.IPv4;
                if (IPv6NetworkUtils.isIpv6Address(staticIp)) {
                    ipVersion = IPv6Constants.IPv6;
                }
                for (NormalIpRangeVO ipr : iprs) {
                    if (ipVersion != ipr.getIpVersion()) {
                        continue;
                    }
                    if (NetworkUtils.isInRange(staticIp, ipr.getStartIp(), ipr.getEndIp())) {
                        found = true;
                        break;
                    }
                }

                if (!l3NetworkVO.getEnableIPAM()) {
                    found = true;
                }

                if (!found) {
                    throw new ApiMessageInterceptionException(argerr("the static IP[%s] is not in any IP range of the L3 network[uuid:%s]", staticIp, l3Uuid));
                }

                SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
                uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getDestL3NetworkUuid());
                uq.add(UsedIpVO_.ip, Op.EQ, msg.getStaticIp());
                if (uq.isExists()) {
                    throw new ApiMessageInterceptionException(operr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", staticIp, l3Uuid));
                }
            }
        }

        msg.setRequiredIpMap(new HashMap<>());

        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            msg.getRequiredIpMap().put(e.getKey(), e.getValue());
        }

        final Map<String, NicIpAddressInfo> nicNetworkInfo = new StaticIpOperator().getNicNetworkInfoBySystemTag(msg.getSystemTags());
        NicIpAddressInfo nicIpAddressInfo = nicNetworkInfo.get(msg.getDestL3NetworkUuid());
        if (nicIpAddressInfo != null) {
            if (!nicIpAddressInfo.ipv4Address.isEmpty() && Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, nicIpAddressInfo.ipv4Address).eq(UsedIpVO_.l3NetworkUuid, msg.getDestL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", nicIpAddressInfo.ipv4Address, msg.getDestL3NetworkUuid()));
            }
            if (!nicIpAddressInfo.ipv6Address.isEmpty() && Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, IPv6NetworkUtils.getIpv6AddressCanonicalString(nicIpAddressInfo.ipv6Address)).eq(UsedIpVO_.l3NetworkUuid, msg.getDestL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", nicIpAddressInfo.ipv6Address, msg.getDestL3NetworkUuid()));
            }
        }
    }

    private void validate(APIGetCandidateZonesClustersHostsForCreatingVmMsg msg) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();

        if (instanceOfferingUuid == null) {
            if (msg.getCpuNum() == null || msg.getMemorySize() == null) {
                throw new ApiMessageInterceptionException(operr("Missing CPU/memory settings"));
            }
        }

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image != null && image.getMediaType() == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new OperationFailureException(argerr("the image[name:%s, uuid:%s] is an ISO, rootDiskSize must be set",
                            image.getName(), image.getUuid()));
                }
            }
        }
    }

    private void validate(final APICreateVmCdRomMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (!vo.getState().equals(VmInstanceState.Stopped)) {
            throw new ApiMessageInterceptionException(argerr(
                    "Can not create CD-ROM for vm[uuid:%s] which is in state[%s] ", msg.getVmInstanceUuid(), vo.getState().toString()));
        }
    }

    private void validate(final APIUpdateVmNicDriverMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vo.getPlatform().equals(ImagePlatform.Other.toString())) {
            throw new ApiMessageInterceptionException(argerr(
                    "Current platform %s not support update nic driver yet", vo.getPlatform()));
        }

        List<String> supportNicDriverTypes = nicManager.getSupportNicDriverTypes();
        if (!supportNicDriverTypes.contains(msg.getDriverType())) {
            throw new ApiMessageInterceptionException(argerr(
                    "Nic driver %s not support yet", msg.getDriverType()));
        }
    }

    private void validate(final APIGetCandidatePrimaryStoragesForCreatingVmMsg msg) {
        ImageMediaType mediaType = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).select(ImageVO_.mediaType).findValue();
        if (ImageMediaType.ISO == mediaType) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(argerr("rootDiskSize is needed when image media type is ISO"));
                }
            }
        }
    }

    private void validate(APIMigrateVmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                if (vo.getState().equals(VmInstanceState.Running) && vo.getHostUuid().equals(msg.getHostUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the vm[uuid:%s] is already on host[uuid:%s]", msg.getVmInstanceUuid(), msg.getHostUuid()
                    ));
                }

                if (vo.getState() == VmInstanceState.Paused && VmSystemTags.VM_STATE_PAUSED_AFTER_MIGRATE.hasTag(msg.getVmInstanceUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the vm[uuid:%s] is still paused after the last migration, please resume it before migrate.", msg.getVmInstanceUuid()));
                }
            }
        }.execute();
    }

    private void validate(APIChangeInstanceOfferingMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                InstanceOfferingVO instanceOfferingVO = Q.New(InstanceOfferingVO.class).eq(InstanceOfferingVO_.uuid, msg.getInstanceOfferingUuid()).find();

                boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getVmInstanceUuid(), Boolean.class);
                if (!numa && !VmInstanceState.Stopped.equals(vo.getState())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the VM cannot do online cpu/memory update because of disabling Instance Offering Online Modification. Please stop the VM then do the cpu/memory update again"
                    ));
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr("The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }

                if (VmInstanceState.Stopped.equals(vo.getState())) {
                    return;
                }

                if (instanceOfferingVO.getCpuNum() < vo.getCpuNum() || instanceOfferingVO.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease capacity when vm[uuid:%s] is running", vo.getUuid()
                    ));
                }
            }
        }.execute();
    }


    private void validate(APIUpdateVmInstanceMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                Integer cpuSum = msg.getCpuNum();
                Long memorySize = msg.getMemorySize();
                if ((cpuSum == null && memorySize == null)) {
                    return;
                }

                VmInstanceState vmState = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();
                boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getUuid(), Boolean.class);
                if (!numa && !VmInstanceState.Stopped.equals(vmState)) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the VM cannot do online cpu/memory update because of disabling Instance Offering Online Modification. Please stop the VM then do the cpu/memory update again"
                    ));
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr("The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }


                if (VmInstanceState.Stopped.equals(vmState)) {
                    return;
                }

                if (msg.getCpuNum() != null && msg.getCpuNum() < vo.getCpuNum()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease cpu of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }

                if (msg.getMemorySize() != null && msg.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease memory size of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }
            }
        }.execute();
    }


    private void validate(APIGetInterdependentL3NetworksImagesMsg msg) {
        if (msg.getL3NetworkUuids() == null && msg.getImageUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "either l3NetworkUuids or imageUuid must be set"
            ));
        }
    }

    private void validate(APIStartVmInstanceMsg msg) {
        // host uuid overrides cluster uuid
        if (msg.getHostUuid() != null) {
            msg.setClusterUuid(null);
        }
    }

    private void validateStaticIPv4(VmNicVO vmNicVO, L3NetworkVO l3NetworkVO, String ip) {
        if (!NetworkUtils.isIpv4Address(ip)) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid IPv4 address", ip));
        }

        for (UsedIpVO ipVo : vmNicVO.getUsedIps()) {
            if (ipVo.getIpVersion() != IPv6Constants.IPv4) {
                continue;
            }

            if (ipVo.getL3NetworkUuid().equals(l3NetworkVO.getUuid())) {
                if (ipVo.getIp().equals(ip)) {
                    throw new ApiMessageInterceptionException(argerr("ip address [%s] already set to vmNic [uuid:%s]",
                            ip, vmNicVO.getUuid()));
                }
                if (!l3NetworkVO.getEnableIPAM()) {
                    continue;
                }
                // check if the ip is in the ip range when ipam is enabled
                NormalIpRangeVO rangeVO = dbf.findByUuid(ipVo.getIpRangeUuid(), NormalIpRangeVO.class);
                if (!NetworkUtils.isIpv4InCidr(ip, rangeVO.getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(argerr("ip address [%s] is not in ip range [%s]",
                            ip, rangeVO.getNetworkCidr()));
                }
            }
        }
    }

    private void validateStaticIPv6(VmNicVO vmNicVO, L3NetworkVO l3NetworkVO, String ip) {
        if (!IPv6NetworkUtils.isIpv6Address(ip)) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid IPv6 address", ip));
        }

        for (UsedIpVO ipVo : vmNicVO.getUsedIps()) {
            if (ipVo.getIpVersion() != IPv6Constants.IPv6) {
                continue;
            }

            if (ipVo.getL3NetworkUuid().equals(l3NetworkVO.getUuid())) {
                if (ip.equals(ipVo.getIp())) {
                    throw new ApiMessageInterceptionException(argerr("ip address [%s] already set to vmNic [uuid:%s]",
                            ip, vmNicVO.getUuid()));
                }
                if (!l3NetworkVO.getEnableIPAM()) {
                    continue;
                }
                NormalIpRangeVO rangeVO = dbf.findByUuid(ipVo.getIpRangeUuid(), NormalIpRangeVO.class);
                if (!IPv6NetworkUtils.isIpv6InRange(ip, rangeVO.getStartIp(), rangeVO.getEndIp())) {
                    throw new ApiMessageInterceptionException(argerr("ip address [%s] is not in ip range [startIp %s, endIp %s]",
                            ip, rangeVO.getStartIp(), rangeVO.getEndIp()));
                }
            }
        }
    }

    private void validate(APISetVmStaticIpMsg msg) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if (msg.getIp() == null && msg.getIp6() == null) {
            if(l3NetworkVO.getEnableIPAM()) {
                throw new ApiMessageInterceptionException(argerr("could not set ip address, due to no ip address is specified"));
            }
        }
        List<VmNicVO> vmNics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid()).list();
        boolean l3Found = false;
        for (VmNicVO nic : vmNics) {
            l3Found = true;
            if (msg.getIp() != null) {
                String ip = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getIp());
                if (NetworkUtils.isIpv4Address(ip)) {
                    validateStaticIPv4(nic, l3NetworkVO, ip);
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    validateStaticIPv6(nic, l3NetworkVO, ip);
                    msg.setIp(ip);
                } else {
                    throw new ApiMessageInterceptionException(argerr("static ip [%s] format error", msg.getIp()));
                }
            }
            if (msg.getIp6() != null) {
                String ip6 = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getIp6());
                validateStaticIPv6(nic, l3NetworkVO, ip6);
                msg.setIp6(ip6);
            }
        }
        if (msg.getIp() != null && !l3NetworkVO.getEnableIPAM()) {
            l3Found = true;
            if (msg.getNetmask() == null) {
                throw new ApiMessageInterceptionException(argerr("ipv4 address need a netmask"));
            }
            if (msg.getGateway() == null) {
                msg.setGateway("");
            }
            if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getIp()).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("ip address [%s] already set to vmNic", msg.getIp()));
            }
        }
        if (msg.getIp6() != null && !l3NetworkVO.getEnableIPAM()) {
            l3Found = true;
            if (msg.getIpv6Prefix() == null) {
                throw new ApiMessageInterceptionException(argerr("ipv6 address need a prefix"));
            }
            if (msg.getIpv6Gateway() == null) {
                msg.setIpv6Gateway("");
            }
            if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getIp6()).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("ip address [%s] already set to vmNic", msg.getIp6()));
            }
        }
        if (!l3Found) {
            throw new ApiMessageInterceptionException(argerr("the VM[uuid:%s] has no nic on the L3 network[uuid:%s]", msg.getVmInstanceUuid(),
                            msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIDeleteVmStaticIpMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("the VM[uuid:%s] has no nic on the L3 network[uuid:%s]", msg.getVmInstanceUuid(),
                            msg.getL3NetworkUuid()));
        }

        if (msg.getStaticIp() != null) {
            if (!Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(UsedIpVO_.ip, msg.getStaticIp()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could not delete static ip [%s] for vm [uuid:%s] " +
                                "because it doesn't existed", msg.getStaticIp(), msg.getVmInstanceUuid()));
            }
        }
    }

    private void validate(APISetVmBootOrderMsg msg) {
        if (msg.getBootOrder() != null) {
            for (String o : msg.getBootOrder()) {
                try {
                    VmBootDevice.valueOf(o);
                } catch (IllegalArgumentException e) {
                    throw new ApiMessageInterceptionException(argerr("invalid boot device[%s] in boot order%s", o, msg.getBootOrder()));
                }
            }
        }
    }

    private boolean isVmHasMemorySnapshotGroup(String vmUuid) {
        List<String> snapShotGroupUuids = Q.New(VmInstanceDeviceAddressGroupVO.class)
                .select(VmInstanceDeviceAddressGroupVO_.resourceUuid)
                .eq(VmInstanceDeviceAddressGroupVO_.vmInstanceUuid, vmUuid)
                .listValues();
        if (snapShotGroupUuids.isEmpty()) {
            return false;
        }
        return Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid)
                .in(VolumeSnapshotGroupVO_.uuid, snapShotGroupUuids)
                .isExists();
    }

    private void validate(APISetVmBootVolumeMsg msg) {
        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (volume.isShareable()) {
            throw new ApiMessageInterceptionException(argerr("boot volume cannot be shareable."));
        }

        if (!msg.getVmInstanceUuid().equals(volume.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] must be attached to vm[uuid:%s]",
                    msg.getVolumeUuid(), msg.getVmInstanceUuid()));
        }

        if (isVmHasMemorySnapshotGroup(msg.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr("the vm %s with memory snapshots do not support setting boot volume", msg.getVmInstanceUuid()));
        }
    }


    private void validate(APIAttachIsoToVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if (isoUuids.contains(msg.getIsoUuid())) {
            throw new ApiMessageInterceptionException(operr("VM[uuid:%s] already has an ISO[uuid:%s] attached", msg.getVmInstanceUuid(), msg.getIsoUuid()));
        }

        ImageConstant.ImageMediaType type = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getIsoUuid()).select(ImageVO_.mediaType).findValue();
        if (type != ImageConstant.ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(argerr("Unsupported Image Media Type: [%s] ", type));
        }

        validateCdRomUuid(msg);
    }

    private void validateCdRomUuid(APIAttachIsoToVmInstanceMsg msg) {
        if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String cdRomUuid = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.CD_ROM, VmSystemTags.CD_ROM_UUID_TOKEN);
        if (cdRomUuid != null) {
            VmCdRomVO cdRomVO = dbf.findByUuid(cdRomUuid, VmCdRomVO.class);
            if (cdRomVO == null) {
                throw new ApiMessageInterceptionException(operr("The cdRom[uuid:%s] does not exist", cdRomUuid));
            }

            if (StringUtils.isNotEmpty(cdRomVO.getIsoUuid())){
                throw new ApiMessageInterceptionException(operr("VM[uuid:%s] cdRom[uuid:%s] has mounted the ISO", msg.getVmInstanceUuid(), cdRomUuid));
            }

            msg.setCdRomUuid(cdRomUuid);
        }
    }

    private void fillIsoUuid(APIDetachIsoFromVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if(isoUuids.size() == 1) {
            msg.setIsoUuid(isoUuids.get(0));
        }
    }

    private void validate(APIDetachIsoFromVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());

        if (isoUuids.size() > 1 && msg.getIsoUuid() == null) {
            throw new ApiMessageInterceptionException(operr("VM[uuid:%s] has multiple ISOs attached, specify the isoUuid when detaching", msg.getVmInstanceUuid()));
        }

        if (msg.getIsoUuid() == null) {
            fillIsoUuid(msg);
        }
    }

    private void validate(APICreateVmNicMsg msg) {
        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.select(L3NetworkVO_.state, L3NetworkVO_.system, L3NetworkVO_.category, L3NetworkVO_.type, L3NetworkVO_.enableIPAM);
        l3q.add(L3NetworkVO_.uuid, Op.EQ, msg.getL3NetworkUuid());
        Tuple t = l3q.findTuple();
        L3NetworkState l3state = t.get(0, L3NetworkState.class);

        if (l3state == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is disabled", msg.getL3NetworkUuid()));
        }

        if (msg.getIp() != null) {
            SimpleQuery<NormalIpRangeVO> iprq = dbf.createQuery(NormalIpRangeVO.class);
            iprq.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            List<NormalIpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (NormalIpRangeVO ipr : iprs) {
                if (NetworkUtils.isInRange(msg.getIp(), ipr.getStartIp(), ipr.getEndIp())) {
                    found = true;
                    break;
                }
            }

            if (!t.get(4, Boolean.class)) {
                found = true;
            }

            if (!found) {
                throw new ApiMessageInterceptionException(argerr("the static IP[%s] is not in any IP range of the L3 network[uuid:%s]", msg.getIp(), msg.getL3NetworkUuid()));
            }

            SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
            uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            uq.add(UsedIpVO_.ip, Op.EQ, msg.getIp());
            if (uq.isExists()) {
                throw new ApiMessageInterceptionException(operr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", msg.getIp(), msg.getL3NetworkUuid()));
            }
        }
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.type, VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        Tuple t = q.findTuple();
        String type = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                            msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.getIpRanges().isEmpty() && l3NetworkVO.getEnableIPAM()) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getL3NetworkUuid()));
        }

        List<String> newAddedL3Uuids = new ArrayList<>(Collections.singletonList(msg.getL3NetworkUuid()));

        /* all l3 must be on same l2 */
        List<String> l2Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid, newAddedL3Uuids).select(L3NetworkVO_.l2NetworkUuid).listValues();
        l2Uuids = l2Uuids.stream().distinct().collect(Collectors.toList());
        if(l2Uuids.size() > 1) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] are belonged to different l2 networks [uuids:%s]",
                    newAddedL3Uuids, l2Uuids));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuids.get(0))
                                     .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] are belonged to l2 networks [uuids:%s] that have not been attached to any cluster",
                    newAddedL3Uuids, l2Uuids));
        }

        String sql = "select ip.l3NetworkUuid from UsedIpVO ip, VmNicVO nic where ip.vmNicUuid = nic.uuid and nic.vmInstanceUuid = :vmUuid and ip.l3NetworkUuid in (:l3Uuids)";
        List<String> attachedL3Uuids = SQL.New(sql, String.class)
                .param("vmUuid", msg.getVmInstanceUuid())
                .param("l3Uuids", newAddedL3Uuids)
                .list();
        if (attachedL3Uuids != null && !attachedL3Uuids.isEmpty()) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }

            List<String> attachedNonGuestL3Uuids = Q.New(L3NetworkVO.class).select(L3NetworkVO_.uuid).
                    notEq(L3NetworkVO_.category, L3NetworkCategory.Private).in(L3NetworkVO_.uuid, attachedL3Uuids).listValues();
            if (attachedNonGuestL3Uuids != null && !attachedNonGuestL3Uuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("unable to attach a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }
        }

        for (String l3Uuid : newAddedL3Uuids) {
            L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3Vo.getState() == L3NetworkState.Disabled) {
                throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is disabled", l3Uuid));
            }
            if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3Vo.isSystem()) {
                throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                        l3Uuid));
            }
        }

        Map<String, List<String>> staticIps = new StaticIpOperator().getStaticIpbySystemTag(msg.getSystemTags());
        msg.setNicNetworkInfo(new StaticIpOperator().getNicNetworkInfoBySystemTag(msg.getSystemTags()).entrySet()
                .stream()
                .filter(entry -> Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, entry.getKey()).eq(L3NetworkVO_.enableIPAM, Boolean.FALSE).isExists())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (msg.getStaticIp() != null) {
            staticIps.computeIfAbsent(msg.getL3NetworkUuid(), k -> new ArrayList<>()).add(msg.getStaticIp());
            SimpleQuery<NormalIpRangeVO> iprq = dbf.createQuery(NormalIpRangeVO.class);
            iprq.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            List<NormalIpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (NormalIpRangeVO ipr : iprs) {
                if (!ipr.getIpVersion().equals(NetworkUtils.getIpversion(msg.getStaticIp()))) {
                    continue;
                }

                if (NetworkUtils.isInRange(msg.getStaticIp(), ipr.getStartIp(), ipr.getEndIp())) {
                    found = true;
                    break;
                }
            }

            if (!l3NetworkVO.getEnableIPAM()) {
                found = true;
            }

            if (!found) {
                throw new ApiMessageInterceptionException(argerr("the static IP[%s] is not in any IP range of the L3 network[uuid:%s]", msg.getStaticIp(), msg.getL3NetworkUuid()));
            }

            SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
            uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            uq.add(UsedIpVO_.ip, Op.EQ, msg.getStaticIp());
            if (uq.isExists()) {
                throw new ApiMessageInterceptionException(operr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", msg.getStaticIp(), msg.getL3NetworkUuid()));
            }
        }

        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            if (!newAddedL3Uuids.contains(e.getKey())) {
                throw new ApiMessageInterceptionException(argerr("static ip l3 uuid[%s] is not included in nic l3 [%s]", e.getKey(), newAddedL3Uuids));
            }

            String l3Uuid = e.getKey();
            List<String> ips = e.getValue();
            SimpleQuery<NormalIpRangeVO> iprq = dbf.createQuery(NormalIpRangeVO.class);
            iprq.add(NormalIpRangeVO_.l3NetworkUuid, Op.EQ, l3Uuid);
            List<NormalIpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (String staticIp : ips) {
                int ipVersion = IPv6Constants.IPv4;
                if (IPv6NetworkUtils.isIpv6Address(staticIp)) {
                    ipVersion = IPv6Constants.IPv6;
                }
                for (NormalIpRangeVO ipr : iprs) {
                    if (ipVersion != ipr.getIpVersion()) {
                        continue;
                    }
                    if (NetworkUtils.isInRange(staticIp, ipr.getStartIp(), ipr.getEndIp())) {
                        found = true;
                        break;
                    }
                }

                if (!l3NetworkVO.getEnableIPAM()) {
                    found = true;
                }

                if (!found) {
                    throw new ApiMessageInterceptionException(argerr("the static IP[%s] is not in any IP range of the L3 network[uuid:%s]", staticIp, l3Uuid));
                }

                SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
                uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
                uq.add(UsedIpVO_.ip, Op.EQ, staticIp);
                if (uq.isExists()) {
                    throw new ApiMessageInterceptionException(operr("the static IP[%s] has been occupied on the L3 network[uuid:%s]", staticIp, l3Uuid));
                }
            }
        }

        msg.setSecondaryL3Uuids(new ArrayList<>());
        msg.setStaticIpMap(new HashMap<>());
        for (String uuid : newAddedL3Uuids) {
            if (!uuid.equals(msg.getL3NetworkUuid())) {
                msg.getSecondaryL3Uuids().add(uuid);
            }
        }

        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            msg.getStaticIpMap().put(e.getKey(), e.getValue());
        }
    }

    private void validate(APIAttachVmNicToVmMsg msg) {
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        String type = vmInstanceVO.getType();
        VmInstanceState state = vmInstanceVO.getState();

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);

        if (vmNicVO.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. The nic has been attached with vm[uuid: %s]", vmNicVO.getVmInstanceUuid()));
        }

        boolean exist = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid())
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        L3NetworkVO l3NetworkVO = dbf.findByUuid(vmNicVO.getL3NetworkUuid(), L3NetworkVO.class);
        if (exist) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }

            if (!L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
                throw new ApiMessageInterceptionException(operr("unable to attach the nic with a non-guest L3 network. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }
        }

        L3NetworkState l3state = l3NetworkVO.getState();
        boolean system = l3NetworkVO.isSystem();

        if (l3state == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is disabled", l3NetworkVO.getUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && system) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is a system network and vm is a user vm",
                    l3NetworkVO.getUuid()));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3NetworkVO.getL2NetworkUuid())
                                     .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its l2 network [uuid:%s] that have not been attached to any cluster",
                    l3NetworkVO.getL2NetworkUuid()));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIChangeVmNicStateMsg msg) {
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        if (msg.getState().equals(VmNicState.enable.toString())) {
            MacOperator mo = new MacOperator();
            if (mo.checkDuplicateMac(nicVO.getHypervisorType(), nicVO.getMac())) {
                throw new ApiMessageInterceptionException(Platform.argerr("Duplicate mac address [%s]", nicVO.getMac()));
            }
        }

        if (!nicVO.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
            throw new ApiMessageInterceptionException(operr("could not update nic[uuid: %s] state, due to nic type[%s] not support",
                    msg.getVmNicUuid(), nicVO.getType()));
        }
        if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, nicVO.getVmInstanceUuid())
                .eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                .eq(VmInstanceVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE).isExists()) {
            throw new ApiMessageInterceptionException(operr("could not update nic[uuid: %s] state, due to vm not support",
                    msg.getVmNicUuid()));
        }
        msg.setVmInstanceUuid(nicVO.getVmInstanceUuid());
        msg.l3Uuid = nicVO.getL3NetworkUuid();
    }

    @Transactional(readOnly = true)
    private void validate(APIDetachL3NetworkFromVmMsg msg) {
        String sql = "select vm.uuid, vm.state from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getVmNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to detach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    vmUuid, state));
        }

        msg.setVmInstanceUuid(vmUuid);

        msg.l3Uuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).select(VmNicVO_.l3NetworkUuid).findValue();
    }

    private void validate(APIGetVmAttachableDataVolumeMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (state != VmInstanceState.Stopped && state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr("vm[uuid:%s] can only attach volume when state is Running or Stopped, current state is %s", msg.getVmInstanceUuid(), state));
        }
    }

    private void validateRootDiskOffering(ImageMediaType imgFormat, APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (imgFormat == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null) {
                    throw new ApiMessageInterceptionException(argerr("image mediaType is ISO but missing root disk settings"));
                }

                if (msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(operr("Unexpected root disk settings"));
                }
            }
        }
    }

    private void validatePsWhetherSameCluster(APICreateVmInstanceMsg msg) {
        if (msg.getPrimaryStorageUuidForRootVolume() == null || msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String primaryStorageUuidForDataVolume = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
        if (primaryStorageUuidForDataVolume == null) {
            return;
        }

        List<String> clusterUuidsForRootVolume = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.clusterUuid).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuidForRootVolume()).listValues();
        List<String> clusterUuidsForDataVolume = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.clusterUuid).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageUuidForDataVolume).listValues();

        clusterUuidsForRootVolume.retainAll(clusterUuidsForDataVolume);
        if (clusterUuidsForRootVolume.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("the primary storage[%s] of the root volume and the primary storage[%s] of the data volume are not in the same cluster", msg.getPrimaryStorageUuidForRootVolume(), primaryStorageUuidForDataVolume));
        }
    }

    private void validateInstanceSettings(NewVmInstanceMessage2 msg) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();

        if (instanceOfferingUuid == null) {
            if (msg.getCpuNum() == null || msg.getMemorySize() == null) {
                throw new ApiMessageInterceptionException(operr("Missing CPU/memory settings"));
            }

            if (msg.getCpuNum() <= 0 || msg.getMemorySize() <= 0) {
                throw new ApiMessageInterceptionException(operr("Unexpected CPU/memory settings"));
            }

            return;
        }

        // InstanceOffering takes precedence over CPU/memory settings.
        InstanceOfferingVO ivo = dbf.findByUuid(instanceOfferingUuid, InstanceOfferingVO.class);
        if (ivo.getState() == InstanceOfferingState.Disabled) {
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s] is Disabled, can't create vm from it", instanceOfferingUuid));
        }

        if (!ivo.getType().equals(VmInstanceConstant.USER_VM_TYPE)){
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s, type:%s] is not UserVm type, can't create vm from it", instanceOfferingUuid, ivo.getType()));
        }

        msg.setCpuNum(ivo.getCpuNum());
        msg.setMemorySize(ivo.getMemorySize());
    }

    private void validateDataDiskSizes(APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (CollectionUtils.isEmpty(msg.getDataDiskSizes())) {
            return;
        }
        msg.getDataDiskSizes().forEach(dataDiskSize -> {
            if (dataDiskSize <= 0) {
                throw new ApiMessageInterceptionException(operr("Unexpected data disk settings. dataDiskSizes need to be greater than 0"));
            }
        });
    }

    private void validate(APICreateVmInstanceMsg msg) {
        validate((NewVmInstanceMessage2) msg);

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.state, ImageVO_.system, ImageVO_.mediaType, ImageVO_.status);
        imgq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        Tuple imgt = imgq.findTuple();
        ImageState imgState = imgt.get(0, ImageState.class);
        if (imgState == ImageState.Disabled) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is Disabled, can't create vm from it", msg.getImageUuid()));
        }

        ImageStatus imgStatus = imgt.get(3, ImageStatus.class);
        if (imgStatus != ImageStatus.Ready) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not ready yet, can't create vm from it", msg.getImageUuid()));
        }

        ImageMediaType imgFormat = imgt.get(2, ImageMediaType.class);
        if (imgFormat != ImageMediaType.RootVolumeTemplate && imgFormat != ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", msg.getImageUuid(), imgFormat));
        }

        boolean isSystemImage = imgt.get(1, Boolean.class);
        if (isSystemImage && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is system image, can't be used to create user vm", msg.getImageUuid()));
        }

        validateRootDiskOffering(imgFormat, msg);
        validateDataDiskSizes(msg);

        List<String> allDiskOfferingUuids = new ArrayList<String>();
        if (msg.getRootDiskOfferingUuid() != null) {
            allDiskOfferingUuids.add(msg.getRootDiskOfferingUuid());
        }
        if (msg.getDataDiskOfferingUuids() != null) {
            allDiskOfferingUuids.addAll(msg.getDataDiskOfferingUuids());
        }

        if (!allDiskOfferingUuids.isEmpty()) {
            SimpleQuery<DiskOfferingVO> dq = dbf.createQuery(DiskOfferingVO.class);
            dq.select(DiskOfferingVO_.uuid);
            dq.add(DiskOfferingVO_.state, Op.EQ, DiskOfferingState.Disabled);
            dq.add(DiskOfferingVO_.uuid, Op.IN, allDiskOfferingUuids);
            List<String> diskUuids = dq.listValue();
            if (!diskUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("disk offerings[uuids:%s] are Disabled, can not create vm from it", diskUuids));
            }
        }

        validatePsWhetherSameCluster(msg);
    }

    private void validate(APICreateVmInstanceFromVolumeMsg msg) {
        validate((NewVmInstanceMessage2) msg);

        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume.isShareable()) {
            throw new ApiMessageInterceptionException(operr("cannot create vm instance from a shareable volume."));
        }

        if (volume.isAttached()) {
            throw new ApiMessageInterceptionException(operr("could not create vm instance from a attached volume."));
        }

        if (volume.getStatus() != VolumeStatus.Ready || volume.getState() != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] could not satisfy conditions[state:Enabled status:Ready]", msg.getVolumeUuid()));
        }
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        validate((NewVmInstanceMessage2) msg);
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        validate((NewVmInstanceMessage2) msg);
    }

    private void validate(NewVmInstanceMessage2 msg) {
        validateInstanceSettings(msg);

        Set<String> macs = new HashSet<>();
        if (null != msg.getSystemTags()) {
            Optional<String> duplicateMac = msg.getSystemTags().stream()
                    .filter(t -> VmSystemTags.CUSTOM_MAC.isMatch(t))
                    .map(t -> t.split("::")[2].toLowerCase())
                    .filter(t -> !macs.add(t))
                    .findAny();
            if (duplicateMac.isPresent()){
                throw new ApiMessageInterceptionException(operr(
                        "Not allowed same mac [%s]", duplicateMac.get()));
            }
        }

        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state);
        List<String> uuids = new ArrayList<>(msg.getL3NetworkUuids());
        List<String> duplicateElements = getDuplicateElementsOfList(uuids);
        if (duplicateElements.size() > 0) {
            throw new ApiMessageInterceptionException(operr("Can't add same uuid in the l3Network,uuid: %s", duplicateElements.get(0)));
        }

        l3q.add(L3NetworkVO_.uuid, Op.IN, msg.getL3NetworkUuids());
        List<Tuple> l3ts = l3q.listTuple();
        for (Tuple t : l3ts) {
            String l3Uuid = t.get(0, String.class);
            Boolean system = t.get(1, Boolean.class);
            L3NetworkState state = t.get(2, L3NetworkState.class);
            if (state != L3NetworkState.Enabled) {
                throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid));
            }
            if (system && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid));
            }
        }

        // smaller taking precedence
        if (msg.getHostUuid() != null) {
            msg.setClusterUuid(null);
            msg.setZoneUuid(null);
        } else if (msg.getClusterUuid() != null) {
            msg.setZoneUuid(null);
        }

        if (msg.getZoneUuid() != null) {
            SimpleQuery<ZoneVO> zq = dbf.createQuery(ZoneVO.class);
            zq.select(ZoneVO_.state);
            zq.add(ZoneVO_.uuid, Op.EQ, msg.getZoneUuid());
            ZoneState zoneState = zq.findValue();
            if (zoneState == ZoneState.Disabled) {
                throw new ApiMessageInterceptionException(operr("zone[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getZoneUuid()));
            }
        }

        if (msg.getClusterUuid() != null) {
            SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
            cq.select(ClusterVO_.state);
            cq.add(ClusterVO_.uuid, Op.EQ, msg.getClusterUuid());
            ClusterState clusterState = cq.findValue();
            if (clusterState == ClusterState.Disabled) {
                throw new ApiMessageInterceptionException(operr("cluster[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getClusterUuid()));
            }
        }

        if (msg.getHostUuid() != null) {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.state, HostVO_.status);
            hq.add(HostVO_.uuid, Op.EQ, msg.getHostUuid());
            Tuple t = hq.findTuple();
            HostState hostState = t.get(0, HostState.class);
            if (hostState == HostState.Disabled) {
                throw new ApiMessageInterceptionException(operr("host[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getHostUuid()));
            }

            HostStatus connectionState = t.get(1, HostStatus.class);
            if (connectionState != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(operr("host[uuid:%s] is specified but it's connection status is %s, can not create vm from it", msg.getHostUuid(), connectionState));
            }
        }

        if (msg.getType() == null) {
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
        }

        if (VmInstanceConstant.USER_VM_TYPE.equals(msg.getType())) {
            if (msg.getDefaultL3NetworkUuid() == null && msg.getL3NetworkUuids().size() != 1) {
                throw new ApiMessageInterceptionException(argerr("there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null"));
            } else if (msg.getDefaultL3NetworkUuid() == null && msg.getL3NetworkUuids().size() == 1) {
                msg.setDefaultL3NetworkUuid(msg.getL3NetworkUuids().get(0));
            } else if (msg.getDefaultL3NetworkUuid() != null && !msg.getL3NetworkUuids().contains(msg.getDefaultL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr("defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids%s", msg.getDefaultL3NetworkUuid(), msg.getL3NetworkUuids()));
            }
        }

        validateCdRomsTag(msg);
    }

    private boolean isCpuTopologyTags(String tag) {
        return VmHardwareSystemTags.CPU_SOCKETS.isMatch(tag)
                || VmHardwareSystemTags.CPU_CORES.isMatch(tag)
                || VmHardwareSystemTags.CPU_THREADS.isMatch(tag);
    }

    private static class CpuTopology {
        int cpuNum;
        Integer cpuSockets;
        Integer cpuCores;
        Integer cpuThreads;

        public CpuTopology(int cpuNum, List<String> systemTags) {
            this.cpuNum = cpuNum;

            this.cpuSockets = getValueFromSystemTag(systemTags, VmHardwareSystemTags.CPU_SOCKETS, VmHardwareSystemTags.CPU_SOCKETS_TOKEN);
            this.cpuCores = getValueFromSystemTag(systemTags, VmHardwareSystemTags.CPU_CORES, VmHardwareSystemTags.CPU_CORES_TOKEN);
            this.cpuThreads = getValueFromSystemTag(systemTags, VmHardwareSystemTags.CPU_THREADS, VmHardwareSystemTags.CPU_THREADS_TOKEN);
        }

        private Integer getValueFromSystemTag(List<String> systemTags, PatternedSystemTag tag, String token) {
            for (String t : systemTags) {
                if (tag.isMatch(t)) {
                    try {
                        return Integer.valueOf(tag.getTokenByTag(t, token));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }

            return null;
        }

        boolean isEmpty() {
            return cpuSockets == null && cpuCores == null && cpuThreads == null;
        }

        void calculateValidTopology() {
            if (cpuSockets == null) {
                if (cpuCores == null && cpuThreads == null) {
                    return;
                }

                throw new ApiMessageInterceptionException(argerr("cpuSockets must be specified when cpuCores or cpuThreads is set"));
            }

            if (cpuCores == null && cpuThreads == null) {
                cpuCores = cpuNum / cpuSockets;
                cpuThreads = 1;
            } else if (cpuCores != null && cpuThreads == null) {
                cpuThreads = cpuNum / cpuSockets / cpuCores;
            } else if (cpuCores == null) {
                cpuCores = cpuNum / cpuSockets / cpuThreads;
            }

            // check the topology is valid
            if (cpuNum == cpuSockets * cpuCores * cpuThreads) {
                return;
            }

            throw new ApiMessageInterceptionException(argerr("invalid cpu topology." +
                            " cpuNum[%s], cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                    cpuNum, cpuSockets, cpuCores, cpuThreads));
        }
    }

    private void validateCdRomsTag(NewVmInstanceMessage msg) {
        if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String tagValue = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.CREATE_VM_CD_ROM_LIST);
        if (tagValue == null) {
            return;
        }

        Map<String, String> tokens = VmSystemTags.CREATE_VM_CD_ROM_LIST.getTokensByTag(tagValue);
        List<String> cdRoms = new ArrayList<>();
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_0));
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_1));
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_2));
        cdRoms = cdRoms.stream().filter(i -> i != null && !VmInstanceConstant.NONE_CDROM.equalsIgnoreCase(i) && !VmInstanceConstant.EMPTY_CDROM.equalsIgnoreCase(i)).collect(Collectors.toList());
        if (cdRoms == null || cdRoms.isEmpty()) {
            return;
        }

        for (String cdRomIsoUuid : cdRoms) {
            ImageVO imageVO = dbf.findByUuid(cdRomIsoUuid, ImageVO.class);
            if (imageVO == null) {
                throw new ApiMessageInterceptionException(argerr("The image[uuid=%s] does not exist", cdRomIsoUuid));
            }
        }

        if (cdRoms.size() != new HashSet<>(cdRoms).size()) {
            throw new ApiMessageInterceptionException(argerr("Do not allow to mount duplicate ISO"));
        }
    }

    private void validate(APIDestroyVmInstanceMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VmInstanceVO.class)) {
            APIDestroyVmInstanceEvent evt = new APIDestroyVmInstanceEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APISetVmConsolePasswordMsg msg) {
        String pwd = msg.getConsolePassword();
        if (pwd.startsWith("password")){
            throw new ApiMessageInterceptionException(argerr("The console password cannot start with 'password' which may trigger a VNC security issue"));
        }
    }

    private void validate(APIAttachL3NetworkToVmNicMsg msg) {
        throw new ApiMessageInterceptionException(argerr("can not call this api because it's Deprecated"));
    }

    private void validate(APIDeleteVmCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
        msg.setVmInstanceUuid(vmCdRomVO.getVmInstanceUuid());
    }

    private void validate(APIUpdateVmCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
        msg.setVmInstanceUuid(vmCdRomVO.getVmInstanceUuid());
    }

    private void validate(APISetVmInstanceDefaultCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);

        if (vmCdRomVO.getDeviceId() == 0) {
            throw new ApiMessageInterceptionException(argerr("The CdRom[%s] Already the default", vmCdRomVO.getUuid()));
        }
    }

}
