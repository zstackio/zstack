package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.upgrade.UpgradeChecker;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosDeployAgentFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VyosDeployAgentFlow.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private UpgradeChecker upgradeChecker;


    private String REMOTE_USER;
    private String REMOTE_PASS = VirtualRouterGlobalConfig.VYOS_PASSWORD.value();
    private int    REMOTE_PORT = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);

    private String REMOTE_ZVR_DIR;
    private String REMOTE_ZVR_BIN_PATH;
    private String REMOTE_ZVRBOOT_BIN_PATH;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            trigger.next();
            return;
        }

        boolean isReconnect = Boolean.parseBoolean((String) data.get(Params.isReconnect.toString()));
        final String vrUuid = (String) data.get(VmInstanceConstant.Params.vmInstanceUuid.toString());
        String vrUserTag = VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER.getTokenByResourceUuid(
                            vrUuid, VirtualRouterVmVO.class, VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER_TOKEN);
        REMOTE_USER = vrUserTag != null ? vrUserTag : "vyos"; //old vpc vrouter has no tag, that's vyos.
        REMOTE_ZVR_DIR = String.format("/home/%s/zvr", REMOTE_USER);
        REMOTE_ZVR_BIN_PATH = String.format("/home/%s/zvr.bin", REMOTE_USER);
        REMOTE_ZVRBOOT_BIN_PATH = String.format("/home/%s/zvrboot.bin", REMOTE_USER);


        if (!isReconnect && !ApplianceVmGlobalConfig.DEPLOY_AGENT_ON_START.value(Boolean.class)) {
            // no need to deploy agent
            trigger.next();
            return;
        }

        String mgmtNicIp;
        if (!isReconnect) {
            VmNicInventory mgmtNic;
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                    }
                });
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        } else {
            mgmtNicIp = (String) data.get(Params.managementNicIp.toString());
        }

        int timeoutInSeconds = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class);
        long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutInSeconds);

        List<Throwable> errors = new ArrayList<>();
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    boolean forceReboot = false;
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        trigger.fail(err(ApplianceVmErrors.UNABLE_TO_START, "virtual router deploy agent failed, because %s",
                                errors.get(errors.size() -1)));
                        return true;
                    }

                    if (NetworkUtils.isRemotePortOpen(mgmtNicIp, REMOTE_PORT, 2000)) {
                        if(isZvrMd5Changed(mgmtNicIp, REMOTE_PORT)){
                            logger.debug(String.format("will deploy virtual router agent by remote user[%s] from management", REMOTE_USER));
                            deployAgent(REMOTE_PORT);
                            forceReboot = true;
                        }
                        rebootAgent(REMOTE_PORT, forceReboot);
                        trigger.next();
                        return true;
                    } else {
                        errors.add(new Throwable(String.format("virtual router agent port %s is not opened on managment nic %s", REMOTE_PORT, mgmtNicIp)));
                        return false;
                    }
                } catch (Throwable t) {
                    logger.warn("virtual router deploy agent failed", t);
                    errors.add(t);
                    return false;
                }
            }

            private void deployAgent(int port) {
                try {
                    new Ssh().setTimeout(300).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/zvr.bin", true).getAbsolutePath(),
                            REMOTE_ZVR_BIN_PATH
                    ).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/zvrboot.bin", true).getAbsolutePath(),
                            REMOTE_ZVRBOOT_BIN_PATH
                    ).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/version", true).getAbsolutePath(),
                            String.format("%s/mn_zvr_version", REMOTE_ZVR_DIR)
                    ).setPrivateKey(asf.getPrivateKey()).setUsername(REMOTE_USER).setHostname(mgmtNicIp).setPort(port).runErrorByExceptionAndClose();

                } catch (SshException  e ) {
                    /*
                    ZSTAC-18352, try again with password when key fail
                     */
                    new Ssh().setTimeout(300).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/zvr.bin", true).getAbsolutePath(),
                            REMOTE_ZVR_BIN_PATH
                    ).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/zvrboot.bin", true).getAbsolutePath(),
                            REMOTE_ZVRBOOT_BIN_PATH
                    ).scpUpload(
                            PathUtil.findFileOnClassPath("ansible/zvr/version", true).getAbsolutePath(),
                            String.format("%s/mn_zvr_version", REMOTE_ZVR_DIR)
                    ).setPassword(REMOTE_PASS).setUsername(REMOTE_USER).setHostname(mgmtNicIp).setPort(port).runErrorByExceptionAndClose();
                }
                
                // update vyos agent version when open grayScaleUpgrade
                upgradeChecker.updateAgentVersion(vrUuid, VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE, new VirtualRouterMetadataOperator().getManagementVersion(), new VirtualRouterMetadataOperator().getManagementVersion());
            }

            private void rebootAgent(int port, boolean forceReboot) {
                String script = String.format("sudo bash %s\n" +
                        "sudo bash %s\n" +
                        "sudo bash %s/ssh/zvr-reboot.sh %s\n",
                        REMOTE_ZVRBOOT_BIN_PATH, REMOTE_ZVR_BIN_PATH, REMOTE_ZVR_DIR, forceReboot);

                try {
                    new Ssh().shell(script
                    ).setTimeout(300).setPrivateKey(asf.getPrivateKey()).setUsername(REMOTE_USER).setHostname(mgmtNicIp).setPort(port).runErrorByExceptionAndClose();
                } catch (SshException  e ) {
                    /*
                    ZSTAC-18352, try again with password when key fail
                     */
                    new Ssh().shell(script
                    ).setTimeout(300).setPassword(REMOTE_PASS).setUsername(REMOTE_USER).setHostname(mgmtNicIp).setPort(port).runErrorByExceptionAndClose();
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                /* retry too fast will produce too much useless log */
                return 20;
            }

            @Override
            public String getName() {
                return VyosDeployAgentFlow.class.getName();
            }
        });
    }

    private boolean isZvrMd5Changed(String ip, int port){
        int interval = 30 ;
        Ssh ssh = new Ssh();
        ssh.setUsername(REMOTE_USER)
                .setPrivateKey(asf.getPrivateKey())
                .setPort(port)
                .setHostname(ip)
                .setTimeout(interval);

        String remoteZvrMd5 = "";
        String remoteZvrbootMd5 = "";

        String localZvrPath = PathUtil.findFileOnClassPath("ansible/zvr/zvr.bin", true).getAbsolutePath();
        String localZvrBootPath = PathUtil.findFileOnClassPath("ansible/zvr/zvrboot.bin", true).getAbsolutePath();


        try {
            ssh.command(String.format("sudo -S md5sum %s 2>/dev/null", REMOTE_ZVR_BIN_PATH));
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
                        localZvrMd5, REMOTE_ZVR_BIN_PATH, remoteZvrMd5));
                return true;
            }

            ssh.command(String.format("sudo -S md5sum %s", REMOTE_ZVRBOOT_BIN_PATH));
            ret = ssh.run();
            if (ret.getReturnCode() == 0) {
                remoteZvrbootMd5 =  ret.getStdout().split(" ")[0];
            }

            ShellResult zvrbootRet = ShellUtils.runAndReturn(String.format("md5sum %s",localZvrBootPath));
            zvrbootRet.raiseExceptionIfFail();
            String localZvrbootMd5 = zvrbootRet.getStdout().split(" ")[0];


            if (!remoteZvrbootMd5.equals(localZvrbootMd5)) {
                logger.debug(String.format("file MD5 changed, local[%s, md5:%s] remote[%s, md5: %s]", localZvrBootPath,
                        localZvrbootMd5, REMOTE_ZVRBOOT_BIN_PATH, remoteZvrbootMd5));
                return true;
            }

        }catch (SshException  e ) {
            logger.debug(String.format("unable to check virtual router[ip:%s, port:%s] zvr md5", ip, port, e.getMessage()));
        }finally {
            ssh.close();
        }

        return false;
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        boolean isReconnect = Boolean.parseBoolean((String) data.get(Params.isReconnect.toString()));

        String mgmtNicIp;
        if (!isReconnect) {
            VmNicInventory mgmtNic;
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                    }
                });
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        } else {
            mgmtNicIp = (String) data.get(Params.managementNicIp.toString());
        }

        debug(mgmtNicIp, 30);
        trigger.rollback();
    }

    private void debug (String vrMgtIp, int timeout) {
        if (!NetworkUtils.isRemotePortOpen(vrMgtIp, REMOTE_PORT, timeout)) {
            logger.debug(String.format("virtual router agent port %s is not opened on managment nic %s",
                    REMOTE_PORT, vrMgtIp));
            return;
        }
        Ssh ssh1 = new Ssh();
        ssh1.setUsername(REMOTE_USER).setPrivateKey(asf.getPrivateKey()).setPort(REMOTE_PORT)
                .setHostname(vrMgtIp).setTimeout(timeout);
        SshResult ret1 = ssh1.command(String.format("sudo tail -n 300 %s/zvrReboot.log", REMOTE_ZVR_DIR)).runAndClose();
        if (ret1.getReturnCode() == 0) {
            logger.debug(String.format("virtual router reboot log %s", ret1.getStdout()));
        } else {
            logger.debug(String.format("get virtual router reboot log failed: %s", ret1.getStderr()));
        }

        Ssh ssh2 = new Ssh();
        ssh2.setUsername(REMOTE_USER).setPrivateKey(asf.getPrivateKey()).setPort(REMOTE_PORT)
                .setHostname(vrMgtIp).setTimeout(timeout);
        SshResult ret2 = ssh2.command("sudo tail -n 300 /tmp/agentRestart.log").runAndClose();
        if (ret2.getReturnCode() == 0) {
            logger.debug(String.format("zvr start log %s", ret2.getStdout()));
        } else {
            logger.debug(String.format("get zvr start log failed: %s", ret2.getStderr()));
        }
    }
}
