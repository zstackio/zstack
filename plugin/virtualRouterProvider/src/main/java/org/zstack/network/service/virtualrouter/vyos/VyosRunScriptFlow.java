package org.zstack.network.service.virtualrouter.vyos;

import edu.emory.mathcs.backport.java.util.LinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.*;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;

public abstract class VyosRunScriptFlow extends NoRollbackFlow {
    private CLogger logger = Utils.getLogger(VyosRunScriptFlow.class);

    public String mgmtNicIp;
    public String vrUuid;
    private String sshUserName = "vyos";
    public int sshPort;
    public int sshTimeout;
    private final Map<String, String> localRemoteMap = new HashMap<>();
    private final List<String> commands = new LinkedList();
    private String scriptContent;

    @Autowired
    public AnsibleFacade asf;
    @Autowired
    public DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    public String getVrUuid() {
        return vrUuid;
    }

    public void setVrUuid(String vrUuid) {
        this.vrUuid = vrUuid;
    }

    public void initEnv() {

    }

    public void createScript() {
    }

    public void beforeExecuteScript() {
    }

    public void afterExecuteScript() {
    }

    public void afterExecuteScriptFail() {
    }

    public void setLogger(CLogger clogger) {
        logger = clogger;
    }

    public String getScriptName() {
        return "script";
    }

    public void scpUpload(final String local, final String remote) {
        localRemoteMap.put(local, remote);
    }

    public void command(String...cmds) {
        commands.addAll(Arrays.asList(cmds));
    }

    public void script(String scriptContent)  {
        this.scriptContent = scriptContent;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        init(data);

        if (isSkipRunningScript(data)) {
            trigger.next();
            return;
        }

        initEnv();

        createScript();

        beforeExecuteScript();

        executeScript(new Completion(trigger) {
            @Override
            public void success() {
                afterExecuteScript();
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
                afterExecuteScriptFail();
            }
        });
    }

    public String getTaskName() {
        return VyosRunScriptFlow.class.getName();
    }

    public boolean isSkipRunningScript(Map data) {
        return false;
    }

    private void init(Map data) {
        final Object vrTmp = data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory mgmtNic;
        if (vrTmp != null) {
            final VirtualRouterVmInventory vr = (VirtualRouterVmInventory)vrTmp;
            mgmtNic = vr.getManagementNic();
            vrUuid = vr.getUuid();
            mgmtNicIp = mgmtNic.getIp();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            vrUuid = spec.getVmInventory().getUuid();
            if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null);
            } else {
                ApplianceVmVO avo = dbf.findByUuid(vrUuid, ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        }

        sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);

        sshTimeout = 300;
    }

    private void executeScript(Completion completion) {
        int timeoutInSeconds = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class);
        long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutInSeconds);
        int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);

        List<Throwable> errors = new ArrayList<>();
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        completion.fail(err(ApplianceVmErrors.UNABLE_TO_START, "vyos %s failed, because %s",
                                getScriptName(), errors.get(errors.size() - 1)));
                        return true;
                    }

                    if (NetworkUtils.isRemotePortOpen(mgmtNicIp, sshPort, 2000)) {
                        Ssh ssh = buildSsh(null, asf.getPrivateKey());

                        try {
                            ssh.runErrorByExceptionAndClose();
                        } catch (SshException e) {
                            ssh = buildSsh(VirtualRouterGlobalConfig.VYOS_PASSWORD.value(), null);
                            ssh.runErrorByExceptionAndClose();
                        }

                        completion.success();
                        return true;
                    } else {
                        errors.add(new Throwable(String.format("vyos agent port %s is not opened on managment nic %s", sshPort, mgmtNicIp)));
                        return false;
                    }
                } catch (Throwable t) {
                    logger.warn(String.format("vyos %s failed", getTaskName()), t);
                    errors.add(t);
                    return false;
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
                return getTaskName();
            }
        });
    }

    private Ssh buildSsh(String password, String privateKey) {
        DebugUtils.Assert(password != null || privateKey != null, "vyos ssh password and private key all null");
        DebugUtils.Assert(mgmtNicIp != null, "vyos management nic ip is null");

        Ssh ssh = new Ssh().setUsername(sshUserName).setHostname(mgmtNicIp).setPort(sshPort).setTimeout(sshTimeout);
        if (privateKey != null) {
            ssh.setPrivateKey(privateKey);
        } else {
            ssh.setPassword(password);
        }

        if (scriptContent != null) {
            ssh.shell(scriptContent);
        } else {
            if (!commands.isEmpty()) {
                commands.forEach(ssh::command);
            }
            if (!localRemoteMap.isEmpty()) {
                for (Map.Entry<String, String> entry : localRemoteMap.entrySet()) {
                    ssh.scpUpload(entry.getKey(), entry.getValue());
                }
            }
        }

        return ssh;
    }
}
