package org.zstack.authentication.checkfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands.ConfirmVerificationFilesCommand;
import org.zstack.kvm.KVMAgentCommands.ConfirmVerificationFilesResponse;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by Wenhao.Zhang on 20/10/20
 */
public class FileVerificationInitialServiceImpl implements FileVerificationInitialService {
    private static CLogger logger = Utils.getLogger(FileVerificationInitialServiceImpl.class);
    
    private List<String> mnInitialVerificationFiles;
    private List<String> hostInitialVerificationFiles;
    
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private FileVerificationFacade fvFacade;
    
    public static final String PATH_INITIAL_CONFIRM = "/host/file/initConfirm";
    
    public void setHostInitialVerificationFiles(List<String> hostInitialVerificationFiles) {
        this.hostInitialVerificationFiles = hostInitialVerificationFiles;
    }
    
    public void setMnInitialVerificationFiles(List<String> mnInitialVerificationFiles) {
        this.mnInitialVerificationFiles = mnInitialVerificationFiles;
    }
    
    @BypassWhenUnitTest
    @Override
    public void initManagementNodeFileVerificationList() {
        // If any check file exists, indicates that the management node already add some check files.
        // initialization process will be ignored
        if (fvFacade.anyCheckFilesExists(FileVerification.NODE_MANAGEMENT_NODE)) {
            logger.debug("Initialize management node verification files process will be ignored");
            return;
        }
        
        List<String> filePaths = findAllMNInitialVerificationFiles();
        logger.debug(String.format(
            "Initialize management node verification files : %s", filePaths));
    
        filePaths.forEach(filePath -> {
            FileVerification fv = new FileVerification();
            fv.setPath(filePath);
            fv.setNode(FileVerification.NODE_MANAGEMENT_NODE);
            fv.setUuid(Platform.getUuid());
            fv.setCategory("system-initial");
            fv.setHexType("md5");
            fv.setState(FileVerificationState.Enabled.toString());
            fv.setInitFile(true);
            fvFacade.addVerificationFile(fv);
        });
    }
    
    @Override
    public void initHostFileVerificationList(HostInventory host, Completion completion) {
        final String hostUuid = host.getUuid();
        
        findAllHostInitialVerificationFiles(host, new ReturnValueCompletion<List<String>>(null) {
            @Override
            public void success(List<String> filePaths) {
                logger.debug(String.format(
                    "Initialize host[uuid=%s] verification files : %s",
                    host.getUuid(), filePaths));
                
                filePaths.forEach(filePath -> {
                    FileVerification fv = new FileVerification();
                    fv.setPath(filePath);
                    fv.setNode(hostUuid);
                    fv.setUuid(Platform.getUuid());
                    fv.setCategory("host-initial");
                    fv.setHexType("md5");
                    fv.setState(FileVerificationState.Enabled.toString());
                    fv.setInitFile(true);
                    fvFacade.addVerificationFile(fv);
                });
    
                completion.success();
            }
    
            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
        
    }
    
    public List<String> findAllMNInitialVerificationFiles() {
        if (mnInitialVerificationFiles == null || mnInitialVerificationFiles.isEmpty()) {
            return Collections.emptyList();
        }
    
        return mnInitialVerificationFiles.stream()
            .map(this::localMatchFiles)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    private List<String> localMatchFiles(String pattern) {
        File file = new File(pattern);
        
        if (!pattern.contains("*")) {
            if (file.exists() && !file.isDirectory()) {
                return Collections.singletonList(pattern);
            } else {
                return Collections.emptyList();
            }
        }
        
        // pattern which contain '*'
        final String filename = file.getName();
        if (filename.length() == 0) {
            return Collections.emptyList();
        }
        final File parentFile = file.getParentFile();
        final List<String> matchFiles = new ArrayList<>();
        
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                run(String.format("find %s -name %s", parentFile, filename));
                String output = stdout();
                if (output.contains("No such file or directory")) {
                    return;
                }
                String[] outputs = output.split("\n");
                Collections.addAll(matchFiles, outputs);
            }
        }.execute();
        
        return matchFiles;
    }
    
    public void findAllHostInitialVerificationFiles(HostInventory host,
                ReturnValueCompletion<List<String>> completion) {
        if (hostInitialVerificationFiles == null || hostInitialVerificationFiles.isEmpty()) {
            completion.success(Collections.emptyList());
            return;
        }
    
        ConfirmVerificationFilesCommand cmd = new ConfirmVerificationFilesCommand();
        cmd.patterns = new ArrayList<>(hostInitialVerificationFiles);
    
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(host.getUuid());
        msg.setPath(PATH_INITIAL_CONFIRM);
        msg.setNoStatusCheck(true);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
    
                KVMHostAsyncHttpCallReply httpReply = reply.castReply();
                ConfirmVerificationFilesResponse response =
                        httpReply.toResponse(ConfirmVerificationFilesResponse.class);
                if (!response.isSuccess()) {
                    completion.fail(operr("KVM Host error, because: %s", response.getError()));
                    return;
                }
                
                completion.success(response.paths);
            }
        });
    }
    
    
}
