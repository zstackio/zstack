package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephGlobalProperty;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/3/16.
 */
public class CephBackupStorageMetaDataMaker implements AddImageExtensionPoint, AddBackupStorageExtensionPoint, ExpungeImageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephBackupStorageMetaDataMaker.class);

    @Autowired
    protected RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    protected  String buildUrl( String hostName, Integer monPort,String subPath) {
        return String.format("http://%s:%s%s", hostName, monPort, subPath);
    }

    @Transactional
    protected String getAllImageInventories(ImageInventory img, String bsUuid) {
        String allImageInventories = null;
        String sql = "select img from ImageVO img where uuid in (select imageUuid from ImageBackupStorageRefVO ref where ref.backupStorageUuid= :bsUuid)";
        TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        if (img != null ) {
            q.setParameter("bsUuid", getBackupStorageUuidFromImageInventory(img));
        } else {
            q.setParameter("bsUuid", bsUuid);
        }
        List<ImageVO> allImageVO = q.getResultList();
        for (ImageVO imageVO : allImageVO) {
            if (allImageInventories != null) {
                allImageInventories = JSONObjectUtil.toJsonString(ImageInventory.valueOf(imageVO)) + "\n" + allImageInventories;
            } else {
                allImageInventories = JSONObjectUtil.toJsonString(ImageInventory.valueOf(imageVO));
            }
        }
        return allImageInventories;
    }


    protected  String getBackupStorageUuidFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        String backupStorageUuid = q.findValue();
        DebugUtils.Assert(backupStorageUuid != null, String.format("cannot find backup storage for image [uuid:%s]", img.getUuid()));
        return backupStorageUuid;
    }

    protected  void restoreImagesBackupStorageMetadataToDatabase(String imagesMetadata, String backupStorageUuid) {
        List<ImageVO>  imageVOs = new ArrayList<ImageVO>();
        List<ImageBackupStorageRefVO> backupStorageRefVOs = new ArrayList<ImageBackupStorageRefVO>();
        String[] metadatas =  imagesMetadata.split("\n");
        for ( String metadata : metadatas) {
            if (metadata.contains("backupStorageRefs")) {
                ImageInventory imageInventory = JSONObjectUtil.toObject(metadata, ImageInventory.class);
                for ( ImageBackupStorageRefInventory ref : imageInventory.getBackupStorageRefs()) {
                    ImageBackupStorageRefVO backupStorageRefVO = new ImageBackupStorageRefVO();
                    backupStorageRefVO.setStatus(ImageStatus.valueOf(ref.getStatus()));
                    backupStorageRefVO.setInstallPath(ref.getInstallPath());
                    backupStorageRefVO.setImageUuid(ref.getImageUuid());
                    backupStorageRefVO.setBackupStorageUuid(backupStorageUuid);
                    backupStorageRefVO.setCreateDate(ref.getCreateDate());
                    backupStorageRefVO.setLastOpDate(ref.getLastOpDate());
                    backupStorageRefVOs.add(backupStorageRefVO);
                }
                ImageVO imageVO = new ImageVO();
                imageVO.setActualSize(imageInventory.getActualSize());
                imageVO.setDescription(imageInventory.getDescription());
                imageVO.setStatus(ImageStatus.valueOf(imageInventory.getStatus()));
                imageVO.setExportUrl(imageInventory.getExportUrl());
                imageVO.setExportMd5Sum(imageInventory.getExportMd5Sum());
                imageVO.setFormat(imageInventory.getFormat());
                imageVO.setGuestOsType(imageInventory.getGuestOsType());
                imageVO.setMd5Sum(imageInventory.getMd5Sum());
                imageVO.setMediaType(ImageConstant.ImageMediaType.valueOf(imageInventory.getMediaType()));
                imageVO.setName(imageInventory.getName());
                imageVO.setPlatform(ImagePlatform.valueOf(imageInventory.getPlatform()));
                imageVO.setSize(imageInventory.getSize());
                imageVO.setState(ImageState.valueOf(imageInventory.getState()));
                imageVO.setSystem(imageInventory.isSystem());
                imageVO.setType(imageInventory.getType());
                imageVO.setUrl(imageInventory.getUrl());
                imageVO.setUuid(imageInventory.getUuid());
                imageVO.setCreateDate(imageInventory.getCreateDate());
                imageVO.setLastOpDate(imageInventory.getLastOpDate());
                imageVOs.add(imageVO);
            }
        }
        dbf.persistCollection(imageVOs);
        dbf.persistCollection(backupStorageRefVOs);
    }

    protected String getHostnameFromBackupStorage(CephBackupStorageInventory inv) {
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.hostname);
        q.add(CephBackupStorageMonVO_.backupStorageUuid, SimpleQuery.Op.EQ, inv.getUuid());
        q.add(CephBackupStorageMonVO_.status, SimpleQuery.Op.EQ, MonStatus.Connected);
        q.setLimit(1);
        String hostName = q.findValue();
        DebugUtils.Assert(hostName!= null, String.format("cannot find hostName for ceph backup storage [uuid:%s]", inv.getUuid()));
        return hostName;
    }

    @Transactional
    protected  String getHostNameFromImageInventory(ImageInventory img) {
        String sql="select mon.hostname from CephBackupStorageMonVO mon, ImageBackupStorageRefVO ref where " +
                "ref.imageUuid= :uuid and ref.backupStorageUuid = mon.backupStorageUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", img.getUuid());
        return q.getSingleResult();
    }

    @Transactional
    protected  Integer getMonPortFromImageInventory(ImageInventory img) {
        String sql="select mon.monPort from CephBackupStorageMonVO mon, ImageBackupStorageRefVO ref where " +
                "ref.imageUuid= :uuid and ref.backupStorageUuid = mon.backupStorageUuid";
        TypedQuery<Integer> q = dbf.getEntityManager().createQuery(sql, Integer.class);
        q.setParameter("uuid", img.getUuid());
        return q.getSingleResult();
    }

    @Transactional
    protected  String getBackupStorageTypeFromImageInventory(ImageInventory img) {
        String sql = "select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO refVo where  " +
                "bs.uuid = refVo.backupStorageUuid and refVo.imageUuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", img.getUuid());
        return q.getSingleResult();
    }

    protected  void dumpImagesBackupStorageInfoToMetaDataFile(ImageInventory img, boolean allImagesInfo,  String hostName, String bsUuid ) {
        logger.debug("dump images info to meta data file");
        CephBackupStorageBase.DumpImageInfoToMetaDataFileCmd dumpCmd = new CephBackupStorageBase.DumpImageInfoToMetaDataFileCmd();
        String metaData;
        if (allImagesInfo) {
            metaData = getAllImageInventories(null, bsUuid);
        } else {
            metaData = JSONObjectUtil.toJsonString(img);
        }
        dumpCmd.setImageMetaData(metaData);
        dumpCmd.setDumpAllMetaData(allImagesInfo);
        dumpCmd.setPoolName(bsUuid);
        if (hostName ==  null || hostName.isEmpty()) {
           hostName = getHostNameFromImageInventory(img);
        }
        Integer monPort = CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT;
        restf.asyncJsonPost(buildUrl(hostName, monPort, CephBackupStorageBase.DUMP_IMAGE_METADATA_TO_FILE), dumpCmd,
                new JsonAsyncRESTCallback<CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp >(null) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.error("Dump image metadata failed" + err.toString());
                    }

                    @Override
                    public void success(CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp rsp) {
                        if (!rsp.isSuccess()) {
                            logger.error("Dump image metadata failed");
                        } else {
                            logger.info("Dump image metadata successfully");
                        }
                    }

                    @Override
                    public Class<CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp> getReturnClass() {
                        return CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp.class;
                    }
                });
    }

    @Override
    public void preAddImage(ImageInventory img) {

    }

    @Override
    public void beforeAddImage(ImageInventory img) {

    }

    @Override
    public void afterAddImage(ImageInventory img) {
        if (!getBackupStorageTypeFromImageInventory(img).equals(CephConstants.CEPH_BACKUP_STORAGE_TYPE)) {
            return;
        }
        SimpleQuery<CephBackupStorageVO> query = dbf.createQuery(CephBackupStorageVO.class);
        query.add(CephBackupStorageVO_.uuid, SimpleQuery.Op.EQ, getBackupStorageUuidFromImageInventory(img));
        CephBackupStorageVO cephBackupStorageVO = query.find();
        CephBackupStorageInventory inv = CephBackupStorageInventory.valueOf(cephBackupStorageVO);

        BakeImageMetadataMsg msg = new BakeImageMetadataMsg();
        msg.setImg(img);
        msg.setOperation(CephConstants.AFTER_ADD_IMAGE);
        msg.setBackupStorageUuid(getBackupStorageUuidFromImageInventory(img));
        msg.setPoolName(inv.getPoolName());
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug("add image info to metadata file successfully");
                } else {
                    reply.setError(reply.getError());
                }
                bus.reply(msg, reply);
            }
        });

    }

    @Override
    public void failedToAddImage(ImageInventory img, ErrorCode err) {

    }

    @Override
    public void preAddBackupStorage(AddBackupStorageStruct backupStorage) {

    }
    @Override
    public void beforeAddBackupStorage(AddBackupStorageStruct backupStorage) {

    }
    @Override
    public void afterAddBackupStorage(AddBackupStorageStruct backupStorage) {
        logger.debug("starting to import ceph images metadata");
        if (!(backupStorage.getType().equals(CephConstants.CEPH_BACKUP_STORAGE_TYPE) && backupStorage.isImportImages())) {
            logger.debug("will not to import ceph images metadata due to importImages didn't set or bs type is not ceph");
            return;
        }
        String backupStorageUuid = backupStorage.getBackupStorageInventory().getUuid();
        SimpleQuery<CephBackupStorageVO> query = dbf.createQuery(CephBackupStorageVO.class);
        query.add(CephBackupStorageVO_.uuid, SimpleQuery.Op.EQ, backupStorageUuid);
        CephBackupStorageVO cephBackupStorageVO = query.find();
        CephBackupStorageInventory inv = CephBackupStorageInventory.valueOf(cephBackupStorageVO);

        BakeImageMetadataMsg msg = new BakeImageMetadataMsg();
        msg.setBackupStorageUuid(backupStorageUuid);
        msg.setOperation(CephConstants.AFTER_ADD_BACKUPSTORAGE);
        msg.setPoolName(inv.getPoolName());
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                   logger.debug("import ceph backup storage images info successfully");
                } else {
                    logger.debug("import ceph backup storage images info failed");
                    reply.setError(reply.getError());
                }
                bus.reply(msg, reply);
            }
        });
    }

    public void failedToAddBackupStorage(AddBackupStorageStruct backupStorage, ErrorCode err) {

    }

    public void preExpungeImage(ImageInventory img) {

    }

    public void beforeExpungeImage(ImageInventory img) {

    }

    public void afterExpungeImage(ImageInventory img, String backupStorageUuid) {
        logger.debug(String.format("starting to delete image %s from metadata file", img.getUuid()));
        if (!getBackupStorageTypeFromImageInventory(img).equals(CephConstants.CEPH_BACKUP_STORAGE_TYPE)) {
            return;
        }
        SimpleQuery<CephBackupStorageVO> query = dbf.createQuery(CephBackupStorageVO.class);
        query.add(CephBackupStorageVO_.uuid, SimpleQuery.Op.EQ, getBackupStorageUuidFromImageInventory(img));
        CephBackupStorageVO cephBackupStorageVO = query.find();
        CephBackupStorageInventory inv = CephBackupStorageInventory.valueOf(cephBackupStorageVO);

        BakeImageMetadataMsg msg = new BakeImageMetadataMsg();
        msg.setImg(img);
        msg.setBackupStorageUuid(getBackupStorageUuidFromImageInventory(img));
        msg.setOperation(CephConstants.AFTER_EXPUNGE_IMAGE);
        msg.setPoolName(inv.getPoolName());
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug("delete image info from metadata file successfully");
                } else {
                    reply.setError(reply.getError());
                }
                bus.reply(msg, reply);
            }
        });

    }

    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {

    }

}
