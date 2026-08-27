package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.physicalserver.PhysicalServerIdentitySpec;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_10000;

public class ZbsNodeRefContributorImpl implements ZbsNodeRefContributor {
    private static final CLogger logger = Utils.getLogger(
            ZbsNodeRefContributorImpl.class);

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;

    @Override
    public Map<String, ZbsNodeRef> bulkList(Collection<String> serverUuids) {
        Set<String> requestedServerUuids = serverUuids == null
                ? Collections.emptySet() : new HashSet<>(serverUuids);
        List<ExternalPrimaryStorageVO> primaryStorages =
                activeZbsPrimaryStorages();
        Map<String, AddonInfo> addonInfos = new LinkedHashMap<>();
        for (ExternalPrimaryStorageVO primaryStorage : primaryStorages) {
            try {
                addonInfos.put(
                        primaryStorage.getUuid(), parseAddonInfo(primaryStorage));
            } catch (OperationFailureException error) {
                logger.warn(String.format(
                        "skip ZBS primary storage[uuid:%s] when deriving node relations: %s",
                        primaryStorage.getUuid(), error.getMessage()));
            }
        }
        Map<String, String> serversBySerialNumber = serversBySerialNumber(
                addonInfos.values());
        Map<String, ZbsNodeRef> result = new LinkedHashMap<>();
        for (Map.Entry<String, AddonInfo> source : addonInfos.entrySet()) {
            for (MdsInfo mds : source.getValue().getMdsInfos()) {
                if (mds == null) {
                    logger.warn(String.format(
                            "skip an empty mdsInfo of ZBS primary storage[uuid:%s] when deriving node relations",
                            source.getKey()));
                    continue;
                }
                String serialNumber = serialNumber(mds);
                if (serialNumber == null) {
                    logger.warn(String.format(
                            "cannot resolve ZBS MDS physical server because primary storage[uuid:%s] does not report serialNumber",
                            source.getKey()));
                    continue;
                }
                String serverUuid = serversBySerialNumber.get(serialNumber);
                if (serverUuid == null) {
                    logger.warn(String.format(
                            "cannot resolve ZBS MDS physical server serialNumber[%s]",
                            serialNumber));
                    continue;
                }
                if (requestedServerUuids.isEmpty()
                        || requestedServerUuids.contains(serverUuid)) {
                    addRef(
                            result, serverUuid, serialNumber,
                            source.getKey(), mds.getAddr());
                }
            }
        }
        return result;
    }

    private List<ExternalPrimaryStorageVO> activeZbsPrimaryStorages() {
        List<ExternalPrimaryStorageVO> result =
                Q.New(ExternalPrimaryStorageVO.class)
                        .eq(ExternalPrimaryStorageVO_.identity,
                                ZbsConstants.IDENTITY)
                        .list();
        if (result.isEmpty()) {
            return result;
        }
        Set<String> activeUuids = new HashSet<>(
                Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.uuid, result.stream()
                                .map(ExternalPrimaryStorageVO::getUuid)
                                .collect(Collectors.toList()))
                        .listValues());
        result.removeIf(primaryStorage ->
                !activeUuids.contains(primaryStorage.getUuid()));
        return result;
    }

    private AddonInfo parseAddonInfo(ExternalPrimaryStorageVO primaryStorage) {
        if (primaryStorage.getAddonInfo() == null
                || primaryStorage.getAddonInfo().isEmpty()) {
            throw invalidAddonInfo(primaryStorage.getUuid(), "is empty");
        }
        AddonInfo addonInfo = JSONObjectUtil.toObject(
                primaryStorage.getAddonInfo(), AddonInfo.class);
        validateAddonInfo(primaryStorage.getUuid(), addonInfo);
        return addonInfo;
    }

    private void validateAddonInfo(
            String primaryStorageUuid, AddonInfo addonInfo) {
        if (addonInfo == null || addonInfo.getMdsInfos() == null
                || addonInfo.getMdsInfos().isEmpty()) {
            throw invalidAddonInfo(
                    primaryStorageUuid, "does not contain mdsInfos");
        }
    }

    private OperationFailureException invalidAddonInfo(
            String primaryStorageUuid, String detail) {
        return new OperationFailureException(operr(
                ORG_ZSTACK_CORE_10000,
                "cannot derive ZBS node relations because primary storage[uuid:%s] addonInfo %s",
                primaryStorageUuid, detail));
    }

    private Map<String, String> serversBySerialNumber(
            Collection<AddonInfo> addonInfos) {
        Set<String> serialNumbers = new HashSet<>();
        for (AddonInfo addonInfo : addonInfos) {
            for (MdsInfo mds : addonInfo.getMdsInfos()) {
                if (mds != null && serialNumber(mds) != null) {
                    serialNumbers.add(serialNumber(mds));
                }
            }
        }
        if (serialNumbers.isEmpty() || physicalServerManager == null) {
            return Collections.emptyMap();
        }
        List<PhysicalServerIdentitySpec> identities = new ArrayList<>();
        for (String serialNumber : serialNumbers) {
            identities.add(new PhysicalServerIdentitySpec(serialNumber, null));
        }
        return physicalServerManager.resolveIdentities(identities);
    }

    private String serialNumber(MdsInfo mds) {
        return Platform.normalizeMachineSerialNumber(
                mds.getPhysicalServerSerialNumber());
    }

    private void addRef(
            Map<String, ZbsNodeRef> refs,
            String serverUuid,
            String serialNumber,
            String primaryStorageUuid,
            String nodeAddress) {
        ZbsNodeRef ref = refs.computeIfAbsent(serverUuid, ignored -> {
            ZbsNodeRef created = new ZbsNodeRef();
            created.setServerUuid(serverUuid);
            created.setSerialNumber(serialNumber);
            return created;
        });
        if (!serialNumber.equals(ref.getSerialNumber())) {
            ref.setUnavailableError(operr(
                    ORG_ZSTACK_CORE_10000,
                    "ZBS relations for physical server[uuid:%s] report conflicting serial numbers[%s, %s]",
                    serverUuid, ref.getSerialNumber(), serialNumber));
        }
        ref.addPrimaryStorageUuid(primaryStorageUuid);
        if (nodeAddress != null) {
            ref.addNodeAddress(nodeAddress);
        }
        ref.incrementSourceRefCount();
    }
}
