package org.zstack.storage.surfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;

import org.zstack.storage.surfs.backup.APIAddSurfsBackupStorageMsg;
import org.zstack.storage.surfs.backup.APIAddNodeToSurfsBackupStorageMsg;
import org.zstack.storage.surfs.backup.APIUpdateSurfsBackupStorageNodeMsg;
import org.zstack.storage.surfs.backup.SurfsBackupStorageNodeVO;
import org.zstack.storage.surfs.backup.SurfsBackupStorageNodeVO_;

import org.zstack.storage.surfs.primary.APIAddSurfsPrimaryStorageMsg;
import org.zstack.storage.surfs.primary.APIAddNodeToSurfsPrimaryStorageMsg;
import org.zstack.storage.surfs.primary.APIUpdateSurfsPrimaryStorageNodeMsg;
import org.zstack.storage.surfs.primary.SurfsPrimaryStorageNodeVO;
import org.zstack.storage.surfs.primary.SurfsPrimaryStorageNodeVO_;

import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
public class SurfsApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(SurfsApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private static final String MON_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSurfsBackupStorageMsg) {
            validate((APIAddSurfsBackupStorageMsg) msg);
        } else if (msg instanceof APIAddSurfsPrimaryStorageMsg) {
            validate((APIAddSurfsPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddNodeToSurfsBackupStorageMsg) {
            validate((APIAddNodeToSurfsBackupStorageMsg) msg);
        } else if (msg instanceof APIAddNodeToSurfsPrimaryStorageMsg) {
            validate((APIAddNodeToSurfsPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateSurfsPrimaryStorageNodeMsg) {
            validate((APIUpdateSurfsPrimaryStorageNodeMsg) msg);
        } else if (msg instanceof APIUpdateSurfsBackupStorageNodeMsg) {
            validate((APIUpdateSurfsBackupStorageNodeMsg) msg);
        }
        
        return msg;
    }

    private void checkExistingPrimaryStorage(List<String> monUrls) {
        List<String> hostnames = CollectionUtils.transformToList(monUrls, new Function<String, String>() {
            @Override
            public String call(String url) {
                NodeUri uri = new NodeUri(url);
                return uri.getHostname();
            }
        });

        SimpleQuery<SurfsPrimaryStorageNodeVO> q = dbf.createQuery(SurfsPrimaryStorageNodeVO.class);
        q.select(SurfsPrimaryStorageNodeVO_.hostname);
        q.add(SurfsPrimaryStorageNodeVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("cannot add surfs primary storage, there has been some surfs primary storage using mon[hostnames:%s]", existing)
            ));
        }
    }

    private void validate(APIAddNodeToSurfsPrimaryStorageMsg msg) {
        checkNodeUrls(msg.getNodeUrls());
    }

    private void validate(APIAddNodeToSurfsBackupStorageMsg msg) {
        checkNodeUrls(msg.getNodeUrls());
    }

    private void checkNodeUrls(List<String> monUrls) {
        List<String> urls = new ArrayList<String>();
        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                new NodeUri(url);
            } catch (OperationFailureException ae) {
                throw new ApiMessageInterceptionException(ae.getErrorCode());
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("invalid monUrl[%s]. A valid url is in format of %s", monUrl, MON_URL_FORMAT)
                ));
            }
        }
    }

    private void validate(APIAddSurfsPrimaryStorageMsg msg) {
        if (msg.getDataVolumePoolName() != null && msg.getDataVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "dataVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getRootVolumePoolName() != null && msg.getRootVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "rootVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getImageCachePoolName() != null && msg.getImageCachePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "imageCachePoolName can be null but cannot be an empty string"
            ));
        }

        checkNodeUrls(msg.getNodeUrls());
        checkExistingPrimaryStorage(msg.getNodeUrls());
    }

    private void checkExistingBackupStorage(List<String> monUrls) {
        List<String> hostnames = CollectionUtils.transformToList(monUrls, new Function<String, String>() {
            @Override
            public String call(String url) {
                NodeUri uri = new NodeUri(url);
                return uri.getHostname();
            }
        });

        SimpleQuery<SurfsBackupStorageNodeVO> q = dbf.createQuery(SurfsBackupStorageNodeVO.class);
        q.select(SurfsBackupStorageNodeVO_.hostname);
        q.add(SurfsBackupStorageNodeVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("cannot add surfs backup storage, there has been some surfs backup storage using mon[hostnames:%s]", existing)
            ));
        }
    }

    private void validate(APIAddSurfsBackupStorageMsg msg) {
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "poolName can be null but cannot be an empty string"
            ));
        }

        checkNodeUrls(msg.getNodeUrls());
        checkExistingBackupStorage(msg.getNodeUrls());
    }

    private void validate(APIUpdateSurfsBackupStorageNodeMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname())
            ));
        }
        SimpleQuery<SurfsBackupStorageNodeVO> q = dbf.createQuery(SurfsBackupStorageNodeVO.class);
        q.select(SurfsBackupStorageNodeVO_.backupStorageUuid);
        q.add(SurfsBackupStorageNodeVO_.uuid, Op.EQ, msg.getNodeUuid());
        String bsUuid = q.findValue();
        msg.setBackupStorageUuid(bsUuid);
    }

    private void validate(APIUpdateSurfsPrimaryStorageNodeMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname())
            ));
        }

        SimpleQuery<SurfsPrimaryStorageNodeVO> q = dbf.createQuery(SurfsPrimaryStorageNodeVO.class);
        q.select(SurfsPrimaryStorageNodeVO_.primaryStorageUuid);
        q.add(SurfsPrimaryStorageNodeVO_.uuid, Op.EQ, msg.getNodeUuid());
        String psUuid = q.findValue();
        msg.setPrimaryStorageUuid(psUuid);
    }
}
