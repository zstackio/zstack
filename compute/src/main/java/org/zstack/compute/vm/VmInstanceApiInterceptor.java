package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
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
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.vm.*;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

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
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            validate((APIGetVmAttachableDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            validate((APIDetachL3NetworkFromVmMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIAttachIsoToVmInstanceMsg) {
            validate((APIAttachIsoToVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmBootOrderMsg) {
            validate((APISetVmBootOrderMsg) msg);
        } else if (msg instanceof APIDeleteVmStaticIpMsg) {
            validate((APIDeleteVmStaticIpMsg) msg);
        } else if (msg instanceof APISetVmStaticIpMsg) {
            validate((APISetVmStaticIpMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            validate((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksImagesMsg) {
            validate((APIGetInterdependentL3NetworksImagesMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMsg) {
            validate((APIUpdateVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmConsolePasswordMsg) {
            validate((APISetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIChangeInstanceOfferingMsg) {
            validate((APIChangeInstanceOfferingMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIMigrateVmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                if (vo.getHostUuid().equals(msg.getHostUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the vm[uuid:%s] is already on host[uuid:%s]", msg.getVmInstanceUuid(), msg.getHostUuid()
                    ));
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

                if (!VmGlobalConfig.NUMA.value(Boolean.class) && !VmInstanceState.Stopped.equals(vo.getState())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the VM cannot do online cpu/memory update because it is not of NUMA architecture. Please stop the VM then do the cpu/memory update again"
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
                if (!VmGlobalConfig.NUMA.value(Boolean.class) && !VmInstanceState.Stopped.equals(vmState)) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the VM cannot do online cpu/memory update because it is not of NUMA architecture. Please stop the VM then do the cpu/memory update again"
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

    private void validate(APISetVmStaticIpMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid IPv4 address", msg.getIp()));
        }

        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
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

    private void validate(APIAttachIsoToVmInstanceMsg msg) {
        String isoUuid = new IsoOperator().getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if (isoUuid != null) {
            throw new ApiMessageInterceptionException(operr("VM[uuid:%s] already has an ISO[uuid:%s] attached", msg.getVmInstanceUuid(), isoUuid));
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

        SimpleQuery<VmNicVO> nq = dbf.createQuery(VmNicVO.class);
        nq.add(VmNicVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        nq.add(VmNicVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        if (nq.isExists()) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                            msg.getL3NetworkUuid(), msg.getVmInstanceUuid()));
        }

        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.select(L3NetworkVO_.state, L3NetworkVO_.system);
        l3q.add(L3NetworkVO_.uuid, Op.EQ, msg.getL3NetworkUuid());
        t = l3q.findTuple();
        L3NetworkState l3state = t.get(0, L3NetworkState.class);
        boolean system = t.get(1, Boolean.class);
        if (l3state == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is disabled", msg.getL3NetworkUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && system) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is a system network", msg.getL3NetworkUuid()));
        } else if (!VmInstanceConstant.USER_VM_TYPE.equals(type) && !system) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The vm[uuid: %s] is not a user vm", type));
        }

        if (msg.getStaticIp() != null) {
            SimpleQuery<IpRangeVO> iprq = dbf.createQuery(IpRangeVO.class);
            iprq.add(IpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            List<IpRangeVO> iprs = iprq.list();

            boolean found = false;
            for (IpRangeVO ipr : iprs) {
                if (NetworkUtils.isIpv4InRange(msg.getStaticIp(), ipr.getStartIp(), ipr.getEndIp())) {
                    found = true;
                    break;
                }
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
    }

    @Transactional(readOnly = true)
    private void validate(APIDetachL3NetworkFromVmMsg msg) {
        String sql = "select vm.uuid, vm.type, vm.state from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getVmNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        String vmType = t.get(1, String.class);
        VmInstanceState state = t.get(2, VmInstanceState.class);

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to detach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                            msg.getVmInstanceUuid(), state));
        }

        msg.setVmInstanceUuid(vmUuid);
    }

    private static <T> List<T> getDuplicateElements(List<T> list) {
        List<T> result = new ArrayList<T>();
        Set<T> set = new HashSet<T>();
        for (T e : list) {
            if (!set.add(e)) {
                result.add(e);
            }
        }
        return result;
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

    private void validate(APICreateVmInstanceMsg msg) {
        SimpleQuery<InstanceOfferingVO> iq = dbf.createQuery(InstanceOfferingVO.class);
        iq.select(InstanceOfferingVO_.state, InstanceOfferingVO_.type);
        iq.add(InstanceOfferingVO_.uuid, Op.EQ, msg.getInstanceOfferingUuid());
        Tuple inst = iq.findTuple();
        InstanceOfferingState istate = inst.get(0, InstanceOfferingState.class);
        String itype = inst.get(1, String.class);
        if (istate == InstanceOfferingState.Disabled) {
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s] is Disabled, can't create vm from it", msg.getInstanceOfferingUuid()));
        }
        if (!itype.equals(VmInstanceConstant.USER_VM_TYPE)){
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s, type:%s] is not UserVm type, can't create vm from it", msg.getInstanceOfferingUuid(), itype));
        }

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.state, ImageVO_.system, ImageVO_.mediaType);
        imgq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        Tuple imgt = imgq.findTuple();
        ImageState imgState = imgt.get(0, ImageState.class);
        if (imgState == ImageState.Disabled) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is Disabled, can't create vm from it", msg.getImageUuid()));
        }


        ImageMediaType imgFormat = imgt.get(2, ImageMediaType.class);
        if (imgFormat != ImageMediaType.RootVolumeTemplate && imgFormat != ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", msg.getImageUuid(), imgFormat));
        }

        if (imgFormat == ImageMediaType.ISO && msg.getRootDiskOfferingUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("rootDiskOfferingUuid cannot be null when image mediaType is ISO"));
        }

        boolean isSystemImage = imgt.get(1, Boolean.class);
        if (isSystemImage && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is system image, can't be used to create user vm", msg.getImageUuid()));
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
                throw new ApiMessageInterceptionException(operr("disk offerings[uuids:%s] are Disabled, can not create vm from it", diskUuids));
            }
        }

        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state);
        List<String> uuids = new ArrayList<>(msg.getL3NetworkUuids());
        List<String> duplicateElements = getDuplicateElements(uuids);
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
}
