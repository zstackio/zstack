package org.zstack.authentication.checkfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
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
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.SQL;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.zstack.header.Component;


import static org.zstack.core.Platform.argerr;

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
        new While<>(allFile.values()).all((f, compl) -> {
            try {
                if ( f.getNode().equals("")){
                    checkLocalFile(f);
                }else{
                    checkHostFile(f);
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

    private void sendChanged(FileVerification fv){
        FVCanonicalEvents.FileStatusChangedData data = new FVCanonicalEvents.FileStatusChangedData();
        data.setNode(fv.getNode());
        data.setPath(fv.getPath());
        evtf.fire(FVCanonicalEvents.FILE_STATUS_CHANGED_PATH, data);
    }

    private void checkLocalFile(FileVerification fv){
        try{
            FileInputStream fis = null;
            fis = new FileInputStream(fv.getPath());
            Class clazz = Class.forName("org.apache.commons.codec.digest.DigestUtils");
            String methodName = fv.getType() + "Hex";
            Object fileDigest = clazz.getMethod(methodName, InputStream.class).invoke(clazz.newInstance(), fis);
            if (! fileDigest.toString().equals(fv.getDigest())){
                logger.warn(String.format("File[%s] is changed!", fv.getIdentity()));
                sendChanged(fv);
            }
        }catch (Exception e){
            throw new CloudRuntimeException(e);
        }

    }
    private void checkHostFile(FileVerification fv){
        String fileMd5 = fv.getHostFileDigest();
        if(fileMd5 == null || fileMd5.equals(fv.getDigest())){
            logger.warn(String.format("File[%s] is changed!", fv.getIdentity()));
            sendChanged(fv);
        }
    }

    private void addLocalFileToCheckList(FileVerification fv) {
        FileInputStream fis = null;
        String path = fv.getPath();
        try {
            fis = new FileInputStream(path);
            Class clazz = Class.forName("org.apache.commons.codec.digest.DigestUtils");
            String methodName = fv.getType() + "Hex";
            Object fileDigest = clazz.getMethod(methodName, InputStream.class).invoke(clazz.newInstance(), fis);
            fv.setDigest(fileDigest.toString());
            allFile.put(fv.getIdentity(), fv);
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
        String digest = fv.getHostFileDigest();
        fv.setDigest(digest);
        allFile.put(fv.getIdentity(), fv);
        dbf.persist(fv.toVO());
    }

    private void deleteFileFromCheckList(String path, String node){
        String fvKey = node.equals("") ? path:String.format("%s.%s",node, path);
        if(allFile.get(fvKey) != null){
            allFile.remove(fvKey);
            SQL.New(FileVerificationVO.class)
                    .eq(FileVerificationVO_.path, path)
                    .eq(FileVerificationVO_.node, node)
                    .delete();
        }
    }

    @Override
    public boolean start() {
        List<FileVerificationVO> vos = dbf.listAll(FileVerificationVO.class);
        for (FileVerificationVO vo : vos) {
            FileVerification fv = vo.toFile();
            allFile.put(fv.getIdentity(), fv);
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
        } else if (msg instanceof APIDeleteVerificationFileMsg) {
            handle((APIDeleteVerificationFileMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    public void handle(APIAddVerificationFileMsg msg){
        APIAddVerificationFileEvent evt = new APIAddVerificationFileEvent(msg.getId());
        String path = msg.getPath();
        String node = msg.getNode();
        String type = msg.getType();
        FileVerification fv = new FileVerification();
        fv.setNode(node);
        fv.setPath(path);
        fv.setType(type);
        String fvKey = fv.getIdentity();
        if(allFile.get(fvKey) == null){
            if(! fv.isFileExists()){
                throw new CloudRuntimeException(String.format("No such file [%s]", path));
            }
            logger.info(String.format("Will add [%s] to CheckList.", fvKey));
            if( node.equals("")){
                addLocalFileToCheckList(fv);
            }else{
                addHostFileToCheckList(fv);
            }
            logger.info(String.format("Successfully add [%s] to CheckList.", fv.getIdentity()));
            evt.setSuccess(true);
            bus.publish(evt);
        }else{
            ErrorCode err = argerr("File[%s] is already in the CheckList", path);
            evt.setError(err);
            bus.publish(evt);
        }
        return;
    }

    public void handle(APIDeleteVerificationFileMsg msg){
        APIDeleteVerificationFileEvent evt = new APIDeleteVerificationFileEvent(msg.getId());
        String path = msg.getPath();
        String hostUuid = msg.getNode();
        deleteFileFromCheckList(path, hostUuid);
        logger.info(String.format("Successfully delete [%s] from CheckList.", path));
        evt.setSuccess(true);
        bus.publish(evt);
        return;
    }

}
