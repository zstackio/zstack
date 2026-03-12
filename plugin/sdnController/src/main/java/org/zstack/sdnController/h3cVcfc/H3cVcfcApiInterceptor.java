package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.APIDetachL2NetworkFromClusterMsg;
import org.zstack.header.network.l3.APIAddIpRangeByNetworkCidrMsg;
import org.zstack.header.network.l3.APIAddIpRangeMsg;
import org.zstack.header.network.l3.APIAddIpv6RangeByNetworkCidrMsg;
import org.zstack.header.network.l3.APIAddIpv6RangeMsg;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerStatus;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.l2.L2NetworkSystemTags;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIDeleteVxlanL2Network;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVniRangeMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.network.l3.L3NetworkHelper;
import org.zstack.sdnController.SdnControllerL2;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;
import org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkMsg;
import org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkPoolMsg;
import org.zstack.sdnController.header.APIRemoveSdnControllerMsg;
import org.zstack.sdnController.header.H3cSdnControllerTenantVO;
import org.zstack.sdnController.header.H3cSdnControllerTenantVO_;
import org.zstack.sdnController.header.SdnVniRange;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class H3cVcfcApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(H3cVcfcApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    SdnControllerManager sdnControllerManager;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof SdnControllerMessage) {
            SdnControllerMessage smsg = (SdnControllerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SdnControllerConstant.SERVICE_ID, smsg.getSdnControllerUuid());
        }
    }

    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIAddSdnControllerMsg.class);
        ret.add(APICreateL2HardwareVxlanNetworkPoolMsg.class);
        ret.add(APICreateVniRangeMsg.class);
        ret.add(APICreateL2VxlanNetworkMsg.class);
        ret.add(APICreateL2HardwareVxlanNetworkMsg.class);
        ret.add(APIAttachL2NetworkToClusterMsg.class);
        ret.add(APIDetachL2NetworkFromClusterMsg.class);
        ret.add(APIDeleteVxlanL2Network.class);
        ret.add(APIRemoveSdnControllerMsg.class);
        ret.add(APICreateL3NetworkMsg.class);
        ret.add(APIAddIpRangeMsg.class);
        ret.add(APIAddIpRangeByNetworkCidrMsg.class);
        ret.add(APIAddIpv6RangeMsg.class);
        ret.add(APIAddIpv6RangeByNetworkCidrMsg.class);
        return ret;
    }

    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSdnControllerMsg) {
            validate((APIAddSdnControllerMsg) msg);
        } else if (msg instanceof APICreateL2HardwareVxlanNetworkPoolMsg){
            validate((APICreateL2HardwareVxlanNetworkPoolMsg)msg);
        } else if (msg instanceof APICreateVniRangeMsg){
            validate((APICreateVniRangeMsg)msg);
        } else if (msg instanceof APICreateL2VxlanNetworkMsg){
            validate((APICreateL2VxlanNetworkMsg)msg);
        } else if (msg instanceof APICreateL2HardwareVxlanNetworkMsg) {
            validate((APICreateL2HardwareVxlanNetworkMsg)msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            validate((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDeleteVxlanL2Network) {
            validate((APIDeleteVxlanL2Network) msg);
        } else if (msg instanceof APIRemoveSdnControllerMsg) {
            validate((APIRemoveSdnControllerMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            validate((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            validate((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            validate((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            validate((APIAddIpv6RangeByNetworkCidrMsg) msg);
        }
        setServiceId(msg);

        return msg;
    }

    public static boolean isOverlappedVniRange(Integer startVni1, Integer endVni1, Integer startVni2, Integer endVni2) {
        if (startVni2 <= startVni1 && endVni1 <= endVni2) {
            return true;
        }
        return false;
    }

    private void validate(APICreateVniRangeMsg msg) {
        VxlanNetworkPoolVO pool = dbf.findByUuid(msg.getL2NetworkUuid(), VxlanNetworkPoolVO.class);
        if ( pool == null ) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10015, "Could not create VNI range because the specified L2 network [uuid:%s] is not a VXLAN network pool", msg.getL2NetworkUuid()));
        }

        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(msg.getL2NetworkUuid(), HardwareL2VxlanNetworkPoolVO.class);
        if (poolVO == null) {
            return;
        }

        SdnControllerVO vo = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        if (!vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }

        // user's vni must <= 4094
        if (msg.getStartVni() > 4094 || msg.getEndVni() > 4094) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10016, "Could not create VNI range [%s-%s] because H3C controllers use VNI as VLAN ID and the range must be within 1-4094", msg.getStartVni(), msg.getEndVni()));
        }

        SdnControllerL2 sdnController = sdnControllerManager.getSdnControllerL2(vo);
        SdnVniRange userVniRange = new SdnVniRange();
        userVniRange.startVni = msg.getStartVni();
        userVniRange.endVni = msg.getEndVni();

        // user's vniRange must respectively covered by a sdn's vniRange
        List <SdnVniRange> legalList = sdnController.getVniRange(SdnControllerInventory.valueOf(vo));
        for (SdnVniRange legalRange : legalList) {
            if (isOverlappedVniRange(userVniRange.startVni, userVniRange.endVni, legalRange.startVni, legalRange.endVni)) {
                return;
            }
        }
        throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10017, "Could not create VNI range [%s-%s] because it is not covered by any of the SDN controller's configured VNI ranges", userVniRange.startVni, userVniRange.endVni));
    }

    private void validate(APIAddIpv6RangeMsg msg) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        if (sdnControllerUuid == null) {
            return;
        }
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (vo == null) {
            return;
        }
        if (!vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }
        if (vo.getStatus() != SdnControllerStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10018, "Could not add IPv6 range because the SDN controller [uuid:%s] is not connected. Current status: %s", 
                    sdnControllerUuid, vo.getStatus()));
        }
    }

    private void validate(APIAddIpv6RangeByNetworkCidrMsg msg) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        if (sdnControllerUuid == null) {
            return;
        }
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (vo == null) {
            return;
        }
        if (!vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }
        if (vo.getStatus() != SdnControllerStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10019, "Could not add IPv6 range by network CIDR because the SDN controller [uuid:%s] is not connected. Current status: %s", 
                    sdnControllerUuid, vo.getStatus()));
        }
    }

    private void validate(APIAddIpRangeMsg msg) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        if (sdnControllerUuid == null) {
            return;
        }
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (vo == null) {
            return;
        }
        if (!vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }
        if (vo.getStatus() != SdnControllerStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10020, "Could not add IP range because the SDN controller [uuid:%s] is not connected. Current status: %s", 
                    sdnControllerUuid, vo.getStatus()));
        }
    }

    private void validate(APIAddIpRangeByNetworkCidrMsg msg) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(msg.getL3NetworkUuid());
        if (sdnControllerUuid == null) {
            return;
        }
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (vo == null) {
            return;
        }
        if (!vo.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }
        if (vo.getStatus() != SdnControllerStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10021, "Could not add IP range by network CIDR because the SDN controller [uuid:%s] is not connected. Current status: %s", 
                    sdnControllerUuid, vo.getStatus()));
        }
    }

    private void validate(APICreateL3NetworkMsg msg) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL2Uuid(msg.getL2NetworkUuid());
        if (sdnControllerUuid == null) {
            return;
        }
        SdnControllerVO vo = dbf.findByUuid(sdnControllerUuid, SdnControllerVO.class);
        if (SdnControllerConstant.H3C_VCFC_CONTROLLER.equals(vo.getVendorType()) &&
                SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2.equals(vo.getVendorVersion()) &&
                L3NetworkCategory.Public.toString().equals(msg.getCategory())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10022, "can not create l3 network" +
                    "because H3C VCFC V2 SDN controller does not support l3[type:%s, category:%s]", msg.getType(), msg.getCategory()));
        }
    }

    private void validate(APIRemoveSdnControllerMsg msg) {
    }

    private void validate(APIDeleteVxlanL2Network msg) {
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
    }

    private void validate(APICreateL2HardwareVxlanNetworkMsg msg) {
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(msg.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        if (poolVO == null) {
            return;
        }

        SdnControllerVO sdnControllerVO = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        if (sdnControllerVO != null
                && SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2.equals(sdnControllerVO.getVendorVersion())) {
            boolean tenantExist = msg.getH3cTenantUuid() != null &&
                    Q.New(H3cSdnControllerTenantVO.class)
                            .eq(H3cSdnControllerTenantVO_.uuid, msg.getH3cTenantUuid())
                            .isExists();
            if (!tenantExist) {
                throw new ApiMessageInterceptionException(argerr(
                ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10023,         "Could not create hardware VXLAN network because tenant UUID is a mandatory parameter for the H3C VCFC V2 controller"));
            }
        }

        boolean hasSdnControllerTag = msg.getSystemTags() != null &&
                msg.getSystemTags().stream()
                        .anyMatch(L2NetworkSystemTags.L2_NETWORK_SDN_CONTROLLER_UUID::isMatch);

        if (!hasSdnControllerTag && poolVO.getSdnControllerUuid() != null) {
            if (msg.getSystemTags() == null) {
                msg.setSystemTags(new ArrayList<>());
            }
            String tag = L2NetworkSystemTags.L2_NETWORK_SDN_CONTROLLER_UUID.instantiateTag(
                    map(e(L2NetworkSystemTags.L2_NETWORK_SDN_CONTROLLER_UUID_TOKEN, poolVO.getSdnControllerUuid()))
            );
            msg.getSystemTags().add(tag);
        }
    }

    private void validate(APICreateL2HardwareVxlanNetworkPoolMsg msg) {
    }

    private void validate(APICreateL2VxlanNetworkMsg msg) {
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
    }

    private boolean validateH3cController(APIAddSdnControllerMsg msg) {
        if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return false;
        }

        boolean vds = false;
        for (String tag : msg.getSystemTags()) {
            if (H3cVcfcSdnControllerSystemTags.H3C_VDS_UUID.isMatch(tag)){
                vds = true;
            }
        }
        return vds;
    }

    private void validate(APIAddSdnControllerMsg msg) {
        if (!msg.getVendorType().equals(SdnControllerConstant.H3C_VCFC_CONTROLLER)) {
            return;
        }
        if (!validateH3cController(msg) && msg.getVendorVersion().equals(SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V1)) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_SDNCONTROLLER_H3CVCFC_10024, "Could not add H3C VCFC controller because VDS UUID system tag is required for H3C VCFC V1 controllers"));
        }
    }
}
