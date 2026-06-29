package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.APIAttachBackupStorageToZoneMsg;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.zone.ManagementNetworkIpVersionManager;
import org.zstack.header.zone.ManagementNetworkIpVersionResourceExtensionPoint;
import org.zstack.storage.ceph.backup.*;
import org.zstack.storage.ceph.primary.*;
import org.zstack.utils.CharacterUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created by frank on 7/29/2015.
 */
public class CephApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor,
        ManagementNetworkIpVersionResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephApiInterceptor.class);

    private static final String CEPH_PRIMARY_STORAGE_RESOURCE_TYPE = "ceph primary storage";
    private static final String CEPH_PRIMARY_STORAGE_MON_RESOURCE_TYPE = "ceph primary storage mon";
    private static final String CEPH_BACKUP_STORAGE_MON_RESOURCE_TYPE = "ceph backup storage mon";
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ManagementNetworkIpVersionManager managementNetworkIpVersionManager;

    private static final String MON_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddCephBackupStorageMsg) {
            validate((APIAddCephBackupStorageMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStorageMsg) {
            validate((APIAddCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            validate((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephBackupStorageMonMsg) {
            validate((APIUpdateCephBackupStorageMonMsg) msg);
        } else if (msg instanceof APIAddMonToCephPrimaryStorageMsg) {
            validate((APIAddMonToCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStorageMonMsg) {
            validate((APIUpdateCephPrimaryStorageMonMsg) msg);
        } else if (msg instanceof APIDeleteCephPrimaryStoragePoolMsg) {
            validate((APIDeleteCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStoragePoolMsg) {
            validate((APIAddCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStoragePoolMsg) {
            validate((APIUpdateCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIAttachPrimaryStorageToClusterMsg) {
            validate((APIAttachPrimaryStorageToClusterMsg) msg);
        } else if (msg instanceof APIAttachBackupStorageToZoneMsg) {
            validate((APIAttachBackupStorageToZoneMsg) msg);
        }
        
        return msg;
    }

    private void validate(APIUpdateCephPrimaryStoragePoolMsg msg) {
        String psUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.uuid, msg.getUuid())
                .findValue();

        msg.setPrimaryStorageUuid(psUuid);
    }

    private void validate(APIAddCephPrimaryStoragePoolMsg msg) {
        if (!CharacterUtils.checkCharacter(msg.getPoolName())){
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10006, "operation failure, because the poolName[poolName:%s] can not include unprintable ascii characters.", msg.getPoolName()));
        }

        String duplicatePoolUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .eq(CephPrimaryStoragePoolVO_.poolName, msg.getPoolName())
                .eq(CephPrimaryStoragePoolVO_.type, msg.getType())
                .select(CephPrimaryStoragePoolVO_.uuid).findValue();
        if (duplicatePoolUuid != null && msg.isCreate()) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_STORAGE_CEPH_10007,         "creation failure, duplicate poolName[%s]. There has been a pool[uuid:%s] with the same name existing.",
                    msg.getPoolName(), duplicatePoolUuid));

        } else if (duplicatePoolUuid != null && !msg.isCreate()) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_STORAGE_CEPH_10008,         "Ceph pool[uuid:%s] with this name is already added into ZStack and used elsewhere, cannot reuse the ceph pool.",
                    duplicatePoolUuid));
        }

    }

    private void validate(APIDeleteCephPrimaryStoragePoolMsg msg) {
        msg.setPrimaryStorageUuid(
                Q.New(CephPrimaryStoragePoolVO.class).select(CephPrimaryStoragePoolVO_.primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.uuid, msg.getUuid()).findValue()
        );
    }

    private void checkExistingPrimaryStorage(List<String> monUrls) {
        List<String> hostnames = CollectionUtils.transformToList(monUrls, new Function<String, String>() {
            @Override
            public String call(String url) {
                MonUri uri = new MonUri(url);
                return uri.getHostname();
            }
        });

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.hostname);
        q.add(CephPrimaryStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10009, "cannot add ceph primary storage, there has been some ceph primary storage using mon[hostnames:%s]", existing));
        }
    }

    private void distinctMons(List<String> mons) {
        List<String> monUrls = new ArrayList<>();
        for(String mon: mons) {
            MonUri uri = new MonUri(mon);
            if (!monUrls.contains(uri.getHostname())) {
                monUrls.add(uri.getHostname());
            } else {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10010, "Cannot add same host[%s] in mons", uri.getHostname()));
            }
        }
    }

    private void validate(APIAddMonToCephPrimaryStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());
        validatePrimaryStorageMonUrls(msg.getPrimaryStorageUuid(), msg.getMonUrls(), ORG_ZSTACK_STORAGE_CEPH_10026);
        List<String> hostnames = msg.getMonUrls().stream()
                .map(MonUri::new)
                .map(MonUri::getHostname)
                .collect(Collectors.toList());

        if (Q.New(CephPrimaryStorageMonVO.class).in(CephPrimaryStorageMonVO_.hostname, hostnames).isExists()){
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10011, "Adding the same Mon node is not allowed"));
        }
    }

    private void validate(APIAddMonToCephBackupStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());
        validateBackupStorageMonUrls(msg.getBackupStorageUuid(), msg.getMonUrls(), ORG_ZSTACK_STORAGE_CEPH_10027);

        List<String> hostnames = msg.getMonUrls().stream()
                .map(MonUri::new)
                .map(MonUri::getHostname)
                .collect(Collectors.toList());

        if (Q.New(CephBackupStorageMonVO.class).in(CephBackupStorageMonVO_.hostname, hostnames).isExists()){
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10012, "Adding the same Mon node is not allowed"));
        }
    }
    private void validate(APIUpdateCephBackupStorageMonMsg msg) {
        if (msg.getHostname() != null && !IPv6NetworkUtils.isValidManagementEndpoint(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10035,
                    "hostname[%s] is not a valid IPv4 address, IPv6 address, or hostname", msg.getHostname()));
        }
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.backupStorageUuid);
        q.add(CephBackupStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String bsUuid = q.findValue();
        msg.setBackupStorageUuid(bsUuid);
        if (msg.getHostname() != null) {
            validateBackupStorageEndpoint(bsUuid, msg.getHostname(), CEPH_BACKUP_STORAGE_MON_RESOURCE_TYPE, msg.getMonUuid(),
                    ORG_ZSTACK_STORAGE_CEPH_10028);
        }
    }

    private void validate(APIUpdateCephPrimaryStorageMonMsg msg) {
        if (msg.getHostname() != null && !IPv6NetworkUtils.isValidManagementEndpoint(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10036,
                    "hostname[%s] is not a valid IPv4 address, IPv6 address, or hostname", msg.getHostname()));
        }

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.primaryStorageUuid);
        q.add(CephPrimaryStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String psUuid = q.findValue();
        msg.setPrimaryStorageUuid(psUuid);
        if (msg.getHostname() != null) {
            validatePrimaryStorageEndpoint(psUuid, msg.getHostname(), CEPH_PRIMARY_STORAGE_MON_RESOURCE_TYPE, msg.getMonUuid(),
                    ORG_ZSTACK_STORAGE_CEPH_10029);
        }
    }

    private void checkMonUrls(List<String> monUrls) {
        distinctMons(monUrls);

        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                new MonUri(url);
            } catch (OperationFailureException ae) {
                throw new ApiMessageInterceptionException(ae.getErrorCode());
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10015, "invalid monUrl[%s]. A valid url is in format of %s", monUrl, MON_URL_FORMAT));
            }
        }
    }

    private void validate(APIAddCephPrimaryStorageMsg msg) {
        if (msg.getDataVolumePoolName() != null && msg.getDataVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_STORAGE_CEPH_10016,         "dataVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getRootVolumePoolName() != null && msg.getRootVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_STORAGE_CEPH_10017,         "rootVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getImageCachePoolName() != null && msg.getImageCachePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
            ORG_ZSTACK_STORAGE_CEPH_10018,         "imageCachePoolName can be null but cannot be an empty string"
            ));
        }

        checkMonUrls(msg.getMonUrls());
        validateMonUrlsInZone(msg.getZoneUuid(), msg.getMonUrls(), CEPH_PRIMARY_STORAGE_RESOURCE_TYPE, msg.getName(),
                ORG_ZSTACK_STORAGE_CEPH_10030);
        checkExistingPrimaryStorage(msg.getMonUrls());
    }

    private void checkExistingBackupStorage(List<String> monUrls) {
        List<String> hostnames = CollectionUtils.transformToList(monUrls, new Function<String, String>() {
            @Override
            public String call(String url) {
                MonUri uri = new MonUri(url);
                return uri.getHostname();
            }
        });

        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.hostname);
        q.add(CephBackupStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10019, "cannot add ceph backup storage, there has been some ceph backup storage using mon[hostnames:%s]", existing));
        }
    }

    private void validate(APIAddCephBackupStorageMsg msg) {
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10020, "poolName can be null but cannot be an empty string"));
        }else if(msg.isImportImages() && msg.getPoolName() == null){
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_CEPH_10021, "poolName is required when importImages is true"));
        }

        checkMonUrls(msg.getMonUrls());
        checkExistingBackupStorage(msg.getMonUrls());
    }

    private void validate(APIAttachPrimaryStorageToClusterMsg msg) {
        String type = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                .findValue();
        if (!CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(type)) {
            return;
        }

        String zoneUuid = Q.New(ClusterVO.class)
                .select(ClusterVO_.zoneUuid)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .findValue();
        validateCephPrimaryStorageMonsInZone(msg.getPrimaryStorageUuid(), zoneUuid, ORG_ZSTACK_STORAGE_CEPH_10031);
    }

    private void validate(APIAttachBackupStorageToZoneMsg msg) {
        validateCephBackupStorageMonsInZone(msg.getBackupStorageUuid(), msg.getZoneUuid(), ORG_ZSTACK_STORAGE_CEPH_10032);
    }

    private void validatePrimaryStorageMonUrls(String primaryStorageUuid, List<String> monUrls, String errorCode) {
        String zoneUuid = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .findValue();
        validateMonUrlsInZone(zoneUuid, monUrls, "ceph primary storage", primaryStorageUuid, errorCode);
    }

    private void validateBackupStorageMonUrls(String backupStorageUuid, List<String> monUrls, String errorCode) {
        List<String> zoneUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.zoneUuid)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, backupStorageUuid)
                .listValues();
        for (String zoneUuid : zoneUuids) {
            validateMonUrlsInZone(zoneUuid, monUrls, "ceph backup storage", backupStorageUuid, errorCode);
        }
    }

    private void validatePrimaryStorageEndpoint(String primaryStorageUuid, String endpoint, String resourceType,
                                                String resourceIdentity, String errorCode) {
        String zoneUuid = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .findValue();
        managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, endpoint, resourceType, resourceIdentity, errorCode);
    }

    private void validateBackupStorageEndpoint(String backupStorageUuid, String endpoint, String resourceType,
                                               String resourceIdentity, String errorCode) {
        List<String> zoneUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.zoneUuid)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, backupStorageUuid)
                .listValues();
        for (String zoneUuid : zoneUuids) {
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, endpoint, resourceType, resourceIdentity, errorCode);
        }
    }

    private void validateMonUrlsInZone(String zoneUuid, List<String> monUrls, String resourceType,
                                       String resourceIdentity, String errorCode) {
        for (String monUrl : monUrls) {
            MonUri uri = new MonUri(monUrl);
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, uri.getHostname(),
                    resourceType, resourceIdentity, errorCode);
        }
    }

    private void validateCephPrimaryStorageMonsInZone(String primaryStorageUuid, String zoneUuid, String errorCode) {
        List<CephPrimaryStorageMonVO> mons = Q.New(CephPrimaryStorageMonVO.class)
                .eq(CephPrimaryStorageMonVO_.primaryStorageUuid, primaryStorageUuid)
                .list();
        for (CephPrimaryStorageMonVO mon : mons) {
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, mon.getHostname(),
                    CEPH_PRIMARY_STORAGE_MON_RESOURCE_TYPE, mon.getUuid(), errorCode);
        }
    }

    private void validateCephBackupStorageMonsInZone(String backupStorageUuid, String zoneUuid, String errorCode) {
        List<CephBackupStorageMonVO> mons = Q.New(CephBackupStorageMonVO.class)
                .eq(CephBackupStorageMonVO_.backupStorageUuid, backupStorageUuid)
                .list();
        for (CephBackupStorageMonVO mon : mons) {
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, mon.getHostname(),
                    CEPH_BACKUP_STORAGE_MON_RESOURCE_TYPE, mon.getUuid(), errorCode);
        }
    }

    @Override
    public void validateExistingResourcesInZone(String zoneUuid, String ipVersion) {
        List<String> primaryStorageUuids = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                .listValues();
        if (!primaryStorageUuids.isEmpty()) {
            List<CephPrimaryStorageMonVO> primaryMons = Q.New(CephPrimaryStorageMonVO.class)
                    .in(CephPrimaryStorageMonVO_.primaryStorageUuid, primaryStorageUuids)
                    .list();
            for (CephPrimaryStorageMonVO mon : primaryMons) {
                managementNetworkIpVersionManager.validateEndpointMatchesIpVersion(zoneUuid, ipVersion, mon.getHostname(),
                        CEPH_PRIMARY_STORAGE_MON_RESOURCE_TYPE, mon.getUuid(), ORG_ZSTACK_STORAGE_CEPH_10033);
            }
        }

        List<String> backupStorageUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.backupStorageUuid)
                .eq(BackupStorageZoneRefVO_.zoneUuid, zoneUuid)
                .listValues();
        if (backupStorageUuids.isEmpty()) {
            return;
        }
        List<CephBackupStorageMonVO> backupMons = Q.New(CephBackupStorageMonVO.class)
                .in(CephBackupStorageMonVO_.backupStorageUuid, backupStorageUuids)
                .list();
        for (CephBackupStorageMonVO mon : backupMons) {
            managementNetworkIpVersionManager.validateEndpointMatchesIpVersion(zoneUuid, ipVersion, mon.getHostname(),
                    CEPH_BACKUP_STORAGE_MON_RESOURCE_TYPE, mon.getUuid(), ORG_ZSTACK_STORAGE_CEPH_10034);
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(APIAttachPrimaryStorageToClusterMsg.class, APIAttachBackupStorageToZoneMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
