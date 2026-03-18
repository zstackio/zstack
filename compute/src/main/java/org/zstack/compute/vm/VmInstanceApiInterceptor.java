package org.zstack.compute.vm;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.compute.VmNicUtils;
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
import org.zstack.header.network.l2.*;
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
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;
import org.zstack.utils.network.NetworkUtils;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.getDuplicateElementsOfList;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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

    /**
     * Common validation logic for IPv4 and IPv6 static IPs.
     * Checks IP format and whether it is already assigned to the current NIC.
     */
    private void validateStaticIpCommon(VmNicVO vmNicVO, L3NetworkVO l3NetworkVO, String ip,
                                        int ipVersion, String formatErrorCode, String duplicateErrorCode) {
        // 1. Format validation
        boolean isValidFormat = (ipVersion == IPv6Constants.IPv4)
                ? NetworkUtils.isIpv4Address(ip)
                : IPv6NetworkUtils.isIpv6Address(ip);

        if (!isValidFormat) {
            String ipType = (ipVersion == IPv6Constants.IPv4) ? "IPv4" : "IPv6";
            throw new ApiMessageInterceptionException(argerr(formatErrorCode,
                    "%s is not a valid " + ipType + " address", ip));
        }

        // 2. Check whether it conflicts with the IPs already assigned to the current NIC
        for (UsedIpVO ipVo : vmNicVO.getUsedIps()) {
            if (ipVo.getIpVersion() != ipVersion) {
                continue;
            }

            if (ipVo.getL3NetworkUuid().equals(l3NetworkVO.getUuid())) {
                if (ipVo.getIp().equals(ip)) {
                    throw new ApiMessageInterceptionException(argerr(duplicateErrorCode,
                            "ip address [%s] already set to vmNic [uuid:%s]", ip, vmNicVO.getUuid()));
                }
            }
        }
    }

    /**
     * Check whether an IP is already in use (using error code ORG_ZSTACK_COMPUTE_VM_10105).
     */
    private void checkIpOccupied(String ip, String l3NetworkUuid) {
        if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, ip).eq(UsedIpVO_.l3NetworkUuid, l3NetworkUuid).isExists()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10105,
                    "the static IP[%s] has been occupied on the L3 network[uuid:%s]", ip, l3NetworkUuid));
        }
    }

    /**
     * Batch check whether multiple IPs are already in use.
     */
    private void checkIpsOccupied(List<String> ips, String l3NetworkUuid) {
        if (ips == null || ips.isEmpty()) {
            return;
        }

        List<String> occupiedIps = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3NetworkUuid)
                .in(UsedIpVO_.ip, ips)
                .select(UsedIpVO_.ip)
                .listValues();

        if (!occupiedIps.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10105,
                    "the static IP%s has been occupied on the L3 network[uuid:%s]", occupiedIps, l3NetworkUuid));
        }
    }


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
        } else if (msg instanceof APIUpdateConsolePasswordMsg) {
            validate((APIUpdateConsolePasswordMsg) msg);
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
        } else if (msg instanceof APIFstrimVmMsg) {
            validate((APIFstrimVmMsg) msg);
        } else if (msg instanceof APITakeVmConsoleScreenshotMsg) {
            validate((APITakeVmConsoleScreenshotMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APITakeVmConsoleScreenshotMsg msg) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!vm.getState().equals(VmInstanceState.Running)) {
            throw new ApiMessageInterceptionException(operr(
            ORG_ZSTACK_COMPUTE_VM_10092,         "can not take vm console screenshot for vm[uuid:%s] which is not Running", msg.getVmInstanceUuid()));
        }
    }

    private void validate(APIGetInterdependentL3NetworksBackupStoragesMsg msg) {
        if (msg.getL3NetworkUuids() == null && msg.getBackupStorageUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10093, "either l3NetworkUuids or backupStorageUuid must be set"));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10094, "could not change to L3 network, the l2 of l3[uuid:%s, %s] use different vswitch",
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10095, error, msg.getVmNicUuid()));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10096, "unable to change to L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getDestL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.enableIpAddressAllocation() && l3NetworkVO.getIpRanges().isEmpty()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10097, "unable to change to L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getDestL3NetworkUuid()));
        }

        List<String> newAddedL3Uuids = new ArrayList<>(Collections.singletonList(msg.getDestL3NetworkUuid()));

        /* all l3 must be on same l2 */
        List<String> l2Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid, newAddedL3Uuids).select(L3NetworkVO_.l2NetworkUuid).listValues();
        l2Uuids = l2Uuids.stream().distinct().collect(Collectors.toList());
        if(l2Uuids.size() > 1) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10098, "unable to change to L3 network. The L3 network[uuid:%s] are belonged to different l2 networks [uuids:%s]",
                    newAddedL3Uuids, l2Uuids));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuids.get(0))
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10099, "unable to change to L3 network. The L3 network[uuid:%s] are belonged to l2 networks [uuids:%s] that have not been attached to any cluster",
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
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10100, "unable to change to L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }

            List<String> attachedNonGuestL3Uuids = Q.New(L3NetworkVO.class).select(L3NetworkVO_.uuid).
                    notEq(L3NetworkVO_.category, L3NetworkCategory.Private).in(L3NetworkVO_.uuid, attachedL3Uuids).listValues();
            if (attachedNonGuestL3Uuids != null && !attachedNonGuestL3Uuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10101, "unable to change to a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }
        }

        for (String l3Uuid : newAddedL3Uuids) {
            L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3Vo.getState() == L3NetworkState.Disabled) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10102, "unable to change to L3 network. The L3 network[uuid:%s] is disabled", l3Uuid));
            }
            if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3Vo.isSystem()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10103, "unable to change to L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                        l3Uuid));
            }
        }

        // Build NicRoleContext for resolve logic
        String defaultL3Uuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.defaultL3NetworkUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        int vmNicCount = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmUuid).count().intValue();
        boolean isDefaultNic = srcL3Uuid.equals(defaultL3Uuid);
        boolean isOnlyNic = vmNicCount == 1;

        StaticIpOperator staticIpOp = new StaticIpOperator();
        Map<String, NicIpAddressInfo> nicNetworkInfo = staticIpOp.getNicNetworkInfoBySystemTag(msg.getSystemTags());

        // Validate IP availability
        staticIpOp.validateIpAvailability(nicNetworkInfo);

        // Resolve netmask/gateway using unified logic (no existingIp reuse for ChangeNicNetwork)
        StaticIpOperator.NicRoleContext nicRole = new StaticIpOperator.NicRoleContext(isDefaultNic, isOnlyNic);
        List<String> resolvedTags = staticIpOp.fillUpStaticIpInfoToVmNics(nicNetworkInfo,
                nicRole, new StaticIpOperator.ExistingIpContext());

        // Remove any existing netmask/gateway/prefix tags, then add resolved ones
        if (msg.getSystemTags() != null) {
            msg.getSystemTags().removeIf(tag ->
                    VmSystemTags.IPV4_NETMASK.isMatch(tag) || VmSystemTags.IPV4_GATEWAY.isMatch(tag)
                            || VmSystemTags.IPV6_PREFIX.isMatch(tag) || VmSystemTags.IPV6_GATEWAY.isMatch(tag));
        }
        if (!resolvedTags.isEmpty()) {
            msg.getSystemTags().addAll(resolvedTags);
        }

        Map<String, List<String>> staticIps = new StaticIpOperator().getStaticIpbySystemTag(msg.getSystemTags());

        // If staticIp parameter is provided, add it to the static IP list
        if (msg.getStaticIp() != null) {
            staticIps.computeIfAbsent(msg.getDestL3NetworkUuid(), k -> new ArrayList<>()).add(msg.getStaticIp());
        }

        msg.setRequiredIpMap(new HashMap<>());

        // Unified loop: validate and set all static IPs together
        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            String l3Uuid = e.getKey();
            List<String> ips = e.getValue();

            // Validate that the L3 network UUID is in the allowed list
            if (!newAddedL3Uuids.contains(l3Uuid)) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10106,
                        "static ip l3 uuid[%s] is not included in nic l3 [%s]", l3Uuid, newAddedL3Uuids));
            }

            // Performance optimization: batch check IP occupation (one query instead of N)
            checkIpsOccupied(ips, l3Uuid);

            // Set requiredIpMap (merged into this loop to eliminate redundant iteration)
            msg.getRequiredIpMap().put(l3Uuid, ips);
        }

        validateDnsAddresses(msg.getDnsAddresses());
    }

    private void validateDnsAddresses(List<String> dnsAddresses) {
        if (dnsAddresses == null || dnsAddresses.isEmpty()) {
            return;
        }

        if (dnsAddresses.size() > VmInstanceConstant.MAXIMUM_NIC_DNS_NUMBER) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_VM_10321, "at most %d DNS addresses are allowed, but got %d",
                    VmInstanceConstant.MAXIMUM_NIC_DNS_NUMBER, dnsAddresses.size()));
        }

        for (String dns : dnsAddresses) {
            if (!NetworkUtils.isIpv4Address(dns) && !IPv6NetworkUtils.isIpv6Address(dns)) {
                throw new ApiMessageInterceptionException(argerr(
                        ORG_ZSTACK_COMPUTE_VM_10322, "invalid DNS address[%s], must be a valid IPv4 or IPv6 address", dns));
            }
        }
    }

    private void validate(APIGetCandidateZonesClustersHostsForCreatingVmMsg msg) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();

        if (instanceOfferingUuid == null) {
            if (msg.getCpuNum() == null || msg.getMemorySize() == null) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10111, "Missing CPU/memory settings"));
            }
        }

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image != null && image.getMediaType() == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10112, "the image[name:%s, uuid:%s] is an ISO, rootDiskSize must be set",
                            image.getName(), image.getUuid()));
                }
            }
        }
    }

    private void validate(final APICreateVmCdRomMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (!vo.getState().equals(VmInstanceState.Stopped)) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_COMPUTE_VM_10113,         "Can not create CD-ROM for vm[uuid:%s] which is in state[%s] ", msg.getVmInstanceUuid(), vo.getState().toString()));
        }
    }

    private void validate(final APIUpdateVmNicDriverMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vo.getPlatform().equals(ImagePlatform.Other.toString())) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_COMPUTE_VM_10114,         "Current platform %s not support update nic driver yet", vo.getPlatform()));
        }

        List<String> supportNicDriverTypes = nicManager.getSupportNicDriverTypes();
        if (!supportNicDriverTypes.contains(msg.getDriverType())) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_COMPUTE_VM_10115,         "Nic driver %s not support yet", msg.getDriverType()));
        }
    }

    private void validate(final APIGetCandidatePrimaryStoragesForCreatingVmMsg msg) {
        ImageMediaType mediaType = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).select(ImageVO_.mediaType).findValue();
        if (ImageMediaType.ISO == mediaType) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10116, "rootDiskSize is needed when image media type is ISO"));
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
                    ORG_ZSTACK_COMPUTE_VM_10117,         "the vm[uuid:%s] is already on host[uuid:%s]", msg.getVmInstanceUuid(), msg.getHostUuid()
                    ));
                }

                if (vo.getState() == VmInstanceState.Paused && VmSystemTags.VM_STATE_PAUSED_AFTER_MIGRATE.hasTag(msg.getVmInstanceUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_VM_10118,         "the vm[uuid:%s] is still paused after the last migration, please resume it before migrate.", msg.getVmInstanceUuid()));
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
                    ORG_ZSTACK_COMPUTE_VM_10119,         "the VM cannot do online cpu/memory update because of disabling Instance Offering Online Modification. Please stop the VM then do the cpu/memory update again"
                    ));
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10120, "The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }

                if (VmInstanceState.Stopped.equals(vo.getState())) {
                    return;
                }

                if (instanceOfferingVO.getCpuNum() < vo.getCpuNum() || instanceOfferingVO.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_VM_10121,         "can't decrease capacity when vm[uuid:%s] is running", vo.getUuid()
                    ));
                }
            }
        }.execute();
    }


    private void validate(APIUpdateVmInstanceMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                if (msg.getReservedMemorySize() != null) {
                    Long memorySize = msg.getMemorySize() == null ? vo.getMemorySize() : msg.getMemorySize();
                    if (msg.getReservedMemorySize() > memorySize) {
                        throw new ApiMessageInterceptionException(argerr(
                        ORG_ZSTACK_COMPUTE_VM_10122,         "reservedMemorySize[%s] is greater than memorySize[%s]", msg.getReservedMemorySize(), memorySize
                        ));
                    }
                }

                if (msg.getCpuNum() == null && msg.getMemorySize() == null) {
                    return;
                }

                boolean uniqueVmName = VmGlobalConfig.UNIQUE_VM_NAME.value(Boolean.class);
                if (uniqueVmName && Q.New(VmInstanceVO.class).eq(VmInstanceVO_.name, msg.getName()).notEq(VmInstanceVO_.uuid, msg.getUuid()).isExists()) {
                    throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10123, "could not create vm, a vm with the name [%s] already exists",
                            msg.getName()));
                }

                Integer cpuSum = msg.getCpuNum();
                Long memorySize = msg.getMemorySize();

                VmInstanceState vmState = q(VmInstanceVO.class).select(VmInstanceVO_.state).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();
                boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getUuid(), Boolean.class);
                if (!numa && !VmInstanceState.Stopped.equals(vmState)) {
                    if (cpuSum != null && cpuSum != vo.getCpuNum()) {
                        throw new ApiMessageInterceptionException(argerr(
                        ORG_ZSTACK_COMPUTE_VM_10124,         "the VM cannot do cpu hot plug because of disabling cpu hot plug. Please stop the VM then do the cpu hot plug again"
                        ));

                    }

                    if (memorySize != null && memorySize != vo.getMemorySize()) {
                        throw new ApiMessageInterceptionException(argerr(
                        ORG_ZSTACK_COMPUTE_VM_10125,         "the VM cannot do memory hot plug because of disabling memory hot plug. Please stop the VM then do the memory hot plug again"
                        ));
                    }
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10126, "The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }

                if (VmInstanceState.Stopped.equals(vmState)) {
                    return;
                }

                if (msg.getCpuNum() != null && msg.getCpuNum() < vo.getCpuNum()) {
                    throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_VM_10127,         "can't decrease cpu of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }

                if (msg.getMemorySize() != null && msg.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_VM_10128,         "can't decrease memory size of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }
            }
        }.execute();
    }


    private void validate(APIGetInterdependentL3NetworksImagesMsg msg) {
    }

    private void validate(APIStartVmInstanceMsg msg) {
        // host uuid overrides cluster uuid
        if (msg.getHostUuid() != null) {
            msg.setClusterUuid(null);
        }
    }

    private void validateStaticIPv4(VmNicVO vmNicVO, L3NetworkVO l3NetworkVO, String ip) {
        validateStaticIpCommon(vmNicVO, l3NetworkVO, ip, IPv6Constants.IPv4,
                ORG_ZSTACK_COMPUTE_VM_10129, ORG_ZSTACK_COMPUTE_VM_10130);
    }

    private void validateStaticIPv6(VmNicVO vmNicVO, L3NetworkVO l3NetworkVO, String ip) {
        validateStaticIpCommon(vmNicVO, l3NetworkVO, ip, IPv6Constants.IPv6,
                ORG_ZSTACK_COMPUTE_VM_10132, ORG_ZSTACK_COMPUTE_VM_10133);
    }

    private void validate(APISetVmStaticIpMsg msg) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if (msg.getIp() == null && msg.getIp6() == null) {
            if(l3NetworkVO.enableIpAddressAllocation()) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10135, "could not set ip address, due to no ip address is specified"));
            }
        }
        List<VmNicVO> vmNics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid()).list();
        boolean l3Found = false;

        // Normalize IP addresses (avoid redundant calls)
        String normalizedIp = null;
        String normalizedIp6 = null;
        UsedIpVO existingIpv4 = null;
        UsedIpVO existingIpv6 = null;

        for (VmNicVO nic : vmNics) {
            if (msg.getL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                l3Found = true;
            }

            // Extract UsedIpVO records matching the same L3 and IP version from the NIC's existing IPs
            for (UsedIpVO usedIp : nic.getUsedIps()) {
                if (!msg.getL3NetworkUuid().equals(usedIp.getL3NetworkUuid())) {
                    continue;
                }

                if (usedIp.getIpVersion() != null && usedIp.getIpVersion() == IPv6Constants.IPv4) {
                    existingIpv4 = usedIp;
                } else if (usedIp.getIpVersion() != null && usedIp.getIpVersion() == IPv6Constants.IPv6) {
                    existingIpv6 = usedIp;
                }
            }
            if (msg.getIp() != null) {
                String ip = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getIp());
                if (NetworkUtils.isIpv4Address(ip)) {
                    validateStaticIPv4(nic, l3NetworkVO, ip);
                    normalizedIp = ip;
                } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                    validateStaticIPv6(nic, l3NetworkVO, ip);
                    msg.setIp(ip);
                    normalizedIp6 = ip;
                } else {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10136, "static ip [%s] format error", msg.getIp()));
                }
            }
            if (msg.getIp6() != null) {
                String ip6 = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getIp6());
                validateStaticIPv6(nic, l3NetworkVO, ip6);
                msg.setIp6(ip6);
                normalizedIp6 = ip6;
            }
        }

        if (!l3Found) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10141, "the VM[uuid:%s] has no nic on the L3 network[uuid:%s]", msg.getVmInstanceUuid(),
                    msg.getL3NetworkUuid()));
        }

        // Get the VM's default L3 network UUID for gateway enforcement
        String defaultL3NetworkUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.defaultL3NetworkUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();
        boolean isDefaultNic = msg.getL3NetworkUuid().equals(defaultL3NetworkUuid);
        boolean isOnlyNic = vmNics.size() == 1;

        StaticIpOperator staticIpOp = new StaticIpOperator();
        StaticIpOperator.NicRoleContext nicRole = new StaticIpOperator.NicRoleContext(isDefaultNic, isOnlyNic);
        StaticIpOperator.ExistingIpContext existingIpCtx = new StaticIpOperator.ExistingIpContext();
        existingIpCtx.putIpv4(msg.getL3NetworkUuid(), existingIpv4);
        existingIpCtx.putIpv6(msg.getL3NetworkUuid(), existingIpv6);

        // Fill parameters and check IP occupation using unified resolve
        if (normalizedIp != null) {
            List<NormalIpRangeVO> ipv4Ranges = Q.New(NormalIpRangeVO.class)
                    .eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            String[] ipv4Result = staticIpOp.resolveIpv4NetmaskAndGateway(normalizedIp, msg.getNetmask(), msg.getGateway(),
                    ipv4Ranges, nicRole, existingIpv4);
            msg.setNetmask(ipv4Result[0]);
            msg.setGateway(ipv4Result[1]);
            checkIpOccupied(normalizedIp, msg.getL3NetworkUuid());
        }
        if (normalizedIp6 != null) {
            List<NormalIpRangeVO> ipv6Ranges = Q.New(NormalIpRangeVO.class)
                    .eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            String[] ipv6Result = staticIpOp.resolveIpv6PrefixAndGateway(normalizedIp6, msg.getIpv6Prefix(), msg.getIpv6Gateway(),
                    ipv6Ranges, nicRole, existingIpv6);
            msg.setIpv6Prefix(ipv6Result[0]);
            msg.setIpv6Gateway(ipv6Result[1]);
            checkIpOccupied(normalizedIp6, msg.getL3NetworkUuid());
        }

        validateDnsAddresses(msg.getDnsAddresses());
    }

    private void validate(APIDeleteVmStaticIpMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10142, "the VM[uuid:%s] has no nic on the L3 network[uuid:%s]", msg.getVmInstanceUuid(),
                            msg.getL3NetworkUuid()));
        }

        if (msg.getStaticIp() != null) {
            if (!Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(UsedIpVO_.ip, msg.getStaticIp()).isExists()) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10143, "could not delete static ip [%s] for vm [uuid:%s] " +
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
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10144, "invalid boot device[%s] in boot order%s", o, msg.getBootOrder()));
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
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10145, "boot volume cannot be shareable."));
        }

        if (!msg.getVmInstanceUuid().equals(volume.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10146, "volume[uuid:%s] must be attached to vm[uuid:%s]",
                    msg.getVolumeUuid(), msg.getVmInstanceUuid()));
        }

        if (isVmHasMemorySnapshotGroup(msg.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10147, "the vm %s with memory snapshots do not support setting boot volume", msg.getVmInstanceUuid()));
        }
    }


    private void validate(APIAttachIsoToVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if (isoUuids.contains(msg.getIsoUuid())) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10148, "VM[uuid:%s] already has an ISO[uuid:%s] attached", msg.getVmInstanceUuid(), msg.getIsoUuid()));
        }

        ImageConstant.ImageMediaType type = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getIsoUuid()).select(ImageVO_.mediaType).findValue();
        if (type != ImageConstant.ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10149, "Unsupported Image Media Type: [%s] ", type));
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
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10150, "The cdRom[uuid:%s] does not exist", cdRomUuid));
            }

            if (StringUtils.isNotEmpty(cdRomVO.getIsoUuid())){
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10151, "VM[uuid:%s] cdRom[uuid:%s] has mounted the ISO", msg.getVmInstanceUuid(), cdRomUuid));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10152, "VM[uuid:%s] has multiple ISOs attached, specify the isoUuid when detaching", msg.getVmInstanceUuid()));
        }

        if (msg.getIsoUuid() == null) {
            fillIsoUuid(msg);
        }
    }

    private void validate(APICreateVmNicMsg msg) {
        L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (l3VO.getState() == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10153, "unable to attach a L3 network. The L3 network[uuid:%s] is disabled", msg.getL3NetworkUuid()));
        }

        if (msg.getIp() != null) {
            SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
            uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            uq.add(UsedIpVO_.ip, Op.EQ, msg.getIp());
            if (uq.isExists()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10155, "the static IP[%s] has been occupied on the L3 network[uuid:%s]", msg.getIp(), msg.getL3NetworkUuid()));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10156, "unable to attach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                            msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.getIpRanges().isEmpty() && l3NetworkVO.enableIpAddressAllocation()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10157, "unable to attach a L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getL3NetworkUuid()));
        }

        List<String> newAddedL3Uuids = new ArrayList<>(Collections.singletonList(msg.getL3NetworkUuid()));

        /* all l3 must be on same l2 */
        List<String> l2Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid, newAddedL3Uuids).select(L3NetworkVO_.l2NetworkUuid).listValues();
        l2Uuids = l2Uuids.stream().distinct().collect(Collectors.toList());
        if(l2Uuids.size() > 1) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10158, "unable to attach a L3 network. The L3 network[uuid:%s] are belonged to different l2 networks [uuids:%s]",
                    newAddedL3Uuids, l2Uuids));
        }

        List<L2NetworkVO> l2NetworkVOS = Q.New(L2NetworkVO.class).in(L2NetworkVO_.uuid, l2Uuids).list();
        List<L2NetworkVO> ovnL2Networks = l2NetworkVOS.stream().filter(vo ->
                vo.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVN_DPDK)).collect(Collectors.toList());
        l2NetworkVOS = l2NetworkVOS.stream().filter(vo ->
                !vo.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVN_DPDK)).collect(Collectors.toList());
        if (!l2NetworkVOS.isEmpty()) {
            List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuids.get(0))
                    .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
            if (clusterUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10159, "unable to attach a L3 network. The L3 network[uuid:%s] are belonged to l2 networks [uuids:%s] that have not been attached to any cluster",
                        newAddedL3Uuids, l2Uuids));
            }
        }

        String sql = "select nic.l3NetworkUuid from VmNicVO nic where nic.vmInstanceUuid = :vmUuid and nic.l3NetworkUuid in (:l3Uuids)";
        List<String> attachedL3Uuids = SQL.New(sql, String.class)
                .param("vmUuid", msg.getVmInstanceUuid())
                .param("l3Uuids", newAddedL3Uuids)
                .list();
        if (attachedL3Uuids != null && !attachedL3Uuids.isEmpty()) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10160, "unable to attach a L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }

            List<String> attachedNonGuestL3Uuids = Q.New(L3NetworkVO.class).select(L3NetworkVO_.uuid).
                    notEq(L3NetworkVO_.category, L3NetworkCategory.Private).in(L3NetworkVO_.uuid, attachedL3Uuids).listValues();
            if (attachedNonGuestL3Uuids != null && !attachedNonGuestL3Uuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10161, "unable to attach a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        attachedL3Uuids, msg.getVmInstanceUuid()));
            }
        }

        for (String l3Uuid : newAddedL3Uuids) {
            L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
            if (l3Vo.getState() == L3NetworkState.Disabled) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10162, "unable to attach a L3 network. The L3 network[uuid:%s] is disabled", l3Uuid));
            }
            if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3Vo.isSystem()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10163, "unable to attach a L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                        l3Uuid));
            }
        }

        new StaticIpOperator().validateSystemTagInApiMessage(msg);
        Map<String, List<String>> staticIps = new StaticIpOperator().getStaticIpbySystemTag(msg.getSystemTags());
        msg.setNicNetworkInfo(new StaticIpOperator().getNicNetworkInfoBySystemTag(msg.getSystemTags()).entrySet()
                .stream()
                .filter(entry -> !IpRangeHelper.isIpAddressAllocationEnableOnL3(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (msg.getStaticIp() != null) {
            staticIps.computeIfAbsent(msg.getL3NetworkUuid(), k -> new ArrayList<>()).add(msg.getStaticIp());

            SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
            uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            uq.add(UsedIpVO_.ip, Op.EQ, msg.getStaticIp());
            if (uq.isExists()) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10165, "the static IP[%s] has been occupied on the L3 network[uuid:%s]", msg.getStaticIp(), msg.getL3NetworkUuid()));
            }
        }

        for (Map.Entry<String, List<String>> e : staticIps.entrySet()) {
            if (!newAddedL3Uuids.contains(e.getKey())) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10166, "static ip l3 uuid[%s] is not included in nic l3 [%s]", e.getKey(), newAddedL3Uuids));
            }

            String l3Uuid = e.getKey();
            List<String> ips = e.getValue();

            for (String staticIp : ips) {

                SimpleQuery<UsedIpVO> uq = dbf.createQuery(UsedIpVO.class);
                uq.add(UsedIpVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
                uq.add(UsedIpVO_.ip, Op.EQ, staticIp);
                if (uq.isExists()) {
                    throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10168, "the static IP[%s] has been occupied on the L3 network[uuid:%s]", staticIp, l3Uuid));
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

        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            List<String> supportNicDriverTypes = nicManager.getSupportNicDriverTypes();

            VmNicParam vmNicParam;
            try {
                vmNicParam = JSONObjectUtil.toObject(msg.getVmNicParams(), VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10169, "invalid json format, causes: %s", e.getMessage()));
            }

            VmNicUtils.validateVmParms(Arrays.asList(vmNicParam), Arrays.asList(msg.getL3NetworkUuid()), supportNicDriverTypes);
        }
    }

    private void validate(APIAttachVmNicToVmMsg msg) {
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        String type = vmInstanceVO.getType();
        VmInstanceState state = vmInstanceVO.getState();

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10170, "unable to attach the nic. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);

        if (vmNicVO.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10171, "unable to attach the nic. The nic has been attached with vm[uuid: %s]", vmNicVO.getVmInstanceUuid()));
        }

        boolean exist = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid())
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        L3NetworkVO l3NetworkVO = dbf.findByUuid(vmNicVO.getL3NetworkUuid(), L3NetworkVO.class);
        if (exist) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10172, "unable to attach the nic. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }

            if (!L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10173, "unable to attach the nic with a non-guest L3 network. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }
        }

        L3NetworkState l3state = l3NetworkVO.getState();
        boolean system = l3NetworkVO.isSystem();

        if (l3state == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10174, "unable to attach the nic. Its L3 network[uuid:%s] is disabled", l3NetworkVO.getUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && system) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10175, "unable to attach the nic. Its L3 network[uuid:%s] is a system network and vm is a user vm",
                    l3NetworkVO.getUuid()));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3NetworkVO.getL2NetworkUuid())
                                     .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10176, "unable to attach the nic. Its l2 network [uuid:%s] that have not been attached to any cluster",
                    l3NetworkVO.getL2NetworkUuid()));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIChangeVmNicStateMsg msg) {
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        if (msg.getState().equals(VmNicState.enable.toString()) && !msg.getState().equals(nicVO.getState().toString())) {
            MacOperator mo = new MacOperator();
            if (mo.checkDuplicateMac(nicVO.getHypervisorType(), nicVO.getL3NetworkUuid(),
                    nicVO.getMac())) {
                throw new ApiMessageInterceptionException(Platform.argerr(ORG_ZSTACK_COMPUTE_VM_10177, "Duplicate mac address [%s]", nicVO.getMac()));
            }
        }

        if (!nicVO.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10178, "could not update nic[uuid: %s] state, due to nic type[%s] not support",
                    msg.getVmNicUuid(), nicVO.getType()));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10179, "unable to detach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10180, "vm[uuid:%s] can only attach volume when state is Running or Stopped, current state is %s", msg.getVmInstanceUuid(), state));
        }
    }

    private void validateRootDiskOffering(ImageMediaType imgFormat, APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (imgFormat == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10181, "image mediaType is ISO but missing root disk settings"));
                }

                if (msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10182, "Unexpected root disk settings"));
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
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10183, "the primary storage[%s] of the root volume and the primary storage[%s] of the data volume are not in the same cluster", msg.getPrimaryStorageUuidForRootVolume(), primaryStorageUuidForDataVolume));
        }
    }

    private void validateZoneOrClusterOrHostOrL3Exist(NewVmInstanceMessage2 msg) {
        if (CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && StringUtils.isEmpty(msg.getZoneUuid())
                && StringUtils.isEmpty(msg.getClusterUuid()) && StringUtils.isEmpty(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10184, "could not create vm, because at least one of field (l3NetworkUuids,zoneUuid,clusterUuid,hostUuid) should be set"));
        }
    }

    private void validateDataDiskSizes(APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (CollectionUtils.isEmpty(msg.getDataDiskSizes())) {
            return;
        }
        msg.getDataDiskSizes().forEach(dataDiskSize -> {
            if (dataDiskSize <= 0) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10185, "Unexpected data disk settings. dataDiskSizes need to be greater than 0"));
            }
        });
    }

    private void validate(APICreateVmInstanceMsg msg) {
        validate((NewVmInstanceMessage2) msg);

        if (CollectionUtils.isNotEmpty(msg.getDiskAOs())) {
            APICreateVmInstanceMsg.DiskAO rootDiskAO = msg.getDiskAOs().stream()
                    .filter(APICreateVmInstanceMsg.DiskAO::isBoot).findFirst().orElse(null);
            if (rootDiskAO == null) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10186, "missing root disk"));
            }
            msg.setPlatform(rootDiskAO.getPlatform());
            msg.setGuestOsType(rootDiskAO.getGuestOsType());
            msg.setArchitecture(rootDiskAO.getArchitecture());
            if (CollectionUtils.isNotEmpty(rootDiskAO.getSystemTags())) {
                if (rootDiskAO.getSystemTags().contains(VmSystemTags.VIRTIO.getTagFormat())) {
                    msg.setVirtio(true);
                }
            } else {
                msg.setVirtio(false);
            }
        }

        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).find();
        if (image == null) {
            String err = "";
            if (msg.getPlatform() == null) {
                err = Platform.missingVariables("platform");
            }

            if (msg.getGuestOsType() == null) {
                err += Platform.missingVariables("guestOsType");
            }

            if (msg.getArchitecture() == null) {
                err += Platform.missingVariables("architecture");
            }

            if (msg.getRootDiskOfferingUuid() == null && msg.getRootDiskSize() == null) {
                err += "rootDiskOfferingUuid or rootDiskSize cannot be all null";
            }

            if (!err.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10187, String.format("when imageUuid is null, %s", err)));
            }
        } else {
            ImageState imgState = image.getState();
            if (imgState == ImageState.Disabled) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10188, "image[uuid:%s] is Disabled, can't create vm from it", msg.getImageUuid()));
            }

            ImageStatus imgStatus = image.getStatus();
            if (imgStatus != ImageStatus.Ready) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10189, "image[uuid:%s] is not ready yet, can't create vm from it", msg.getImageUuid()));
            }

            ImageMediaType imgFormat = image.getMediaType();
            if (imgFormat != ImageMediaType.RootVolumeTemplate && imgFormat != ImageMediaType.ISO) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10190, "image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", msg.getImageUuid(), imgFormat));
            }

            boolean isSystemImage = image.isSystem();
            if (isSystemImage && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10191, "image[uuid:%s] is system image, can't be used to create user vm", msg.getImageUuid()));
            }

            if (msg.getPlatform() == null && image.getPlatform() == null) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10192, "at least one of field platform in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            }

            if (msg.getGuestOsType() == null && image.getGuestOsType() == null) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10193, "at least one of field guestOsType in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            }

            if (msg.getArchitecture() == null && image.getArchitecture() == null) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10194, "at least one of field architecture in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            }

            validateRootDiskOffering(imgFormat, msg);

            if (msg.getVirtio() == null) {
                if (image.getVirtio() != null) {
                    msg.setVirtio(image.getVirtio());
                } else {
                    msg.setVirtio(false);
                }
            }
        }

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
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10195, "disk offerings[uuids:%s] are Disabled, can not create vm from it", diskUuids));
            }
        }

        validatePsWhetherSameCluster(msg);
        validateDataDiskAOs(msg);
    }

    private void validateDataDiskAOs(APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }
        for (APICreateVmInstanceMsg.DiskAO diskAO : msg.getDiskAOs()) {
            if (diskAO.isBoot()) {
                continue;
            }
            checkMutualExclusion(diskAO);
        }
    }

    public void checkMutualExclusion(APICreateVmInstanceMsg.DiskAO diskAO) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("size", diskAO.getSize() > 0);
        map.put("templateUuid", diskAO.getTemplateUuid() != null);
        map.put("diskOfferingUuid", diskAO.getDiskOfferingUuid() != null);
        map.put("sourceUuid", diskAO.getSourceUuid() != null);
        int count = 0;
        StringBuilder errorMsg = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue()) {
                count++;
                errorMsg.append(entry.getKey()).append(", ");
            }
        }

        if (count > 1) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10196, "Cannot set the following properties at the same time: %s", errorMsg));
        }

        if (count == 0) {
            StringJoiner properties = new StringJoiner(", ");
            for (String key : map.keySet()) {
                properties.add(key);
            }
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10197, "Need to set one of the following properties, and can only be one of them: %s", properties));
        }
    }

    private void validate(APICreateVmInstanceFromVolumeMsg msg) {
        validate((NewVmInstanceMessage2) msg);

        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume.isShareable()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10198, "cannot create vm instance from a shareable volume."));
        }

        if (volume.isAttached()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10199, "could not create vm instance from a attached volume."));
        }

        if (volume.getStatus() != VolumeStatus.Ready || volume.getState() != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10200, "volume[uuid:%s] could not satisfy conditions[state:Enabled status:Ready]", msg.getVolumeUuid()));
        }
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        validate((NewVmInstanceMessage2) msg);
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        validate((NewVmInstanceMessage2) msg);
    }

    private void validate(NewVmInstanceMessage2 msg) {
        VmInstanceUtils.validateInstanceSettings(msg);

        boolean uniqueVmName = VmGlobalConfig.UNIQUE_VM_NAME.value(Boolean.class);
        if (uniqueVmName && Q.New(VmInstanceVO.class).eq(VmInstanceVO_.name, msg.getName()).isExists()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10201, "could not create vm, a vm with the name [%s] already exists",
                    msg.getName()));
        }

        Set<String> macs = new HashSet<>();
        if (null != msg.getSystemTags()) {
            Optional<String> duplicateMac = msg.getSystemTags().stream()
                    .filter(t -> VmSystemTags.CUSTOM_MAC.isMatch(t))
                    .map(t -> t.split("::")[2].toLowerCase())
                    .filter(t -> !macs.add(t))
                    .findAny();
            if (duplicateMac.isPresent()){
                throw new ApiMessageInterceptionException(operr(
                ORG_ZSTACK_COMPUTE_VM_10202,         "Not allowed same mac [%s]", duplicateMac.get()));
            }
        }

        if (msg.getVmNicParams() != null && !msg.getVmNicParams().isEmpty()) {
            List<String> supportNicDriverTypes = nicManager.getSupportNicDriverTypes();
            if (msg.getL3NetworkUuids() == null || msg.getL3NetworkUuids().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10203, "l3NetworkUuids and vmNicInventories mustn't both be empty or both be set"));
            }

            List<VmNicParam> vmNicInventories;
            try {
                vmNicInventories = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new OperationFailureException(operr(ORG_ZSTACK_COMPUTE_VM_10204, "invalid json format, causes: %s", e.getMessage()));
            }

            VmNicUtils.validateVmParms(vmNicInventories, msg.getL3NetworkUuids(), supportNicDriverTypes);
        }

        if (!CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
            SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
            l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state);
            List<String> uuids = new ArrayList<>(msg.getL3NetworkUuids());
            List<String> duplicateElements = getDuplicateElementsOfList(uuids);
            if (duplicateElements.size() > 0) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10205, "Can't add same uuid in the l3Network,uuid: %s", duplicateElements.get(0)));
            }

            l3q.add(L3NetworkVO_.uuid, Op.IN, msg.getL3NetworkUuids());
            List<Tuple> l3ts = l3q.listTuple();
            for (Tuple t : l3ts) {
                String l3Uuid = t.get(0, String.class);
                Boolean system = t.get(1, Boolean.class);
                L3NetworkState state = t.get(2, L3NetworkState.class);
                if (state != L3NetworkState.Enabled) {
                    throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10206, "l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid));
                }
                if (system && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10207, "l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid));
                }
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
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10208, "zone[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getZoneUuid()));
            }
        }

        if (msg.getClusterUuid() != null) {
            SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
            cq.select(ClusterVO_.state);
            cq.add(ClusterVO_.uuid, Op.EQ, msg.getClusterUuid());
            ClusterState clusterState = cq.findValue();
            if (clusterState == ClusterState.Disabled) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10209, "cluster[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getClusterUuid()));
            }
        }

        if (msg.getHostUuid() != null) {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.state, HostVO_.status);
            hq.add(HostVO_.uuid, Op.EQ, msg.getHostUuid());
            Tuple t = hq.findTuple();
            HostState hostState = t.get(0, HostState.class);
            if (hostState == HostState.Disabled) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10210, "host[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getHostUuid()));
            }

            HostStatus connectionState = t.get(1, HostStatus.class);
            if (connectionState != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_VM_10211, "host[uuid:%s] is specified but it's connection status is %s, can not create vm from it", msg.getHostUuid(), connectionState));
            }
        }

        if (msg.getType() == null) {
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
        }

        if (VmInstanceConstant.USER_VM_TYPE.equals(msg.getType())) {
            if (msg.getDefaultL3NetworkUuid() == null && (!CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && msg.getL3NetworkUuids().size() != 1)) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10212, "there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null"));
            } else if (msg.getDefaultL3NetworkUuid() == null && (msg.getL3NetworkUuids()!= null &&msg.getL3NetworkUuids().size() == 1)) {
                msg.setDefaultL3NetworkUuid(msg.getL3NetworkUuids().get(0));
            } else if (msg.getDefaultL3NetworkUuid() != null && !msg.getL3NetworkUuids().contains(msg.getDefaultL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10213, "defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids%s", msg.getDefaultL3NetworkUuid(), msg.getL3NetworkUuids()));
            }
        }

        validateCdRomsTag(msg);
        validateZoneOrClusterOrHostOrL3Exist(msg);
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
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10214, "The image[uuid=%s] does not exist", cdRomIsoUuid));
            }
        }

        if (cdRoms.size() != new HashSet<>(cdRoms).size()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10215, "Do not allow to mount duplicate ISO"));
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
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10216, "The console password cannot start with 'password' which may trigger a VNC security issue"));
        }
    }

    private void validate(APIUpdateConsolePasswordMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr(
            ORG_ZSTACK_COMPUTE_VM_10217,         "Cannot update console password for VM[uuid:%s] because it is not in 'Running' state. Current state is '%s'.",
                    vm.getUuid(), vm.getState()
            ));
        }
        boolean hasPassword = VmSystemTags.CONSOLE_PASSWORD.hasTag(vm.getUuid());
        if (!hasPassword) {
            throw new ApiMessageInterceptionException(operr(
            ORG_ZSTACK_COMPUTE_VM_10218,         "Cannot update the console password for VM[uuid:%s] because no console password is currently set. ",
                    vm.getUuid()
            ));
        }
    }

    private void validate(APIAttachL3NetworkToVmNicMsg msg) {
        throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10219, "can not call this api because it's Deprecated"));
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
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_VM_10220, "The CdRom[%s] Already the default", vmCdRomVO.getUuid()));
        }
    }

    private void validate(APIFstrimVmMsg msg) {
        Tuple t = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid())
                .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                .findTuple();
        VmInstanceState state = t.get(0, VmInstanceState.class);

        if (state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr(
            ORG_ZSTACK_COMPUTE_VM_10221,         "vm[uuid:%s] can only fstrim when state is Running, current state is %s", msg.getUuid(), state));
        }
        msg.setHostUuid(t.get(1, String.class));
    }
}