package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.network.l2.L2NetworkSystemTags;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L3NetworkHelper {

    public static String getL3networkVSwitchType(String l3Uuid) {
        String l2Uuid = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.l2NetworkUuid)
                .eq(L3NetworkVO_.uuid, l3Uuid).findValue();
        return Q.New(L2NetworkVO.class)
                .select(L2NetworkVO_.vSwitchType)
                .eq(L2NetworkVO_.uuid, l2Uuid).findValue();
    }

    public static String getSdnControllerUuidFromL2Uuid(String l2Uuid) {
        return L2NetworkSystemTags.L2_NETWORK_SDN_CONTROLLER_UUID.getTokenByResourceUuid(
                l2Uuid, L2NetworkSystemTags.L2_NETWORK_SDN_CONTROLLER_UUID_TOKEN);
    }

    public static String getSdnControllerUuidFromL3Uuid(String l3Uuid) {
        String l2Uuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid)
                .select(L3NetworkVO_.l2NetworkUuid).findValue();

        return getSdnControllerUuidFromL2Uuid(l2Uuid);
    }
}
