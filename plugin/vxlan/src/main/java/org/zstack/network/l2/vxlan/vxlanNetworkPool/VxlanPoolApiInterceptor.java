package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

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
        }
        return msg;
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
        if ((checktart >= existStart) && (checktart < existEnd)) {
            return true;
        } else if ((checkEnd > existStart) && (checkEnd <= existEnd)) {
            return true;
        }
        return false;
    }
}
