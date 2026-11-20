package org.zstack.storage.zbs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 11:56
 */
public class ZbsStorageFactory implements ExternalPrimaryStorageSvcBuilder, BackupStorageSelector {
    private static CLogger logger = Utils.getLogger(ZbsStorageFactory.class);
    public static final ExternalStorageFencerType fencerType = new ExternalStorageFencerType(ZbsConstants.IDENTITY, VolumeProtocol.CBD.toString());

    private List<String> preferBackupStorageTypes;

    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<org.zstack.header.storage.addon.primary.AddonInfo> completion) {
        AddonInfo addonInfo = new AddonInfo();

        Config conf = JSONObjectUtil.toObject(config, Config.class);

        if (CollectionUtils.isEmpty(conf.getMdsUrls())) {
            completion.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10037, "mdsUrls cannot be null or empty"));
            return;
        }

        String errInfo = "";
        for (MdsInfo mdsInfo : MdsInfo.valueOf(conf.getMdsUrls())) {
            Ssh ssh = new Ssh();
            ssh.setUsername(mdsInfo.getUsername())
                    .setPassword(mdsInfo.getPassword()).setPort(mdsInfo.getPort())
                    .setHostname(mdsInfo.getAddr())
                    .setTimeout(5);
            try {
                ssh.sudoCommand("/usr/bin/zbs list logical-pool --format json");
                SshResult ret = ssh.run();
                if (ret.getReturnCode() != 0) {
                    errInfo += String.format("failed to list logical pools from MDS[%s], because %s\n", mdsInfo.getAddr(), ret.getStderr());
                    continue;
                }

                ssh.reset();

                String poolStr =  ret.getStdout();
                ZbsListPoolResult result = JSONObjectUtil.toObject(poolStr, ZbsListPoolResult.class);
                if (!result.isSuccess()) {
                    errInfo += String.format("failed to list logical pools from MDS[%s], because %s\n", mdsInfo.getAddr(), result.getError().getMessage());
                    continue;
                }

                for (ZbsListPoolResult.Result poolRet : result.getResult()) {
                    if (poolRet.getStatusCode() == 0) {
                        poolRet.getLogicalPoolInfos().forEach(it ->
                                addonInfo.addLogicalPoolInfo(LogicalPoolInfo.valueOf(it)));
                    }
                }
                completion.success(addonInfo);
                return;
            } finally {
                ssh.close();
            }
        }

        completion.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10038, "unable to discover logical pools from all MDSs, details: %s", errInfo));
    }

    public void setPreferBackupStorageTypes(List<String> preferBackupStorageTypes) {
        this.preferBackupStorageTypes = preferBackupStorageTypes;
    }

    @Override
    public List<String> getPreferBackupStorageTypes() {
        return new ArrayList<>(preferBackupStorageTypes);
    }

    @Override
    public Optional<String> applyMetadataUpdate(String storedConfig, String requestConfig) {
        JsonParser parser = new JsonParser();
        JsonObject request = parser.parse(requestConfig).getAsJsonObject();
        if (!request.has("poolAlias")) {
            return Optional.empty();
        }

        JsonElement patch = request.get("poolAlias");
        if (request.entrySet().size() != 1 || !patch.isJsonObject()) {
            throw invalidPoolAlias();
        }
        JsonObject edit = patch.getAsJsonObject();
        JsonElement name = edit.get("logicalName");
        JsonElement alias = edit.get("aliasName");
        if (edit.entrySet().size() != 2 || !isString(name) || StringUtils.isBlank(name.getAsString())
                || alias == null || (!alias.isJsonNull() && !isString(alias))) {
            throw invalidPoolAlias();
        }

        JsonObject config = parser.parse(storedConfig).getAsJsonObject();
        if (config.has("pools") && config.get("pools").isJsonArray()) {
            for (JsonElement element : config.getAsJsonArray("pools")) {
                JsonObject pool = element.getAsJsonObject();
                if (name.equals(pool.get("logicalName"))) {
                    if (alias.isJsonNull()) {
                        pool.remove("aliasName");
                    } else {
                        pool.add("aliasName", alias);
                    }
                    return Optional.of(config.toString());
                }
            }
        }
        throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10022, "no configured ZBS pool named[%s]", name.getAsString()));
    }

    private static boolean isString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static ApiMessageInterceptionException invalidPoolAlias() {
        return new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10022,
                "poolAlias requires a logicalName and a string or null aliasName"));
    }

    @Override
    public String getIdentity() {
        return ZbsConstants.IDENTITY;
    }
}
