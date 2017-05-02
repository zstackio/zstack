package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.utils.network.NetworkUtils;

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
        String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        if (!type.equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
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
    }

    private void validate(APICreateL3NetworkMsg msg) {
        String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        if (type.equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vxlan network pool do not support create l3 network")
            ));
        }
    }
}
