package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.AddBackupStorageExtensionPoint;
import org.zstack.header.storage.backup.AddBackupStorageStruct;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/3/16.
 */
public class SftpBackupStorageMetaDataMaker implements AddImageExtensionPoint, AddBackupStorageExtensionPoint, ExpungeImageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SftpBackupStorageMetaDataMaker.class);
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private SftpBackupStorageDumpMetadataInfo dumpInfo;

    private String buildUrl(String subPath, String hostName) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(SftpBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostName);
        }

        ub.port(SftpBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    @Transactional
    private String getAllImageInventories(SftpBackupStorageDumpMetadataInfo dumpInfo) {
        TypedQuery<ImageVO> q;
        String allImageInventories = null;
        ImageInventory img = dumpInfo.getImg();
        String sql = "select img from ImageVO img where uuid in (select imageUuid from ImageBackupStorageRefVO ref where ref.backupStorageUuid= :bsUuid)";
        q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        if (dumpInfo.getImg() != null ) {
            q.setParameter("bsUuid", getBackupStorageUuidFromImageInventory(img));
        } else {
            q.setParameter("bsUuid", dumpInfo.getBackupStorageUuid());
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


    private void restoreImagesBackupStorageMetadataToDatabase(String imagesMetadata, String backupStorageUuid) {
        List<ImageVO> imageVOs = new ArrayList<ImageVO>();
        List<ImageBackupStorageRefVO> backupStorageRefVOs = new ArrayList<ImageBackupStorageRefVO>();
        String[] metadatas = imagesMetadata.split("\n");
        for (String metadata : metadatas) {
            if (metadata.contains("backupStorageRefs")) {
                ImageInventory imageInventory = JSONObjectUtil.toObject(metadata, ImageInventory.class);
                for (ImageBackupStorageRefInventory ref : imageInventory.getBackupStorageRefs()) {
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


    private String getBsUrlFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        List<String> bsUuids = q.listValue();
        if (bsUuids.isEmpty()) {
            return null;
        }
        String bsUuid = bsUuids.get(0);

        SimpleQuery<SftpBackupStorageVO> q2 = dbf.createQuery(SftpBackupStorageVO.class);
        q2.select(SftpBackupStorageVO_.url);
        q2.add(SftpBackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
        List<String> urls = q2.listValue();
        if (urls.isEmpty()) {
            return null;
        }
        return urls.get(0);
    }

    private String getHostNameFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        List<String> bsUuids = q.listValue();
        if (bsUuids.isEmpty()) {
            throw new CloudRuntimeException("Didn't find any available backup storage");
        }
        String bsUuid = bsUuids.get(0);

        SimpleQuery<SftpBackupStorageVO> q2 = dbf.createQuery(SftpBackupStorageVO.class);
        q2.select(SftpBackupStorageVO_.hostname);
        q2.add(SftpBackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
        List<String> hostNames = q2.listValue();
        if (hostNames.isEmpty()) {
            throw new CloudRuntimeException("Didn't find any available hostname");
        }
        return hostNames.get(0);
    }

    @Transactional
    private String getBackupStorageTypeFromImageInventory(ImageInventory img) {
        String sql = "select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO refVo where  " +
                "bs.uuid = refVo.backupStorageUuid and refVo.imageUuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", img.getUuid());
        String type = q.getSingleResult();
        DebugUtils.Assert(type != null, String.format("cannot find backupStorage type for image [uuid:%s]", img.getUuid()));
        return type;
    }

    private String getBackupStorageUuidFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        String backupStorageUuid = q.findValue();
        DebugUtils.Assert(backupStorageUuid != null, String.format("cannot find backup storage for image [uuid:%s]", img.getUuid()));
        return backupStorageUuid;
    }

    protected void dumpImagesBackupStorageInfoToMetaDataFile(SftpBackupStorageDumpMetadataInfo dumpInfo) {
        logger.debug("dump all images info to meta data file");
        boolean allImagesInfo = dumpInfo.getDumpAllInfo();
        ImageInventory img = dumpInfo.getImg();
        String bsUrl = dumpInfo.getBackupStorageUrl();
        String hostName = dumpInfo.getBackupStorageHostname();
        SftpBackupStorageCommands.DumpImageInfoToMetaDataFileCmd dumpCmd = new SftpBackupStorageCommands.DumpImageInfoToMetaDataFileCmd();
        String metaData;
        if (allImagesInfo) {
            metaData = getAllImageInventories(dumpInfo);
        } else {
            metaData = JSONObjectUtil.toJsonString(img);
        }
        dumpCmd.setImageMetaData(metaData);
        dumpCmd.setDumpAllMetaData(allImagesInfo);
        if (bsUrl != null) {
            dumpCmd.setBackupStoragePath(bsUrl);
        } else {
            dumpCmd.setBackupStoragePath(getBsUrlFromImageInventory(img));
        }
        if (hostName == null || hostName.isEmpty()) {
            hostName = getHostNameFromImageInventory(img);
        }
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE, hostName), dumpCmd,
                new JsonAsyncRESTCallback<SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp>(null) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.error("dump image metadata failed" + err.toString());
                    }

                    @Override
                    public void success(SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp rsp) {
                        if (!rsp.isSuccess()) {
                            logger.error("dump image metadata failed");
                        } else {
                            logger.info("dump image metadata successfully");
                        }
                    }

                    @Override
                    public Class<SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp> getReturnClass() {
                        return SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp.class;
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
        if (!getBackupStorageTypeFromImageInventory(img).equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)) {
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.setName("add-image-metadata-to-backupStorage-file");
        chain.then(new ShareFlow() {
            boolean metaDataExist = false;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-image-metadata-file-exist";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SftpBackupStorageCommands.CheckImageMetaDataFileExistCmd cmd = new SftpBackupStorageCommands.CheckImageMetaDataFileExistCmd();
                        cmd.setBackupStoragePath(getBsUrlFromImageInventory(img));
                        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST, getHostNameFromImageInventory(img)), cmd,
                                new JsonAsyncRESTCallback<SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp>(trigger) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        logger.error("check image metadata file exist failed" + err.toString());
                                        trigger.fail(err);
                                    }

                                    @Override
                                    public void success(SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp) {
                                        if (!rsp.isSuccess()) {
                                            logger.error(String.format("check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                            ErrorCode ec = operr("check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                            trigger.fail(ec);
                                        } else {
                                            if (!rsp.getExist()) {
                                                logger.info(String.format("image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                                // call generate and dump all image info to yaml
                                                trigger.next();
                                            } else {
                                                logger.info(String.format("image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                                metaDataExist = true;
                                                trigger.next();
                                            }
                                        }
                                    }

                                    @Override
                                    public Class<SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp> getReturnClass() {
                                        return SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp.class;
                                    }
                                });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-metadata-file";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        if (!metaDataExist) {
                            SftpBackupStorageCommands.GenerateImageMetaDataFileCmd generateCmd = new SftpBackupStorageCommands.GenerateImageMetaDataFileCmd();
                            generateCmd.setBackupStoragePath(getBsUrlFromImageInventory(img));
                            restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE, getHostNameFromImageInventory(img)), generateCmd,
                                    new JsonAsyncRESTCallback<SftpBackupStorageCommands.GenerateImageMetaDataFileRsp>(trigger) {
                                        @Override
                                        public void fail(ErrorCode err) {
                                            logger.error("create image metadata file failed" + err.toString());
                                        }

                                        @Override
                                        public void success(SftpBackupStorageCommands.GenerateImageMetaDataFileRsp rsp) {
                                            if (!rsp.isSuccess()) {
                                                ErrorCode ec = operr("create image metadata file : %s failed", rsp.getBackupStorageMetaFileName());
                                                trigger.fail(ec);
                                            } else {
                                                logger.info("create image metadata file successfully");
                                                SftpBackupStorageDumpMetadataInfo dumpInfo = new SftpBackupStorageDumpMetadataInfo();
                                                dumpInfo.setImg(img);
                                                dumpInfo.setDumpAllInfo(true);
                                                dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
                                                trigger.next();
                                            }
                                        }

                                        @Override
                                        public Class<SftpBackupStorageCommands.GenerateImageMetaDataFileRsp> getReturnClass() {
                                            return SftpBackupStorageCommands.GenerateImageMetaDataFileRsp.class;
                                        }
                                    });

                        } else {
                            SftpBackupStorageDumpMetadataInfo dumpInfo = new SftpBackupStorageDumpMetadataInfo();
                            dumpInfo.setDumpAllInfo(false);
                            dumpInfo.setImg(img);
                            dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
                            trigger.next();
                        }


                    }
                });


                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        // do nothing
                    }
                });

            }

        }).start();
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
        String backupStorageType = backupStorage.getBackupStorageInventory().getType();
        if (!( backupStorageType.equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE) && backupStorage.isImportImages())) {
            return;
        }
        SftpBackupStorageInventory inv = (SftpBackupStorageInventory) backupStorage.getBackupStorageInventory();
        logger.debug("starting to import images metadata");
        SftpBackupStorageCommands.GetImagesMetaDataCmd cmd = new SftpBackupStorageCommands.GetImagesMetaDataCmd();
        cmd.setBackupStoragePath(inv.getUrl());
        cmd.uuid = inv.getUuid();
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GET_IMAGES_METADATA, inv.getHostname()), cmd,
                new JsonAsyncRESTCallback<SftpBackupStorageCommands.GetImagesMetaDataRsp>(null) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.error("check image metadata file exist failed" + err.toString());
                    }

                    @Override
                    public void success(SftpBackupStorageCommands.GetImagesMetaDataRsp rsp) {
                        if (!rsp.isSuccess()) {
                            logger.error(String.format("get images metadata: %s failed", rsp.getImagesMetaData()));
                        } else {
                            logger.info(String.format("get images metadata: %s success", rsp.getImagesMetaData()));
                            restoreImagesBackupStorageMetadataToDatabase(rsp.getImagesMetaData(), backupStorage.getBackupStorageInventory().getUuid());
                        }
                    }

                    @Override
                    public Class<SftpBackupStorageCommands.GetImagesMetaDataRsp> getReturnClass() {
                        return SftpBackupStorageCommands.GetImagesMetaDataRsp.class;
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
        if (!getBackupStorageTypeFromImageInventory(img).equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)) {
            return;
        }
        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.setName("delete-image-info-from-metadata-file");
        String hostName = getHostNameFromImageInventory(img);
        String bsUrl = getBsUrlFromImageInventory(img);
        chain.then(new ShareFlow() {
            boolean metaDataExist = false;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-image-metadata-file-exist";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SftpBackupStorageCommands.CheckImageMetaDataFileExistCmd cmd = new SftpBackupStorageCommands.CheckImageMetaDataFileExistCmd();
                        cmd.setBackupStoragePath(bsUrl);
                        cmd.uuid = backupStorageUuid;
                        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST, hostName), cmd,
                                new JsonAsyncRESTCallback<SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp>(trigger) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        logger.error("check image metadata file exist failed" + err.toString());
                                        trigger.fail(err);
                                    }

                                    @Override
                                    public void success(SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp) {
                                        if (!rsp.isSuccess()) {
                                            logger.error(String.format("check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                            ErrorCode ec = operr("check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                            trigger.fail(ec);
                                        } else {
                                            if (!rsp.getExist()) {
                                                logger.info(String.format("image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                                ErrorCode ec = operr("image metadata file: %s is not exist", rsp.getBackupStorageMetaFileName());
                                                trigger.fail(ec);
                                            } else {
                                                logger.info(String.format("image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                                trigger.next();
                                            }
                                        }
                                    }

                                    @Override
                                    public Class<SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp> getReturnClass() {
                                        return SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp.class;
                                    }
                                });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "delete-image-info";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileCmd deleteCmd = new SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileCmd();
                        deleteCmd.setImageUuid(img.getUuid());
                        deleteCmd.setImageBackupStorageUuid(backupStorageUuid);
                        deleteCmd.setBackupStoragePath(bsUrl);
                        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DELETE_IMAGES_METADATA, hostName), deleteCmd,
                                new JsonAsyncRESTCallback<SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp>(trigger) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        logger.error("delete image metadata file failed" + err.toString());
                                    }

                                    @Override
                                    public void success(SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp rsp) {
                                        if (!rsp.isSuccess()) {
                                            ErrorCode ec = operr("delete image metadata file failed: %s", rsp.getError());
                                            trigger.fail(ec);
                                        } else {
                                            if (rsp.getRet() != 0) {
                                                logger.info(String.format("delete image %s metadata failed : %s", img.getUuid(), rsp.getOut()));
                                                trigger.next();
                                            } else {
                                                logger.info(String.format("delete image %s metadata successfully", img.getUuid()));
                                                trigger.next();
                                            }
                                        }
                                    }

                                    @Override
                                    public Class<SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp> getReturnClass() {
                                        return SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp.class;
                                    }
                                });


                    }
                });

                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        // do nothing
                    }
                });

            }
        }).start();
    }

    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {

    }

}
