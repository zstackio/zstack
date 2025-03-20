package org.zstack.compute.vm;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.InstanceOfferingState;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.getDuplicateElementsOfList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstanceHelper {
    private static final CLogger logger = Utils.getLogger(VmInstanceHelper.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmNicManager nicManager;

    public void validate(NewVmInstanceMessage2 msg) {
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

        if (!CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
            SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
            l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state, L3NetworkVO_.l2NetworkUuid);
            List<String> uuids = new ArrayList<>(msg.getL3NetworkUuids());
            List<String> duplicateElements = getDuplicateElementsOfList(uuids);
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class) && !duplicateElements.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("Can't add same uuid in the l3Network,uuid: %s", duplicateElements.get(0)));
            }

            l3q.add(L3NetworkVO_.uuid, SimpleQuery.Op.IN, msg.getL3NetworkUuids());
            List<Tuple> l3ts = l3q.listTuple();
            for (Tuple t : l3ts) {
                String l3Uuid = t.get(0, String.class);
                Boolean system = t.get(1, Boolean.class);
                L3NetworkState state = t.get(2, L3NetworkState.class);
                String l2Uuid = t.get(3, String.class);
                if (state != L3NetworkState.Enabled) {
                    throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid));
                }
                if (system && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                    throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid));
                }
                L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();
                if (l2NetworkVO.getAttachedClusterRefs() == null || l2NetworkVO.getAttachedClusterRefs().isEmpty()) {
                    throw new ApiMessageInterceptionException(argerr(String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster",
                            l2NetworkVO.getUuid(), l3Uuid)));
                } else if (msg.getClusterUuid() != null && l2NetworkVO.getAttachedClusterRefs().stream().noneMatch(c -> c.getClusterUuid().equals(msg.getClusterUuid()))) {
                    throw new ApiMessageInterceptionException(argerr(String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster[uuid: %s]",
                            l2NetworkVO.getUuid(), l3Uuid, msg.getClusterUuid())));
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
            zq.add(ZoneVO_.uuid, SimpleQuery.Op.EQ, msg.getZoneUuid());
            ZoneState zoneState = zq.findValue();
            if (zoneState == ZoneState.Disabled) {
                throw new ApiMessageInterceptionException(operr("zone[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getZoneUuid()));
            }
        }

        if (msg.getClusterUuid() != null) {
            SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
            cq.select(ClusterVO_.state);
            cq.add(ClusterVO_.uuid, SimpleQuery.Op.EQ, msg.getClusterUuid());
            ClusterState clusterState = cq.findValue();
            if (clusterState == ClusterState.Disabled) {
                throw new ApiMessageInterceptionException(operr("cluster[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getClusterUuid()));
            }
        }

        if (msg.getHostUuid() != null) {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.state, HostVO_.status);
            hq.add(HostVO_.uuid, SimpleQuery.Op.EQ, msg.getHostUuid());
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
            if (msg.getDefaultL3NetworkUuid() == null && (!CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && msg.getL3NetworkUuids().size() != 1)) {
                throw new ApiMessageInterceptionException(argerr("there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null"));
            } else if (msg.getDefaultL3NetworkUuid() == null && (msg.getL3NetworkUuids()!= null &&msg.getL3NetworkUuids().size() == 1)) {
                msg.setDefaultL3NetworkUuid(msg.getL3NetworkUuids().get(0));
            } else if (msg.getDefaultL3NetworkUuid() != null && !msg.getL3NetworkUuids().contains(msg.getDefaultL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr("defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids%s", msg.getDefaultL3NetworkUuid(), msg.getL3NetworkUuids()));
            }
        }

        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            if (CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuids and vmNicInventories mustn't both be empty or both be set"));
            }

            List<VmNicParam> vmNicParams;
            try {
                vmNicParams = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new OperationFailureException(operr("invalid json format, causes: %s", e.getMessage()));
            }

            new VmNicParamValidator().withVmNicParams(vmNicParams)
                    .withL3Uuids(msg.getL3NetworkUuids())
                    .withDefaultL3Uuid(msg.getDefaultL3NetworkUuid())
                    .withSupportNicDriverTypes(nicManager.getSupportNicDriverTypes())
                    .withVmType(msg.getType())
                    .isWindowsVm(ImagePlatform.Windows.toString().equals(msg.getPlatform()))
                    .validate();
        }

        validateCdRomsTag(msg);
        validateZoneOrClusterOrHostOrL3Exist(msg);
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

            if (msg.getReservedMemorySize() != null) {
                if (msg.getReservedMemorySize() > msg.getMemorySize()) {
                    throw new ApiMessageInterceptionException(operr("reserved memory[%s] is greater than memory size[%s]", msg.getReservedMemorySize(), msg.getMemorySize()));
                }
            } else {
                msg.setReservedMemorySize(0L);
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
        // reserved memory should support customize
        if (msg.getReservedMemorySize() == null) {
            msg.setReservedMemorySize(ivo.getReservedMemorySize());
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
                throw new ApiMessageInterceptionException(argerr("The image[uuid:%s] does not exist", cdRomIsoUuid));
            }
        }

        if (cdRoms.size() != new HashSet<>(cdRoms).size()) {
            throw new ApiMessageInterceptionException(argerr("Do not allow to mount duplicate ISO"));
        }
    }

    private void validateZoneOrClusterOrHostOrL3Exist(NewVmInstanceMessage2 msg) {
        if (CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && StringUtils.isEmpty(msg.getZoneUuid())
                && StringUtils.isEmpty(msg.getClusterUuid()) && StringUtils.isEmpty(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr("could not create vm, because at least one of field (l3NetworkUuids,zoneUuid,clusterUuid,hostUuid) should be set"));
        }
    }
}
