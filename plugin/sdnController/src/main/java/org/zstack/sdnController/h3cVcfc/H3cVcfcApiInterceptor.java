package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.APIDetachL2NetworkFromClusterMsg;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIDeleteVxlanL2Network;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVniRangeMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;

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
            throw new ApiMessageInterceptionException(argerr("unable create vni range, because l2 uuid[%s] is not vxlan network pool",msg.getL2NetworkUuid()));
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
            throw new ApiMessageInterceptionException(argerr("the vni range:[%s.%s} is illegal, because h3c's controller uses vni as vlan id", msg.getStartVni(), msg.getEndVni()));
        }
        
        SdnController sdnController = sdnControllerManager.getSdnController(vo);
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
        throw new ApiMessageInterceptionException(argerr("the vni range:[%s.%s} is illegal, must covered by a sdn's vniRange", userVniRange.startVni, userVniRange.endVni));
    }

    private void validate(APICreateL3NetworkMsg msg) {
    }

    private void validate(APIRemoveSdnControllerMsg msg) {
    }

    private void validate(APIDeleteVxlanL2Network msg) {
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
    }

    private void validate(APICreateL2HardwareVxlanNetworkMsg msg) {
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
        if (!validateH3cController(msg)) {
            throw new ApiMessageInterceptionException(argerr("H3C VCFC controller must include systemTags vdsUuid::{%s}"));
        }
    }
}
