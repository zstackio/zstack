package org.zstack.authentication.checkfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.SQL;
import org.zstack.core.db.Q;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.zstack.header.Component;
import org.zstack.utils.path.PathUtil;


import static org.zstack.core.Platform.argerr;
import static java.nio.file.StandardCopyOption.*;

public class FileVerificationFacadeImpl extends AbstractService implements FileVerificationFacade, Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FileVerificationFacadeImpl.class);
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static final String HOST_FILE_VERIFICATION = "/host/file/check";
    private static final String LOCAL_BACKUP_DIR = PathUtil.join(PathUtil.getZStackHomeFolder(), "backupfiles");


    private Future<Void> fileCheckTask;

    public Map<String, FileVerification> allFile = new ConcurrentHashMap<>();
    @Override
    public String getId() {
        return bus.makeLocalServiceId(FileVerificationConstant.SERVICE_ID);
    }

    @Override
    public Map getAllFile() {
        return allFile;
    }

    public static class CheckHostFileCmd extends KVMAgentCommands.AgentCommand {
        private List<FileVerification> files;

        public List<FileVerification> getFiles() {
            return files;
        }

        public void setFiles(List<FileVerification> files) {
            this.files = files;
        }
    }

    public static class CheckHostFileRsp extends KVMAgentCommands.AgentResponse {
        public List<String> changeList;
        public List<String> restoreFailedList;

        public List<String> getChangeList() {
            return changeList;
        }

        public void setChangeList(List<String> changeList) {
            this.changeList = changeList;
        }

        public List<String> getRestoreFailedList() {
            return restoreFailedList;
        }

        public void setRestoreFailedList(List<String> restoreFailedList) {
            this.restoreFailedList = restoreFailedList;
        }
    }

    private void startFileCheck() {
        if (fileCheckTask != null) {
            fileCheckTask.cancel(true);
        }
        logger.debug("Start file check task.");
        fileCheckTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return FVGlobalConfig.FILE_VERIFICATION_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "file-check-tasks";
            }

            @Override
            @ExceptionSafe
            public void run() {
                checkFile();
            }
        });
    }

    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] is ready, starts to check task.", Platform.getManagementServerId()));
        startFileCheck();

        FVGlobalConfig.FILE_VERIFICATION_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startFileCheck();
            }
        });
    }

    private void checkFile() {
        Map<String, List> filesGroupByNode = new HashMap<>();
        List<String> nodes = Q.New(FileVerificationVO.class)
                .select(FileVerificationVO_.node)
                .groupBy(FileVerificationVO_.node)
                .listValues();
        if (nodes.isEmpty()) {
            return;
        }
        for(String node : nodes){
            List<FileVerificationVO> fvs = Q.New(FileVerificationVO.class)
                    .eq(FileVerificationVO_.node, node)
                    .eq(FileVerificationVO_.state, FileVerificationState.Enabled.toString())
                    .list();
            if(!fvs.isEmpty()){
                filesGroupByNode.put(node, fvs);
            }
        }
        new While<>(filesGroupByNode.keySet()).all((node, compl) -> {
            try {
                if( node.equals("mn")){
                    checkLocalFiles(filesGroupByNode.get(node));
                }else{
                    checkHostFiles(node, filesGroupByNode.get(node));
                }
            }catch (Exception e){
                throw new CloudRuntimeException(e);
            }
        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                logger.info("File check successfully!");
            }
        });
    }

    private void sendChanged(String node, String path, String category, String restore){
        FVCanonicalEvents.FileStatusChangedData data = new FVCanonicalEvents.FileStatusChangedData();
        data.setNode(node);
        data.setPath(path);
        data.setCatefory(category);
        data.setRestore(restore);
        evtf.fire(FVCanonicalEvents.FILE_STATUS_CHANGED_PATH, data);
    }

    public void backupRestoreLocalFile(String srcPath, String dstPath) throws IOException {
        File source = new File(srcPath);
        File dest = new File(dstPath);
        if (!dest.getParentFile().exists()){
            dest.getParentFile().mkdirs();
        }
        Files.copy(source.toPath(), dest.toPath(), REPLACE_EXISTING);

    }

    private String getLocalFileDigest(FileVerification fv){
        try {
            FileInputStream fis = null;
            fis = new FileInputStream(fv.getPath());
            Class clazz = Class.forName("org.apache.commons.codec.digest.DigestUtils");
            String methodName = fv.getHexType() + "Hex";
            Object fileDigest = clazz.getMethod(methodName, InputStream.class).invoke(clazz.newInstance(), fis);
            return fileDigest.toString();
        }catch (Exception e){
            throw new CloudRuntimeException(e);
        }

    }

    private void checkLocalFiles(List<FileVerificationVO> fvs){
        for (FileVerificationVO fv : fvs) {
            boolean needRestore = false;
            if (PathUtil.isDir(fv.getPath())) {
                logger.warn(String.format("Filed to restore [%s.%s], it's a directory now.", fv.getNode(), fv.getPath()));
                sendChanged(fv.getNode(), fv.getPath(), fv.getCategory(), FileRestoreState.False.toString());
                continue;
            }
            if (PathUtil.exists(fv.getPath())) {
                needRestore = !getLocalFileDigest(fv.toFile()).equals(fv.getDigest());
            } else {
                needRestore = true;
            }
            if (needRestore) {
                try {
                    logger.info(String.format("Will restore local file from [%s] to [%s]", PathUtil.join(LOCAL_BACKUP_DIR, fv.getUuid()), fv.getPath()));
                    backupRestoreLocalFile(PathUtil.join(LOCAL_BACKUP_DIR, fv.getUuid()), fv.getPath());
                } catch (Exception e) {
                    logger.warn(String.format("The file[%s.%s] was modified and restore failed.", fv.getNode(), fv.getPath()));
                    sendChanged(fv.getNode(), fv.getPath(), fv.getCategory(), FileRestoreState.False.toString());
                } finally {
                    logger.info(String.format("The file[%s.%s] was modified but successfully restored.", fv.getNode(), fv.getPath()));
                    sendChanged(fv.getNode(), fv.getPath(), fv.getCategory(), FileRestoreState.True.toString());
                }

            }
        }

    }
    private void checkHostFiles(String node, List<FileVerification> files){
        CheckHostFileCmd cmd = new CheckHostFileCmd();
        cmd.setFiles(files);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(node);
        msg.setPath(HOST_FILE_VERIFICATION);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, node);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new CloudRuntimeException(String.format("failed to check host[%s] files.", node));
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                CheckHostFileRsp rsp = r.toResponse(CheckHostFileRsp.class);
                if (rsp.changeList.isEmpty()){
                    logger.info(String.format("all file on host[%s] is integrated.", node));
                    return;
                }
                for (String uuid : rsp.changeList){
                    if (rsp.restoreFailedList.contains(uuid)){
                        logger.warn(String.format("The file[%s.%s] was modified and restore failed.", node, allFile.get(uuid).getPath()));
                        sendChanged(node, allFile.get(uuid).getPath(), allFile.get(uuid).getCategory(), FileRestoreState.False.toString());
                    }else{
                        logger.info(String.format("The file[%s.%s] was modified but successfully restored.", node, allFile.get(uuid).getPath()));
                        sendChanged(node, allFile.get(uuid).getPath(), allFile.get(uuid).getCategory(), FileRestoreState.True.toString());
                    }
                }
            }
        });
    }

    private void addLocalFileToCheckList(FileVerification fv) {
        FileInputStream fis = null;
        String path = fv.getPath();
        try {
            String digest = getLocalFileDigest(fv);
            fv.setDigest(digest);
            String backupPath = PathUtil.join(LOCAL_BACKUP_DIR, fv.getUuid());
            backupRestoreLocalFile(path, backupPath);
            allFile.put(fv.getUuid(), fv);
            dbf.persist(fv.toVO());
        }catch(IOException e){
            throw new CloudRuntimeException(e);
        }catch (Exception e){
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e_) {
                    logger.warn(String.format("FileInputStream close IOException：%s", e_.getMessage()));
                }
            }
        }

    }

    private void addHostFileToCheckList(FileVerification fv){
        fv.addHostFile(true);
    }


    private void recoverFileToCheckList(FileVerification fv) throws IOException{
        if (fv.getNode().equals("mn")){
            String digest = getLocalFileDigest(fv);
            if(! digest.equals(fv.getDigest())){
                backupRestoreLocalFile(fv.getPath(), PathUtil.join(LOCAL_BACKUP_DIR, fv.getUuid()));
                allFile.get(fv.getUuid()).setDigest(digest);
                SQL.New(FileVerificationVO.class).eq(FileVerificationVO_.uuid, fv.getUuid()).set(FileVerificationVO_.digest, digest).update();
            }
        }else{
            fv.addHostFile(false);
        }
    }

    @Override
    public boolean start() {
        List<FileVerificationVO> vos = dbf.listAll(FileVerificationVO.class);
        for (FileVerificationVO vo : vos) {
            FileVerification fv = vo.toFile();
            allFile.put(fv.getUuid(), fv);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg){
        if (msg instanceof APIAddVerificationFileMsg) {
            handle((APIAddVerificationFileMsg) msg);
        }else if(msg instanceof APIRemoveVerificationFileMsg){
            handle((APIRemoveVerificationFileMsg) msg);
        }else if(msg instanceof APIRecoverVerificationFileMsg){
            handle((APIRecoverVerificationFileMsg) msg);
        }else if (msg instanceof APIDeleteVerificationFileMsg) {
            handle((APIDeleteVerificationFileMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    public void handle(APIAddVerificationFileMsg msg){
        APIAddVerificationFileEvent evt = new APIAddVerificationFileEvent(msg.getId());
        String uuid = Platform.getUuid();
        String path = msg.getPath();
        String node = msg.getNode();
        String hexType = msg.getHexType();
        String category = msg.getCategory();
        FileVerification fv = new FileVerification();
        fv.setUuid(uuid);
        fv.setNode(node);
        fv.setPath(path);
        fv.setHexType(hexType);
        fv.setCategory(category);
        fv.setState(FileVerificationState.Enabled.toString());
        FileVerificationVO fvo = Q.New(FileVerificationVO.class).eq(FileVerificationVO_.node, node).eq(FileVerificationVO_.path, path).find();
        if(fvo == null){
            if(! fv.isFileExists()){
                throw new CloudRuntimeException(String.format("No such file [%s] on node: %s", path, node));
            }
            logger.info(String.format("Will add [%s.%s] to CheckList.", node, path));
            if( node.equals("mn")){
                addLocalFileToCheckList(fv);
            }else{
                addHostFileToCheckList(fv);
            }
            logger.info(String.format("Successfully add [%s.%s] to CheckList.", node, path));
            evt.setSuccess(true);
            bus.publish(evt);
        }else{
            ErrorCode err = argerr("File[%s.%s] is already in the CheckList", node, path);
            evt.setError(err);
            bus.publish(evt);
        }
        return;
    }

    public void handle(APIRemoveVerificationFileMsg msg){
        APIRemoveVerificationFileEvent evt = new APIRemoveVerificationFileEvent(msg.getId());
        String uuid = msg.getUuid();
        if(allFile.get(uuid) == null){
            throw new CloudRuntimeException(String.format("Cannot found %s from CheckList", uuid));
        }
        SQL.New(FileVerificationVO.class).eq(FileVerificationVO_.uuid, uuid).set(FileVerificationVO_.state, FileVerificationState.Disabled.toString()).update();
        allFile.get(uuid).setState(FileVerificationState.Disabled.toString());
        logger.info(String.format("Successfully remove file[%s.%s] from CheckList.", allFile.get(uuid).getNode(), allFile.get(uuid).getPath()));
        evt.setSuccess(true);
        bus.publish(evt);
        return;
    }

    public void handle(APIRecoverVerificationFileMsg msg){
        APIRecoverVerificationFileEvent evt = new APIRecoverVerificationFileEvent(msg.getId());
        String uuid = msg.getUuid();
        FileVerification fv = allFile.get(uuid);
        if(fv == null){
            throw new CloudRuntimeException(String.format("Cannot found %s from CheckList", uuid));
        }
        try {
            recoverFileToCheckList(fv);
        }catch (IOException e){
            throw new CloudRuntimeException(e);
        }
        SQL.New(FileVerificationVO.class).eq(FileVerificationVO_.uuid, uuid).set(FileVerificationVO_.state, FileVerificationState.Enabled.toString()).update();
        allFile.get(uuid).setState(FileVerificationState.Enabled.toString());
        logger.info(String.format("Successfully recover file[%s.%s] to CheckList.", allFile.get(uuid).getNode(), allFile.get(uuid).getPath()));
        evt.setSuccess(true);
        bus.publish(evt);
        return;
    }

    public void handle(APIDeleteVerificationFileMsg msg){
        APIDeleteVerificationFileEvent evt = new APIDeleteVerificationFileEvent(msg.getId());
        String uuid = msg.getUuid();
        SQL.New(FileVerificationVO.class).eq(FileVerificationVO_.uuid, uuid).delete();
        logger.info(String.format("Successfully delete file[%s.%s] from CheckList.", allFile.get(uuid).getNode(), allFile.get(uuid).getPath()));
        allFile.remove(uuid);
        evt.setSuccess(true);
        bus.publish(evt);
        return;
    }


}
