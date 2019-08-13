package org.zstack.authentication.checkfile;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.kvm.KVMHostVO;
import org.zstack.utils.Linux;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FileVerification {
    private static final CLogger logger = Utils.getLogger(FileVerification.class);
    private String path;
    //for managementNode, node="", for host, node=HostUuid
    private String node;
    private String type;
    private String digest;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private FileVerificationFacadeImpl fvf;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getIdentity() {
        if (node.equals("")){
            return path;
        }else {
            return String.format("%s.%s", node, path);
        }

    }

    public EventFacade getEvtf() {
        return evtf;
    }

    public FileVerificationVO toVO() {
        FileVerificationVO vo = new FileVerificationVO();
        vo.setPath(path);
        vo.setNode(node);
        vo.setType(type);
        vo.setDigest(digest);
        return vo;
    }

    public KVMHostVO getHostByUuid(){
        KVMHostVO vo = dbf.findByUuid(node, KVMHostVO.class);
        return vo == null ? null : vo;
    }

    public boolean isFileExists(){
        if (node.equals("")) {
            File file = new File(path);
            return file.exists();
        }else {
            KVMHostVO host = getHostByUuid();
            String cmd = String.format("sshpass -p %s ssh -o StrictHostKeyChecking=no %s@%s [ -f %s ]", host.getPassword(), host.getUsername(), host.getManagementIp(), path);
            Linux.ShellResult ret = Linux.shell(cmd);
            return ret.getExitCode() == 0;
        }
    }

    public String getHostFileDigest(){
        KVMHostVO host = getHostByUuid();
        if (host != null) {
            String digestType = type + "sum";
            String ip = host.getManagementIp();
            String username = host.getUsername();
            String password = host.getPassword();
            String cmd = String.format("sshpass -p %s ssh -o StrictHostKeyChecking=no %s@%s %s %s", password, username, ip, digestType, path);
            Linux.ShellResult ret = Linux.shell(cmd);
            return ret.getExitCode() == 0 ? ret.getStdout().split("\\s+")[0] : null;
        }else {
            return null;
        }
    }
}


