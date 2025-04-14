package org.zstack.sdnController;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.APICreateL2NoVlanNetworkMsg;
import org.zstack.header.network.l2.APICreateL2VlanNetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.vm.APIAttachL3NetworkToVmMsg;
import org.zstack.header.vm.APIChangeVmNicNetworkMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.network.l3.L3NetworkHelper;
import org.zstack.network.securitygroup.*;
import org.zstack.sdnController.header.*;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

/**
 * Created by shixin.ruan on 09/17/2019
 */
public class SdnControllerApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(SdnControllerApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof SdnControllerMessage) {
            SdnControllerMessage smsg = (SdnControllerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SdnControllerConstant.SERVICE_ID, smsg.getSdnControllerUuid());
        }
    }

    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIAttachSecurityGroupToL3NetworkMsg.class);
        ret.add(APIAddVmNicToSecurityGroupMsg.class);
        ret.add(APISetVmNicSecurityGroupMsg.class);
        ret.add(APIAddSecurityGroupRuleMsg.class);
        ret.add(APIAttachL3NetworkToVmMsg.class);
        ret.add(APIChangeVmNicNetworkMsg.class);

        return ret;
    }

    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSdnControllerMsg) {
            validate((APIAddSdnControllerMsg)msg);
        } else if (msg instanceof APISdnControllerAddHostMsg) {
            validate((APISdnControllerAddHostMsg)msg);
        } else if (msg instanceof APISdnControllerRemoveHostMsg) {
            validate((APISdnControllerRemoveHostMsg)msg);
        } else if (msg instanceof APISdnControllerChangeHostMsg) {
            validate((APISdnControllerChangeHostMsg)msg);
        } else if (msg instanceof APIAttachSecurityGroupToL3NetworkMsg) {
            validate((APIAttachSecurityGroupToL3NetworkMsg)msg);
        } else if (msg instanceof APIAddVmNicToSecurityGroupMsg) {
            validate((APIAddVmNicToSecurityGroupMsg) msg);
        } else if (msg instanceof APISetVmNicSecurityGroupMsg) {
            validate((APISetVmNicSecurityGroupMsg) msg);
        } else if (msg instanceof APIAddSecurityGroupRuleMsg) {
            validate((APIAddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicNetworkMsg) {
            validate((APIChangeVmNicNetworkMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        String sdnControlerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        if (sdnControlerUuid == null) {
            return;
        }

        SdnControllerVO controllerVO = dbf.findByUuid(sdnControlerUuid, SdnControllerVO.class);
        if (controllerVO == null) {
            throw new ApiMessageInterceptionException(argerr("could not attach l3network to vm, " +
                            "because sdn controller[uuid:%s] is not find", sdnControlerUuid));
        }

        VmInstanceVO vmVo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        boolean found = false;
        for (SdnControllerHostRefVO ref : controllerVO.getHostRefVOS()) {
            if (ref.getHostUuid().equals(vmVo.getHostUuid())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ApiMessageInterceptionException(argerr("could not attach l3network to vm, " +
                    "because host[uuid:%s] of vm is not attached to sdn controller[uuid:%s]",
                    vmVo.getHostUuid(), sdnControlerUuid));
        }
    }

    private void validate(APIChangeVmNicNetworkMsg msg) {
        String sdnControlerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getDestL3NetworkUuid());
        if (sdnControlerUuid == null) {
            return;
        }
        
        SdnControllerVO controllerVO = dbf.findByUuid(sdnControlerUuid, SdnControllerVO.class);
        if (controllerVO == null) {
            throw new ApiMessageInterceptionException(argerr("could not change vmnic to l3network[uuid:%s], " +
                    "because sdn controller[uuid:%s] is not find", msg.getDestL3NetworkUuid(), sdnControlerUuid));
        }

        VmInstanceVO vmVo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        boolean found = false;
        for (SdnControllerHostRefVO ref : controllerVO.getHostRefVOS()) {
            if (ref.getHostUuid().equals(vmVo.getHostUuid())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ApiMessageInterceptionException(argerr("could not change vmnic to l3network[uuid:%s], " +
                            "because host[uuid:%s] of vm is not attached to sdn controller[uuid:%s]",
                    msg.getDestL3NetworkUuid(), vmVo.getHostUuid(), sdnControlerUuid));
        }
    }

    private void validate(APISetVmNicSecurityGroupMsg msg) {
        VmNicVO nicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        String nicControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(nicVO.getL3NetworkUuid());
        for (APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO ref : msg.getRefs()) {
            String sgControllerUuid = SecurityGroupHelper.getSdnControllerUuid(ref.getSecurityGroupUuid());
            if (!StringUtils.equals(sgControllerUuid, nicControllerUuid)) {
                throw new ApiMessageInterceptionException(argerr("could not add vmnic to securityGroup, " +
                                "because they have different sdn controller[nic controller uuid:%s, security group controller uuid:%s]",
                        nicControllerUuid, sgControllerUuid));
            }
        }
    }

    private void validate(APIAddSecurityGroupRuleMsg msg) {
        String sgControllerUuid = SecurityGroupHelper.getSdnControllerUuid(msg.getSecurityGroupUuid());
        for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule : msg.getRules()) {
            if (rule.getRemoteSecurityGroupUuid() == null) {
                continue;
            }
            String remoteControllerUuid = SecurityGroupHelper.getSdnControllerUuid(rule.getRemoteSecurityGroupUuid());
            if (!StringUtils.equals(sgControllerUuid, remoteControllerUuid)) {
                throw new ApiMessageInterceptionException(argerr("could not add securityGroup rule, " +
                                "because rule remote security group sdn controller uuid[:%s] is different from security group controller uuid[:%s]",
                        remoteControllerUuid, sgControllerUuid));
            }
        }
    }

    private void validate(APIAddVmNicToSecurityGroupMsg msg) {
        String sgControllerUuid = SecurityGroupHelper.getSdnControllerUuid(msg.getSecurityGroupUuid());
        for (String uuid : msg.getVmNicUuids()) {
            VmNicVO nicVO = dbf.findByUuid(uuid, VmNicVO.class);
            String nicControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(nicVO.getL3NetworkUuid());
            if (!StringUtils.equals(sgControllerUuid, nicControllerUuid)) {
                throw new ApiMessageInterceptionException(argerr("could not add vmnic to securityGroup, " +
                                "because they have different sdn controller[nic controller uuid:%s, security group controller uuid:%s]",
                        nicControllerUuid, sgControllerUuid));
            }
        }
    }


    private void validate(APIAttachSecurityGroupToL3NetworkMsg msg) {
        String l3SdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        String sgSdnControllerUuid = SecurityGroupHelper.getSdnControllerUuid(msg.getSecurityGroupUuid());
        if (!StringUtils.equals(l3SdnControllerUuid, sgSdnControllerUuid)) {
            throw new ApiMessageInterceptionException(argerr("could not attach l3 network to securityGroup, " +
                    "because they have different sdn controller[l3 controller uuid:%s, security group controller uuid:%s]",
                    l3SdnControllerUuid, sgSdnControllerUuid));
        }
    }

    private void validate(APIAddSdnControllerMsg msg) {
        if (!SdnControllerType.getAllTypeNames().contains(msg.getVendorType())) {
            throw new ApiMessageInterceptionException(argerr("Sdn controller type: %s in not in the supported list: %s ", msg.getVendorType(), SdnControllerType.getAllTypeNames()));
        }

        if (!NetworkUtils.isUnicastIPAddress(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("Sdn controller ip[%s] is not an unicast address ", msg.getIp()));
        }

        boolean existed = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.ip, msg.getIp()).isExists();
        if (existed) {
            throw new ApiMessageInterceptionException(argerr("Sdn controller with ip [%s] is already added ", msg.getIp()));
        }
    }

    private void validate(APISdnControllerAddHostMsg msg) {
        if (Q.New(SdnControllerHostRefVO.class)
                .eq(SdnControllerHostRefVO_.vSwitchType, msg.getvSwitchType())
                .eq(SdnControllerHostRefVO_.hostUuid, msg.getHostUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could not add host[uuid:%s] to sdn controller[uuid:%s], " +
                            " because host already attached to sdn controller", msg.getHostUuid(), msg.getSdnControllerUuid()));
        }

        if (msg.getVtepIp() != null && msg.getNetmask() == null) {
            throw new ApiMessageInterceptionException(argerr("could not add host[uuid:%s] to sdn controller[uuid:%s], " +
                    " because netmask is not specified", msg.getHostUuid(), msg.getSdnControllerUuid()));
        }

        if (msg.getVtepIp() != null) {
            SdnControllerHostRefVO refvo = Q.New(SdnControllerHostRefVO.class)
                    .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                    .eq(SdnControllerHostRefVO_.vSwitchType, msg.getvSwitchType())
                    .eq(SdnControllerHostRefVO_.vtepIp, msg.getVtepIp()).find();
            if (refvo != null) {
                throw new ApiMessageInterceptionException(argerr("could not add host[uuid:%s] to sdn controller[uuid:%s], " +
                        " because vtepip is used by host[uuid:%s]", msg.getHostUuid(),
                        msg.getSdnControllerUuid(), refvo.getHostUuid()));
            }
        }

        if (msg.getNicNames().size() > 1 && msg.getBondMode() == null) {
            msg.setBondMode(L2NetworkConstant.BONDING_MODE_AB);
        }

        if (msg.getBondMode() != null && msg.getLacpMode() == null) {
            msg.setLacpMode(L2NetworkConstant.LACP_MODE_ACTIVE);
        }
    }

    private void validate(APISdnControllerRemoveHostMsg msg) {
        if (!Q.New(SdnControllerHostRefVO.class)
                .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                .eq(SdnControllerHostRefVO_.hostUuid, msg.getHostUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could not remove host[uuid:%s] from sdn controller[uuid:%s], " +
                    " because host has not been added to sdn controller", msg.getHostUuid(), msg.getSdnControllerUuid()));
        }
    }

    private void validate(APISdnControllerChangeHostMsg msg) {
        SdnControllerHostRefVO refVO = Q.New(SdnControllerHostRefVO.class)
                .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                .eq(SdnControllerHostRefVO_.hostUuid, msg.getHostUuid()).find();
        if (refVO == null) {
            throw new ApiMessageInterceptionException(argerr("could not change host[uuid:%s] of sdn controller[uuid:%s], " +
                    " because host has not been added to sdn controller", msg.getHostUuid(), msg.getSdnControllerUuid()));
        }

        if (msg.getVtepIp() != null && msg.getNetmask() == null) {
            throw new ApiMessageInterceptionException(argerr("could not change host[uuid:%s] of sdn controller[uuid:%s], " +
                    " because netmask is specified", msg.getHostUuid(), msg.getSdnControllerUuid()));
        }

        if (msg.getNicNames() == null) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> nicNamePciAddressMap = gson.fromJson(refVO.getNicPciAddresses(), type);
            msg.setNicNames(new ArrayList<>(nicNamePciAddressMap.keySet()));
        }

        if (msg.getNicNames().size() > 1 && msg.getBondMode() == null) {
            msg.setBondMode(refVO.getBondMode());
        }
    }

}
