package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkAttachStatus;
import org.zstack.header.network.l2.L2NetworkHostRefInventory;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO_;

import java.util.ArrayList;
import java.util.List;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NetworkHostHelper {
    @Autowired
    DatabaseFacade dbf;

    public void attachL2NetworkToHost(String l2NetworkUuid, String hostUuid,
                                      String providerType,
                                      L2NetworkAttachStatus status) {
        L2NetworkHostRefVO ref = Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid).find();
        if (ref != null) {
            ref.setAttachStatus(status);
            dbf.updateAndRefresh(ref);
        } else {
            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(hostUuid);
            vo.setL2NetworkUuid(l2NetworkUuid);
            vo.setL2ProviderType(providerType);
            vo.setAttachStatus(status);
            dbf.persist(vo);
        }
    }

    public void attachL2NetworksToHost(List<String> l2NetworkUuids, String hostUuid,
                                       String providerType,
                                       L2NetworkAttachStatus status) {
        List<L2NetworkHostRefVO> refs = new ArrayList<>();
        for (String uuid : l2NetworkUuids) {
            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(hostUuid);
            vo.setL2NetworkUuid(uuid);
            vo.setL2ProviderType(providerType);
            vo.setAttachStatus(status);
            refs.add(vo);
        }

        dbf.persistCollection(refs);
    }

    public L2NetworkHostRefInventory getL2NetworkHostRef(String l2NetworkUuid, String hostUuid) {
        L2NetworkHostRefVO ref = Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid).find();
        if (ref == null) {
            return null;
        } else {
            return L2NetworkHostRefInventory.valueOf(ref);
        }
    }
}
