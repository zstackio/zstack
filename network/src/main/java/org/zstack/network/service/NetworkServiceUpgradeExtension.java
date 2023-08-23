package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class NetworkServiceUpgradeExtension  implements Component {
    private static final CLogger logger = Utils.getLogger(NetworkServiceUpgradeExtension.class);

    @Autowired
    DatabaseFacade dbf;

    @Autowired
    protected TagManager tagMgr;

    private void upgradeMtuSystemTag() {
        /* Create a new l3: Only the new l2 needs to consider the global configuration of mtu */
        List<L3NetworkVO> l3NetworkVOList = Q.New(L3NetworkVO.class).list();

        l3NetworkVOList.forEach(l3NetworkVO -> {
            if (NetworkServiceSystemTag.L3_MTU.getTag(l3NetworkVO.getUuid(), L3NetworkVO.class) == null) {
                L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid()).find();
                Integer mtu = new MtuGetter().getDefaultL2MtuFromGlobalConfig(L2NetworkInventory.valueOf(l2NetworkVO));
                int defaultMtu = Integer.parseInt(NetworkServiceGlobalConfig.DHCP_MTU_DUMMY.value());
                new MtuGetter().createL3MtuSystemTag(l3NetworkVO.getUuid(), mtu != null ? mtu : defaultMtu);
            }
        });
    }

    @Override
    public boolean start() {
        if (NetworkServiceGlobalConfig.UPGRADE_MTU_SYSTEMTAG) {
            upgradeMtuSystemTag();
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}

