package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.*;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class VmInstanceApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmInstanceMessage) {
            VmInstanceMessage vmsg = (VmInstanceMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDestroyVmInstanceMsg) {
            validate((APIDestroyVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg)msg);
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            validate((APIGetVmAttachableDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachNicFromVmMsg) {
            validate((APIDetachNicFromVmMsg) msg);
        } else if (msg instanceof APIAttachNicToVmMsg) {
            validate((APIAttachNicToVmMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIAttachNicToVmMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.type);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        String type = q.findValue();
        if (!VmInstanceConstant.USER_VM_TYPE.equals(type)) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("unable to attach a nic. The vm[uuid: %s] is not a user vm", type)
            ));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIDetachNicFromVmMsg msg) {
        String sql = "select vm.uuid, vm.type from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        String vmType = t.get(1, String.class);

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vmType)) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("unable to detach a nic. The vm[uuid: %s] is not a user vm", vmUuid)
            ));
        }

        msg.setVmInstanceUuid(vmUuid);
    }

    private void validate(APIGetVmAttachableDataVolumeMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (state != VmInstanceState.Stopped && state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("vm[uuid:%s] can only attach volume when state is Running or Stopped, current state is %s", msg.getVmInstanceUuid(), state)
            ));
        }
    }

    private void validate(APICreateVmInstanceMsg msg) {
        SimpleQuery<InstanceOfferingVO> iq = dbf.createQuery(InstanceOfferingVO.class);
        iq.select(InstanceOfferingVO_.state);
        iq.add(InstanceOfferingVO_.uuid, Op.EQ, msg.getInstanceOfferingUuid());
        InstanceOfferingState istate = iq.findValue();
        if (istate == InstanceOfferingState.Disabled) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("instance offering[uuid:%s] is Disabled, can't create vm from it", msg.getInstanceOfferingUuid())
            ));
        }

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.state, ImageVO_.system, ImageVO_.mediaType);
        imgq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        Tuple imgt = imgq.findTuple();
        ImageState imgState = imgt.get(0, ImageState.class);
        if (imgState == ImageState.Disabled) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("image[uuid:%s] is Disabled, can't create vm from it", msg.getImageUuid())
            ));
        }


        ImageMediaType imgFormat = imgt.get(2, ImageMediaType.class);
        if (imgFormat != ImageMediaType.RootVolumeTemplate && imgFormat != ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", msg.getImageUuid(), imgFormat)
            ));
        }

        if (imgFormat == ImageMediaType.ISO && msg.getRootDiskOfferingUuid() == null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("rootDiskOfferingUuid cannot be null when image mediaType is ISO")
            ));
        }

        boolean isSystemImage = imgt.get(1, Boolean.class);
        if (isSystemImage && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("image[uuid:%s] is system image, can't be used to create user vm", msg.getImageUuid())
            ));
        }


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
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("disk offerings[uuids:%s] are Disabled, can not create vm from it", diskUuids)
                ));
            }
        }

        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state);
        l3q.add(L3NetworkVO_.uuid, Op.IN, msg.getL3NetworkUuids());
        List<Tuple> l3ts = l3q.listTuple();
        for (Tuple t : l3ts) {
            String l3Uuid = t.get(0, String.class);
            Boolean system = t.get(1, Boolean.class);
            L3NetworkState state = t.get(2, L3NetworkState.class);
            if (state != L3NetworkState.Enabled) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid)
                ));
            }

            if (system && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid)
                ));
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
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("zone[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getZoneUuid())
                ));
            }
        }

        if (msg.getClusterUuid() != null) {
            SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
            cq.select(ClusterVO_.state);
            cq.add(ClusterVO_.uuid, Op.EQ, msg.getClusterUuid());
            ClusterState clusterState = cq.findValue();
            if (clusterState == ClusterState.Disabled) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("cluster[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getClusterUuid())
                ));
            }
        }

        if (msg.getHostUuid() != null) {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.state, HostVO_.status);
            hq.add(HostVO_.uuid, Op.EQ, msg.getHostUuid());
            Tuple t = hq.findTuple();
            HostState hostState = t.get(0, HostState.class);
            if (hostState == HostState.Disabled) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("host[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getHostUuid())
                ));
            }

            HostStatus connectionState = t.get(1, HostStatus.class);
            if (connectionState != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("host[uuid:%s] is specified but it's connection status is %s, can not create vm from it", msg.getHostUuid(), connectionState)
                ));
            }
        }

        if (msg.getType() == null) {
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
        }

        if (VmInstanceConstant.USER_VM_TYPE.equals(msg.getType())) {
            if (msg.getDefaultL3NetworkUuid() == null && msg.getL3NetworkUuids().size() != 1) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null")
                ));
            } else if (msg.getDefaultL3NetworkUuid() == null && msg.getL3NetworkUuids().size() == 1) {
                msg.setDefaultL3NetworkUuid(msg.getL3NetworkUuids().get(0));
            } else if (msg.getDefaultL3NetworkUuid() != null && !msg.getL3NetworkUuids().contains(msg.getDefaultL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids%s", msg.getDefaultL3NetworkUuid(), msg.getL3NetworkUuids())
                ));
            }
        }
    }

    private void validate(APIDestroyVmInstanceMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VmInstanceVO.class)) {
            APIDestroyVmInstanceEvent evt = new APIDestroyVmInstanceEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
}
