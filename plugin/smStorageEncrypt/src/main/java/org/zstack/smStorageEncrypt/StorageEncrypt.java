package org.zstack.smStorageEncrypt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.opencrypto.securitymachine.SecurityMachineGlobalConfig;
import org.zstack.crypto.securitymachine.secretresourcepool.*;
import org.zstack.header.exception.CloudOperationError;
import org.zstack.header.message.MessageReply;
import org.zstack.header.securitymachine.secretresourcepool.CreateSecretkeyMsg;
import org.zstack.header.securitymachine.secretresourcepool.QuerySecretKeyMsg;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.kvm.KVMBeforeAsyncJsonPostExtensionPoint;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class StorageEncrypt implements KVMBeforeAsyncJsonPostExtensionPoint {
    private static final CLogger logger = Utils.getLogger(StorageEncrypt.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public LinkedHashMap kvmBeforeAsyncJsonPostExtensionPoint(String path, LinkedHashMap commandMap, Map header) {

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (path.contains(StorageEncryptConstants.CREATE_VOLUME_FROM_CACHE_PATH) || path.contains(StorageEncryptConstants.CREATE_EMPTY_VOLUME_PATH)) {
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon", volumeUuid));
                linkedHashMap =  createAndAddKey(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted", volumeUuid));
                return null;
            }

        }

        if (path.contains(KVMConstant.KVM_START_VM_PATH)){
            LinkedHashMap encryptVolumes = new LinkedHashMap();
            JsonObject jsonObject = new Gson().toJsonTree(commandMap.get(StorageEncryptConstants.ROOT_VOLUME)).getAsJsonObject();
            String rootVolumeUuid =  jsonObject.get(StorageEncryptConstants.VOLUME_UUID).getAsString();
            List<LinkedTreeMap> dataVolumes = (List<LinkedTreeMap>) commandMap.get(StorageEncryptConstants.DATA_VOLUMES);
            List<LinkedTreeMap> cacheVolumes = (List<LinkedTreeMap>) commandMap.get(StorageEncryptConstants.CACHE_VOLUMES);
            if (isEncrypt(rootVolumeUuid)){
                String key = getKeyByVolumeTag(rootVolumeUuid);
                encryptVolumes.put(rootVolumeUuid,key);
            }
            if (dataVolumes.size() > 0) {
                for (LinkedTreeMap dataVolume: dataVolumes) {
                    JsonObject dataVoluemeJsonObject = new Gson().toJsonTree(dataVolume).getAsJsonObject();
                    String volumeUuid = dataVoluemeJsonObject.get(StorageEncryptConstants.VOLUME_UUID).getAsString();
                    if (isEncrypt(volumeUuid)){
                        String key = getKeyByVolumeTag(volumeUuid);
                        encryptVolumes.put(volumeUuid,key);
                    }
                }
            }

            if (cacheVolumes.size() > 0) {
                for (LinkedTreeMap cacheVolume: cacheVolumes) {
                    JsonObject cacheVolumeJsonObject = new Gson().toJsonTree(cacheVolume).getAsJsonObject();
                    String volumeUuid = cacheVolumeJsonObject.get(StorageEncryptConstants.VOLUME_UUID).getAsString();
                    if (isEncrypt(volumeUuid)){
                        String key = getKeyByVolumeTag(volumeUuid);
                        encryptVolumes.put(volumeUuid,key);
                    }
                }
            }

            linkedHashMap = addEncryptVolumesInCmd(encryptVolumes);
        }

        if (path.contains(KVMConstant.KVM_ATTACH_VOLUME)){
            JsonObject jsonObject = new Gson().toJsonTree(commandMap.get(StorageEncryptConstants.VOLUME)).getAsJsonObject();
            String volumeUuid =  jsonObject.get(StorageEncryptConstants.VOLUME_UUID).getAsString();
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH)){
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(StorageEncryptConstants.OFFLINE_MERGE_SNAPSHOT_PATH)){
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(StorageEncryptConstants.REVERT_VOLUME_FROM_SNAPSHOT_PATH)){
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(StorageEncryptConstants.SHRINK_SNAPSHOT_PATH)){
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(StorageEncryptConstants.CREATE_TEMPLATE_FROM_VOLUME_PATH)){
            String volumePath = (String) commandMap.get(StorageEncryptConstants.VOLUME_PATH);
            String tmpUuid = volumePath.split("/")[3];
            VolumeSnapshotVO s = dbf.findByUuid(tmpUuid, VolumeSnapshotVO.class);
            String volumeUuid = null;
            if (s==null) {
                volumeUuid = tmpUuid;
            } else {
                volumeUuid = s.getVolumeUuid();
            }

            Boolean isEncrypt = isEncrypt(volumeUuid);
            if (isEncrypt){
                logger.debug(String.format("volume[uuid:%s] need to be encrypted, add encrypt options in kvm host addon when attach to vm", volumeUuid));
                linkedHashMap = addKeyToCmd(volumeUuid);
            } else {
                logger.debug(String.format("volume[uuid:%s] does not need to be encrypted when attach to vm", volumeUuid));
                return null;
            }
        }

        if (path.contains(StorageEncryptConstants.RESIZE_VOLUME_PATH)){
            String volumeUuid = (String) commandMap.get(StorageEncryptConstants.VOLUME_UUID);
            Boolean isLive = (Boolean) commandMap.get(StorageEncryptConstants.LIVE);
            if (!isLive) {
                Boolean isEncrypt = isEncrypt(volumeUuid);
                if (isEncrypt){
                    logger.debug(String.format("volume[uuid:%s] is encrypted, add encrypt options in kvm host addon when resize it", volumeUuid));
                    linkedHashMap = addKeyToCmd(volumeUuid);
                } else {
                    logger.debug(String.format("volume[uuid:%s] is not  encrypted, when resize ", volumeUuid));
                    return null;
                }
            }
        }

        return linkedHashMap;
    }

    public boolean isEncrypt(String volumeUuid){
        final String isEncryptTag = VolumeSystemTags.VOLUME_ENCRYPT.getTag(volumeUuid);
        if (isEncryptTag != null && isEncryptTag.equals("volumeEncrypt::true")) {
            return true;
        } else {
            return false;
        }
    }

    public String getKeyByVolumeTag(String volumeUuid) {
        final String volumeEncryptKeyidTag = VolumeSystemTags.VOLUME_ENCRYPT_KEYID.getTag(volumeUuid);
        if (volumeEncryptKeyidTag == null) {
            logger.debug(String.format("volume[uuid:%s] not have encrypt keyid", volumeUuid));
        }
        String key = getKeyById(volumeEncryptKeyidTag.split("::")[1]);
        return key;
    }

    public LinkedHashMap addEncryptVolumesInCmd(LinkedHashMap encryptVolumes){

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(StorageEncryptConstants.ENCRYPT_VOLUMES, encryptVolumes);
        return linkedHashMap;
    }

    public LinkedHashMap addKeyToCmd(String volumeUuid){
        String key = getKeyByVolumeTag(volumeUuid);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(StorageEncryptConstants.ENCRYPT_KEY, key);
        linkedHashMap.put(StorageEncryptConstants.VOLUME_UUID, volumeUuid);
        return linkedHashMap;
    }

    public LinkedHashMap createAndAddKey(String volumeUuid) {
        String resourceId= SecurityMachineGlobalConfig.RESOURCE_POOL_UUID_FOR_DATA_PROTECT.value(String.class);
        CreateSecretkeyMsg msg=new CreateSecretkeyMsg();
        msg.setSecretResourcePoolUuid(resourceId);
        msg.setAlgorithm(TPSecretResourcePoolConstant.ALGORITHM_SM4);
        msg.setKeyUsage(TPSecretResourcePoolConstant.KEY_USAGE_ENC);
        bus.makeTargetServiceIdByResourceUuid(msg, SecretResourcePoolConstant.SERVICE_ID,resourceId);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.debug(String.format("failed to create secret key for volume[uuid:%s], because %s", volumeUuid,reply.getError().getDetails()));
            throw new CloudOperationError(reply.getError().getCode(),reply.getError().getDetails());
        }

        CreateSecretkeyReply cr= reply.castReply();
        logger.debug(String.format("success to create secret key for volume[uuid:%s], the keyid is %s", volumeUuid, cr.getKeyId()));
        final String volumeEncryptKeyidTag = VolumeSystemTags.VOLUME_ENCRYPT_KEYID.getTag(volumeUuid);
        if (volumeEncryptKeyidTag == null) {
            SystemTagCreator creator = VolumeSystemTags.VOLUME_ENCRYPT_KEYID.newSystemTagCreator(volumeUuid);
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        }
        VolumeSystemTags.VOLUME_ENCRYPT_KEYID.updateTagByToken(volumeUuid, VolumeSystemTags.VOLUME_ENCRYPT_KEYID_TOKEN,cr.getKeyId());
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(StorageEncryptConstants.ENCRYPT_KEY, cr.getSecretKey());
        return linkedHashMap;
    }

    private String getKeyById(String keyId){
        String resourceId=SecurityMachineGlobalConfig.RESOURCE_POOL_UUID_FOR_DATA_PROTECT.value(String.class);
        QuerySecretKeyMsg qMsg=new QuerySecretKeyMsg();
        qMsg.setKeyId(keyId);
        qMsg.setSecretResourcePoolUuid(resourceId);
        bus.makeTargetServiceIdByResourceUuid(qMsg,SecretResourcePoolConstant.SERVICE_ID,resourceId);
        MessageReply reply = bus.call(qMsg);
        if (!reply.isSuccess()) {
            logger.debug(String.format("failed to get the key of keyId[keyId:%s], because %s", keyId,reply.getError().getDetails()));
            throw new CloudOperationError(reply.getError().getCode(),reply.getError().getDetails());
        }
        logger.debug(String.format("success to get the key of keyId[keyId:%s]", keyId));
        QuerySecretKeyReply cr=reply.castReply();
        String key = cr.getSecretKey();
        return key;
    }

}

