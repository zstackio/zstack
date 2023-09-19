package org.zstack.authentication.checkfile;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostVO;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FileVerification {
    private static final CLogger logger = Utils.getLogger(FileVerification.class);
    public static final String NODE_MANAGEMENT_NODE = "mn";
    private String uuid;
    private String path;
    private String node; //for managementNode, node="mn", for host, node=HostUuid
    private String hexType;
    private String digest;
    private String category;
    private String state;
    transient private boolean initFile;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private FileVerificationFacadeImpl fvf;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getHexType() {
        return hexType;
    }

    public void setHexType(String hexType) {
        this.hexType = hexType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
    
    public boolean isInitFile() {
        return initFile;
    }
    
    public void setInitFile(boolean initFile) {
        this.initFile = initFile;
    }
    
    private static final String HOST_ADD_FILE = "/host/file/add";

    public EventFacade getEvtf() {
        return evtf;
    }

    public static class AddHostFileCmd extends KVMAgentCommands.AgentCommand {
        private String uuid;
        private String path;
        private String hexType;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getHexType() {
            return hexType;
        }

        public void setHexType(String hexType) {
            this.hexType = hexType;
        }
    }

    public static class addHostFileRsp extends KVMAgentCommands.AgentResponse {
        public String digest;
        public boolean backup;
    }

    public FileVerificationVO toVO() {
        FileVerificationVO vo = new FileVerificationVO();
        vo.setUuid(uuid);
        vo.setPath(path);
        vo.setNode(node);
        vo.setHexType(hexType);
        vo.setDigest(digest);
        vo.setState(state);
        vo.setCategory(category);
        return vo;
    }

    private String getSshPassCommand() {
        KVMHostVO vo = dbf.findByUuid(node, KVMHostVO.class);
        if (vo == null) {
            throw new CloudRuntimeException(String.format("failed to find target host[uuid:%s].", node));
        }

        return String.format(
                "timeout 10 sshpass -p '%s' ssh -q -o UserKnownHostsFile=/dev/null -o PubkeyAuthentication=no -o StrictHostKeyChecking=no -p %d %s@%s PATH=/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin",
                vo.getPassword(),
                vo.getPort(),
                vo.getUsername(),
                vo.getManagementIp()
        );
    }

    public boolean isFileExists(){
        if (NODE_MANAGEMENT_NODE.equals(node)) {
            File file = new File(path);
            return file.getAbsoluteFile().exists();
        } else {
            ShellResult rst = ShellUtils.runAndReturn(String.format(
                    "%s ls %s 2>/dev/null", getSshPassCommand(), path
            ));
            return rst.getRetCode() == 0;
        }
    }

    public void addHostFile(boolean addNew){
        AddHostFileCmd cmd = new AddHostFileCmd();
        cmd.setUuid(uuid);
        cmd.setPath(path);
        cmd.setHexType(hexType);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(node);
        msg.setPath(HOST_ADD_FILE);
        if (initFile) {
            msg.setNoStatusCheck(true);
        }
        
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, node);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new CloudRuntimeException(String.format("failed to add file [%s.%s] to checkList.", node, path));
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                addHostFileRsp rsp = r.toResponse(addHostFileRsp.class);
                if (! rsp.backup){
                    logger.debug(String.format("failed to backup file [%s.%s]", node, path));
                }
                digest = rsp.digest;
                if(addNew){
                    fvf.allFile.put(uuid, FileVerification.this);
                    dbf.persist(FileVerification.this.toVO());
                }else{
                    fvf.allFile.get(uuid).setDigest(digest);
                    SQL.New(FileVerificationVO.class).eq(FileVerificationVO_.uuid, uuid).set(FileVerificationVO_.digest, digest).update();
                }
            }
        });
    }

}


