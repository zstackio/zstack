package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by weiwang on 02/05/2017.
 */
public class VxlanPoolApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VxlanNetworkPool.class);
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVniRangeMsg) {
            validate((APICreateVniRangeMsg) msg);
        } else if (msg instanceof APICreateL2VxlanNetworkMsg) {
            validate((APICreateL2VxlanNetworkMsg) msg);
        } else if (msg instanceof  APIDeleteVniRangeMsg) {
            validate((APIDeleteVniRangeMsg) msg);
        }
        return msg;
    }

    private void validate(APIDeleteVniRangeMsg msg) {
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();
        msg.setL2NetworkUuid(vo.getL2NetworkUuid());
    }

    private void validate(APICreateL2VxlanNetworkMsg msg) {
        VxlanNetworkPoolVO vo = Q.New(VxlanNetworkPoolVO.class).eq(VxlanNetworkPoolVO_.uuid, msg.getPoolUuid()).find();
        if (msg.getZoneUuid() != null && !msg.getZoneUuid().equals(vo.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("the zone uuid provided not equals to zone uuid of pool [%s], please correct it or do not fill it",
                            msg.getPoolUuid())
            ));
        } else if (msg.getZoneUuid() == null ) {
            msg.setZoneUuid(vo.getZoneUuid());
        }
    }

    private void validate(APICreateVniRangeMsg msg) {
        if (msg.getStartVni() > msg.getEndVni()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("start number [%s] of vni range is bigger than end number [%s]",
                            msg.getStartVni(), msg.getStartVni())
            ));
        }

        List<VniRangeVO> exists = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, msg.getL2NetworkUuid()).list();
        for (VniRangeVO e : exists) {
            if (checkOverlap(msg.getStartVni(), msg.getEndVni(), e.getStartVni(), e.getEndVni()) == true) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("this vni range has overlapped with vni range [%s], which start vni is [%s], end vni is [%s]",
                                e.getUuid(), e.getStartVni(), e.getEndVni())
                ));
            }
        }
    }

    private boolean checkOverlap(Integer checktart, Integer checkEnd, Integer existStart, Integer existEnd){
        if ((existStart <= checktart) && (checktart <= existEnd)) {
            return true;
        } else if ((checktart <= existStart) && (existStart <= existEnd)) {
            return true;
        }
        return false;
    }
}
