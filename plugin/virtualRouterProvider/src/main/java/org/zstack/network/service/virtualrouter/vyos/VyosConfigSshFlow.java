package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by zhanyong.miao 2018/10/08
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosConfigSshFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VyosConfigSshFlow.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            trigger.next();
            return;
        }

        boolean isReconnect = Boolean.valueOf((String) data.get(Params.isReconnect.toString()));

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

        if (isReconnect && !NetworkUtils.isRemotePortOpen(mgmtNicIp, 22, 2000)) {
            throw new OperationFailureException(operr("unable to ssh in to the vyos[%s], the ssh port seems not open", mgmtNicIp));
        }

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        trigger.fail(err(ApplianceVmErrors.UNABLE_TO_START, "the SSH port is not" +
                                " open after %s seconds. Failed to login the vyos router[ip:%s]", timeoutInSeconds, mgmtNicIp));
                        return true;
                    }

                    if (NetworkUtils.isRemotePortOpen(mgmtNicIp, 22, 2000)) {
                        configAgentSsh();
                        return true;
                    } else {
                        return false;
                    }
                } catch (Throwable t) {
                    logger.warn("unhandled exception happened", t);
                    return false;
                }
            }

            private void configAgentSsh() {
                Boolean  passwordAuth = VirtualRouterGlobalConfig.SSH_LOGIN_PASSWORD.value(Boolean.class);
                String script;
                if (!passwordAuth) {
                    script = "sudo sed -i \"/PasswordAuthentication /c PasswordAuthentication no\" /etc/ssh/sshd_config\n"+
                            "sudo service ssh restart\n";
                } else {
                    script = " sudo sed -i \"/PasswordAuthentication /c PasswordAuthentication yes\" /etc/ssh/sshd_config\n"+
                            "sudo service ssh restart\n";
                }
                new Ssh().shell( script
                ).setTimeout(300).setPrivateKey(asf.getPrivateKey()).setUsername("vyos").setHostname(mgmtNicIp).setPort(22).runErrorByExceptionAndClose();
                
                trigger.next();

            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return VyosConfigSshFlow.class.getName();
            }
        });
    }
}
