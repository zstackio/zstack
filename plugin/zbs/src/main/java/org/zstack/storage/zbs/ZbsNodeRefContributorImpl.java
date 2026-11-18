package org.zstack.storage.zbs;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.physicalserver.PhysicalServerVO;
import org.zstack.physicalserver.PhysicalServerVO_;
import javax.persistence.Tuple;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_10000;

public class ZbsNodeRefContributorImpl implements ZbsNodeRefContributor {
    private static final CLogger logger = Utils.getLogger(ZbsNodeRefContributorImpl.class);

    @Override
    public Map<String, ZbsNodeRef> getNodesByServerUuids(Collection<String> serverUuids) {
        if (serverUuids == null || serverUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Tuple> servers = Q.New(PhysicalServerVO.class)
                .select(PhysicalServerVO_.serialNumber, PhysicalServerVO_.uuid)
                .in(PhysicalServerVO_.uuid, serverUuids).listTuple();
        if (servers.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            conditions.add("ps.addonInfo like :serial" + i + " escape '!'");
        }
        SQL query = SQL.New("select ps.uuid, ps.addonInfo from ExternalPrimaryStorageVO ps "
                + "where ps.identity = :identity and (" + String.join(" or ", conditions) + ")")
                .param("identity", ZbsConstants.IDENTITY);
        for (int i = 0; i < servers.size(); i++) {
            String serial = JSONObjectUtil.toJsonString(servers.get(i).get(0, String.class));
            query.param("serial" + i, "%" + serial.replace("!", "!!").replace("%", "!%")
                    .replace("_", "!_") + "%");
        }
        Map<String, ZbsNodeRef> nodes = getNodesBySerialNumber(primaryStorages(query));
        Map<String, ZbsNodeRef> result = new LinkedHashMap<>();
        for (Tuple server : servers) {
            ZbsNodeRef node = nodes.get(server.get(0, String.class));
            if (node != null) {
                result.put(server.get(1, String.class), node);
            }
        }
        return result;
    }

    @Override
    public Map<String, ZbsNodeRef> getAllNodesBySerialNumber() {
        return getNodesBySerialNumber(activeZbsPrimaryStorages());
    }

    Map<String, ZbsNodeRef> getNodesBySerialNumber(Collection<ExternalPrimaryStorageVO> primaryStorages) {
        Map<String, ZbsNodeRef> result = new LinkedHashMap<>();
        for (ExternalPrimaryStorageVO primaryStorage : primaryStorages) {
            for (MdsInfo mds : parseAddonInfo(primaryStorage).getMdsInfos()) {
                if (mds == null) {
                    logger.warn(String.format(
                            "skip an empty mdsInfo of ZBS primary storage[uuid:%s] when deriving node relations",
                            primaryStorage.getUuid()));
                    continue;
                }
                String serialNumber = serialNumber(mds);
                if (serialNumber == null) {
                    logger.warn(String.format(
                            "cannot resolve ZBS MDS physical server because " +
                                    "primary storage[uuid:%s] does not report serialNumber",
                            primaryStorage.getUuid()));
                    continue;
                }
                addRef(result, serialNumber, mds.getAddr());
            }
        }
        return result;
    }

    private List<ExternalPrimaryStorageVO> activeZbsPrimaryStorages() {
        return primaryStorages(SQL.New("select ps.uuid, ps.addonInfo from ExternalPrimaryStorageVO ps "
                + "where ps.identity = :identity").param("identity", ZbsConstants.IDENTITY));
    }

    private List<ExternalPrimaryStorageVO> primaryStorages(SQL query) {
        List<ExternalPrimaryStorageVO> result = new ArrayList<>();
        for (Object[] row : query.<Object[]>list()) {
            ExternalPrimaryStorageVO source = new ExternalPrimaryStorageVO();
            source.setUuid((String) row[0]);
            source.setAddonInfo((String) row[1]);
            result.add(source);
        }
        return result;
    }

    static AddonInfo parseAddonInfo(ExternalPrimaryStorageVO primaryStorage) {
        if (primaryStorage.getAddonInfo() == null || primaryStorage.getAddonInfo().isEmpty()) {
            throw invalidAddonInfo(primaryStorage.getUuid(), "is empty");
        }
        AddonInfo addonInfo = JSONObjectUtil.toObject(primaryStorage.getAddonInfo(), AddonInfo.class);
        validateAddonInfo(primaryStorage.getUuid(), addonInfo);
        return addonInfo;
    }

    private static void validateAddonInfo(String primaryStorageUuid, AddonInfo addonInfo) {
        if (addonInfo == null || addonInfo.getMdsInfos() == null || addonInfo.getMdsInfos().isEmpty()) {
            throw invalidAddonInfo(primaryStorageUuid, "does not contain mdsInfos");
        }
    }

    private static OperationFailureException invalidAddonInfo(String primaryStorageUuid, String detail) {
        return new OperationFailureException(operr(
                ORG_ZSTACK_CORE_10000,
                "cannot derive ZBS node relations because primary storage[uuid:%s] addonInfo %s",
                primaryStorageUuid, detail));
    }

    static Set<String> serialNumbers(AddonInfo addonInfo) {
        Set<String> serialNumbers = new HashSet<>();
        if (addonInfo != null && addonInfo.getMdsInfos() != null) {
            for (MdsInfo mds : addonInfo.getMdsInfos()) {
                if (mds != null && serialNumber(mds) != null) {
                    serialNumbers.add(serialNumber(mds));
                }
            }
        }
        return serialNumbers;
    }

    private static String serialNumber(MdsInfo mds) {
        return Platform.normalizeMachineSerialNumber(mds.getPhysicalServerSerialNumber());
    }

    private void addRef(Map<String, ZbsNodeRef> refs, String serialNumber, String nodeAddress) {
        if (refs.containsKey(serialNumber)) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_10000,
                    "Multiple ZBS nodes report physical server[serialNumber:%s]", serialNumber));
        }
        ZbsNodeRef ref = new ZbsNodeRef();
        ref.setSerialNumber(serialNumber);
        ref.setNodeAddress(nodeAddress);
        refs.put(serialNumber, ref);
    }
}
