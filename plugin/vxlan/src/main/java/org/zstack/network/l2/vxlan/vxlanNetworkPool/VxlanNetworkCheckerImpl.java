package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by weiwang on 02/05/2017.
 */
public class VxlanNetworkCheckerImpl implements VxlanNetworkChecker {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        }

        return msg;
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).find();
        if (!l2NetworkVO.getType().equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
            return;
        }

        String pattern = ".*cidr::\\{.*\\}";

        if (msg.getSystemTags() == null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("need to input one system tag like : l2NetworkUuid::XXX::clusterUuid::XXX::cidr::{X.X.X.X/Y}")
            ));
        }

        for (String tag : msg.getSystemTags()) {
            if (!Pattern.matches(pattern, tag)) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("wrong system tag format, shoud be like : l2NetworkUuid::XXX::clusterUuid::XXX::cidr::{X.X.X.X/Y}")
                ));
            }

            StringTokenizer st = new StringTokenizer(tag, "::");
            while (st.hasMoreElements()) {
                String t = st.nextToken();
                if (Pattern.matches("\\{.*\\}", t)) {
                    if (!NetworkUtils.isCidr(t.replaceAll("\\{|\\}", ""))) {
                        throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                                String.format("wrong cidr format in system tag")
                        ));
                    }
                }
            }
        }

        String overlapedPool = getOverlapVniRangePool(L2NetworkInventory.valueOf(l2NetworkVO), msg.getClusterUuid());

        if (overlapedPool != null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("overlap vni range with vxlan network pool [%s]", overlapedPool)
            ));
        }

    }

    public String getOverlapVniRangePool(L2NetworkInventory inv, String clusterUuid) {
        List<VniRangeVO> checkRanges = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, inv.getUuid()).list();
        List<String> l2Uuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).notEq(L2NetworkClusterRefVO_.l2NetworkUuid, inv.getUuid()).listValues();

        if (l2Uuids == null){
            return null;
        }

        for (String l2Uuid : l2Uuids) {
            L2NetworkVO l2 = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();
            if (!l2.getType().equals(inv.getType())){
                continue;
            }

            List<VniRangeVO> ranges = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, l2Uuid).list();
            for (VniRangeVO range : ranges) {
                for (VniRangeVO crange: checkRanges) {
                    if (isVniRangeOverlap(range.getStartVni(), range.getEndVni(), crange.getStartVni(), crange.getEndVni())) {
                        return l2Uuid;
                    }
                }
            }
        }

        return null;
    }

    public static boolean isVniRangeOverlap(Integer startVni1, Integer endVni1, Integer startVni2, Integer endVni2) {
        if ((startVni1 >= startVni2 && startVni1 <= endVni2) || (startVni1 <= startVni2 && startVni2 <= endVni1)) {
            return true;
        }
        return false;
    }

    private void validate(APICreateL3NetworkMsg msg) {
        String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        if (type.equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vxlan network pool doesn't support create l3 network")
            ));
        }
    }
}
