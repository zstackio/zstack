package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.util.Map;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosDeployAgentFlow extends VyosRunScriptFlow {
    private static final CLogger logger = Utils.getLogger(VyosDeployAgentFlow.class);

    private final static String REMOTE_ZVR_PATH = "/home/vyos/zvr.bin";
    private final static String REMOTE_ZVRBOOT_PATH = "/home/vyos/zvrboot.bin";

    @Override
    public void initEnv() {
        setLogger(Utils.getLogger(VyosDeployAgentFlow.class));
    }

    @Override
    public boolean isSkipRunningScript(Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        boolean isReconnect = Boolean.parseBoolean((String) data.get(ApplianceVmConstant.Params.isReconnect.toString()));

        // no need to reboot agent
        if (!isReconnect && !ApplianceVmGlobalConfig.DEPLOY_AGENT_ON_START.value(Boolean.class)) {
            return true;
        }

        if (isZvrMd5Changed(mgmtNicIp, sshPort)) {
            data.put(ApplianceVmConstant.Params.needRebootAgent.toString(), Boolean.TRUE.toString());
            return false;
        }
        return true;
    }

    @Override
    public void createScript() {
        scpUpload(PathUtil.findFileOnClassPath("ansible/zvr/zvr.bin", true).getAbsolutePath(),
                "/home/vyos/zvr.bin");
        scpUpload(PathUtil.findFileOnClassPath("ansible/zvr/zvrboot.bin", true).getAbsolutePath(),
                "/home/vyos/zvrboot.bin");
        scpUpload(PathUtil.findFileOnClassPath("ansible/zvr/version", true).getAbsolutePath(),
                "/home/vyos/zvr/version");
    }

    @Override
    public String getTaskName() {
        return VyosDeployAgentFlow.class.getName();
    }

    @Override
    public String getScriptName() {
        return "vyos deploy agent";
    }

    private boolean isZvrMd5Changed(String ip, int port){
        int interval = 30 ;
        Ssh ssh = new Ssh();
        ssh.setUsername("vyos")
                .setPrivateKey(asf.getPrivateKey())
                .setPort(port)
                .setHostname(ip)
                .setTimeout(interval);

        String remoteZvrMd5 = "";
        String remoteZvrbootMd5 = "";

        String localZvrPath = PathUtil.findFileOnClassPath("ansible/zvr/zvr.bin", true).getAbsolutePath();
        String localZvrBootPath = PathUtil.findFileOnClassPath("ansible/zvr/zvrboot.bin", true).getAbsolutePath();


        try {
            ssh.command(String.format("sudo -S md5sum %s 2>/dev/null", REMOTE_ZVR_PATH));
            SshResult ret = ssh.run();
            if (ret.getReturnCode() == 0) {
                remoteZvrMd5 =  ret.getStdout().split(" ")[0];
            }
            ssh.reset();

            ShellResult zvrRet = ShellUtils.runAndReturn(String.format("md5sum %s", localZvrPath));
            zvrRet.raiseExceptionIfFail();
            String localZvrMd5 = zvrRet.getStdout().split(" ")[0];

            if (!remoteZvrMd5.equals(localZvrMd5)) {
                logger.debug(String.format("file MD5 changed, local[%s, md5:%s] remote[%s, md5: %s]", localZvrPath,
                        localZvrMd5, REMOTE_ZVR_PATH, remoteZvrMd5));
                return true;
            }

            ssh.command(String.format("sudo -S md5sum %s 2>/dev/null", REMOTE_ZVRBOOT_PATH));
            ret = ssh.run();
            if (ret.getReturnCode() == 0) {
                remoteZvrbootMd5 =  ret.getStdout().split(" ")[0];
            }

            ShellResult zvrbootRet = ShellUtils.runAndReturn(String.format("md5sum %s",localZvrBootPath));
            zvrbootRet.raiseExceptionIfFail();
            String localZvrbootMd5 = zvrbootRet.getStdout().split(" ")[0];


            if (!remoteZvrbootMd5.equals(localZvrbootMd5)) {
                logger.debug(String.format("file MD5 changed, local[%s, md5:%s] remote[%s, md5: %s]", localZvrBootPath,
                        localZvrbootMd5, REMOTE_ZVRBOOT_PATH, remoteZvrbootMd5));
                return true;
            }

        }catch (SshException  e ) {
            logger.debug(String.format("unable to check vyos[ip:%s, port:%s] zvr md5", ip, port, e.getMessage()));
        }finally {
            ssh.close();
        }

        return false;
    }

}
