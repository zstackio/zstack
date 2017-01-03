package org.zstack.storage.fusionstor;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.fusionstor.backup.APIAddFusionstorBackupStorageMsg;
import org.zstack.storage.fusionstor.backup.APIAddMonToFusionstorBackupStorageMsg;
import org.zstack.storage.fusionstor.backup.APIUpdateFusionstorBackupStorageMonMsg;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageMonVO;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageMonVO_;
import org.zstack.storage.fusionstor.primary.APIAddFusionstorPrimaryStorageMsg;
import org.zstack.storage.fusionstor.primary.APIAddMonToFusionstorPrimaryStorageMsg;
import org.zstack.storage.fusionstor.primary.APIUpdateFusionstorPrimaryStorageMonMsg;
import org.zstack.storage.fusionstor.primary.FusionstorPrimaryStorageMonVO;
import org.zstack.storage.fusionstor.primary.FusionstorPrimaryStorageMonVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
public class FusionstorApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(FusionstorApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private static final String MON_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddFusionstorBackupStorageMsg) {
            validate((APIAddFusionstorBackupStorageMsg) msg);
        } else if (msg instanceof APIAddFusionstorPrimaryStorageMsg) {
            validate((APIAddFusionstorPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddMonToFusionstorBackupStorageMsg) {
            validate((APIAddMonToFusionstorBackupStorageMsg) msg);
        } else if (msg instanceof APIAddMonToFusionstorPrimaryStorageMsg) {
            validate((APIAddMonToFusionstorPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateFusionstorPrimaryStorageMonMsg) {
            validate((APIUpdateFusionstorPrimaryStorageMonMsg) msg);
        } else if (msg instanceof APIUpdateFusionstorBackupStorageMonMsg) {
            validate((APIUpdateFusionstorBackupStorageMonMsg) msg);
        }
        
        return msg;
    }

    private void checkExistingPrimaryStorage(List<String> monUrls) {
        List<String> hostnames = CollectionUtils.transformToList(monUrls, new Function<String, String>() {
            @Override
            public String call(String url) {
                MonUri uri = new MonUri(url);
                return uri.getHostname();
            }
        });

        SimpleQuery<FusionstorPrimaryStorageMonVO> q = dbf.createQuery(FusionstorPrimaryStorageMonVO.class);
        q.select(FusionstorPrimaryStorageMonVO_.hostname);
        q.add(FusionstorPrimaryStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("cannot add fusionstor primary storage, there has been some fusionstor primary storage using mon[hostnames:%s]", existing)
            ));
        }
    }

    private void validate(APIAddMonToFusionstorPrimaryStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());
    }

    private void validate(APIAddMonToFusionstorBackupStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());
    }

    private void checkMonUrls(List<String> monUrls) {
        List<String> urls = new ArrayList<String>();
        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                new MonUri(url);
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

    private void validate(APIAddFusionstorPrimaryStorageMsg msg) {
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

        checkMonUrls(msg.getMonUrls());
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

        SimpleQuery<FusionstorBackupStorageMonVO> q = dbf.createQuery(FusionstorBackupStorageMonVO.class);
        q.select(FusionstorBackupStorageMonVO_.hostname);
        q.add(FusionstorBackupStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("cannot add fusionstor backup storage, there has been some fusionstor backup storage using mon[hostnames:%s]", existing)
            ));
        }
    }

    private void validate(APIAddFusionstorBackupStorageMsg msg) {
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "poolName can be null but cannot be an empty string"
            ));
        }

        checkMonUrls(msg.getMonUrls());
        checkExistingBackupStorage(msg.getMonUrls());
    }

    private void validate(APIUpdateFusionstorBackupStorageMonMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname())
            ));
        }
        SimpleQuery<FusionstorBackupStorageMonVO> q = dbf.createQuery(FusionstorBackupStorageMonVO.class);
        q.select(FusionstorBackupStorageMonVO_.backupStorageUuid);
        q.add(FusionstorBackupStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String bsUuid = q.findValue();
        msg.setBackupStorageUuid(bsUuid);
    }

    private void validate(APIUpdateFusionstorPrimaryStorageMonMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname())
            ));
        }

        SimpleQuery<FusionstorPrimaryStorageMonVO> q = dbf.createQuery(FusionstorPrimaryStorageMonVO.class);
        q.select(FusionstorPrimaryStorageMonVO_.primaryStorageUuid);
        q.add(FusionstorPrimaryStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String psUuid = q.findValue();
        msg.setPrimaryStorageUuid(psUuid);
    }
}
