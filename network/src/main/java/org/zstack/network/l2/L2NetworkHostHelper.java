package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l2.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NetworkHostHelper {
    @Autowired
    DatabaseFacade dbf;

    private static final CLogger logger = Utils.getLogger(L2NetworkHostHelper.class);

    public void initL2NetworkHostRef(String l2NetworkUuid, List<String> hostUuids, String l2ProviderType) {
        List<L2NetworkHostRefVO> vos = new ArrayList<>();
        List<String> oldHosts = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .listValues();

        hostUuids.forEach(uuid -> {
            if (oldHosts.contains(uuid)) {
                return;
            }

            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(uuid);
            vo.setL2NetworkUuid(l2NetworkUuid);
            vo.setL2ProviderType(l2ProviderType);
            vo.setAttachStatus(L2NetworkAttachStatus.Detached);
            vos.add(vo);
            logger.debug(String.format("add L2NetworkHostRefVO, l2NetworkUuid:%s, hostUuid:%s",
                    l2NetworkUuid, uuid));
        });

        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }

    public void initL2NetworkHostRefOrSetDetached(List<String> l2NetworkUuids, String hostUuid, String l2ProviderType) {
        List<L2NetworkHostRefVO> newVos = new ArrayList<>();
        List<L2NetworkHostRefVO> oldVos = Q.New(L2NetworkHostRefVO.class)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .list();
        List<String> oldL2s = new ArrayList<>();

        oldVos.forEach(ref -> {
            ref.setAttachStatus(L2NetworkAttachStatus.Detached);
            oldL2s.add(ref.getL2NetworkUuid());
        });

        l2NetworkUuids.forEach(l2Uuid -> {
            if (oldL2s.contains(l2Uuid)) {
                return;
            }

            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(hostUuid);
            vo.setL2NetworkUuid(l2Uuid);
            vo.setL2ProviderType(l2ProviderType);
            vo.setAttachStatus(L2NetworkAttachStatus.Detached);
            newVos.add(vo);
        });

        if (!newVos.isEmpty()) {
            logger.debug(String.format("add L2NetworkHostRefVO, %s", JSONObjectUtil.toJsonString(newVos)));
            dbf.persistCollection(newVos);
        }
        if (!oldVos.isEmpty()) {
            logger.debug(String.format("update L2NetworkHostRefVO, %s", JSONObjectUtil.toJsonString(oldVos)));
            dbf.updateCollection(oldVos);
        }
    }

    public void deleteL2NetworkHostRef(String l2NetworkUuid, List<String> hostUuids) {
        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuid:%s, hostUuids:%s",
                l2NetworkUuid, hostUuids));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .delete();
    }

    public void changeL2NetworkToHostRefDetached(String l2NetworkUuid, String hostUuid) {
        if (!Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid).isExists()) {
            throw new CloudRuntimeException(String.format("can not find host l2 network ref[l2NetworkUuid: %s, hostUuid: %s]",
                    l2NetworkUuid, hostUuid));
        }

        logger.debug(String.format("change L2NetworkHostRefVO to Detached, l2NetworkUuid:%s, hostUuid:%s",
                l2NetworkUuid, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .set(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Detached).update();
    }

    public void changeL2NetworkToHostRefAttached(String l2NetworkUuid, String hostUuid) {
        if (!Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid).isExists()) {
            throw new CloudRuntimeException(String.format("can not find host l2 network ref[l2NetworkUuid: %s, hostUuid: %s]",
                    l2NetworkUuid, hostUuid));
        }

        logger.debug(String.format("change L2NetworkHostRefVO to Attached, l2NetworkUuid:%s, hostUuid:%s",
                l2NetworkUuid, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .set(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Attached).update();
    }

    public List<String> getAttachedHostUuidsRefToL2NetworkUuid(String l2NetworkUuid) {
        return Q.New(L2NetworkHostRefVO.class).select(L2NetworkHostRefVO_.hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Attached)
                .listValues();
    }
}
